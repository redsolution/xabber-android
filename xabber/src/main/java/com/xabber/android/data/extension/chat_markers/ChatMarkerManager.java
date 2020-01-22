package com.xabber.android.data.extension.chat_markers;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.chat_markers.filter.ChatMarkersFilter;
import com.xabber.android.data.extension.reliablemessagedelivery.StanzaIdElement;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.RosterManager;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class ChatMarkerManager implements OnPacketListener {

    private static final String LOG_TAG = ChatMarkerManager.class.getSimpleName();
    private static final StanzaFilter OUTGOING_MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            MessageWithBodiesFilter.INSTANCE,
            new NotFilter(ChatMarkersFilter.INSTANCE),
            EligibleForChatMarkerFilter.INSTANCE
    );

    private static ChatMarkerManager instance;

    public static ChatMarkerManager getInstance() {
        if (instance == null) instance = new ChatMarkerManager();
        return instance;
    }

    public ChatMarkerManager() {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(ChatMarkersElements.NAMESPACE);
                connection.addPacketInterceptor(new StanzaListener() {
                    @Override
                    public void processStanza(Stanza packet) {
                        Message message = (Message) packet;
                        message.addExtension(new ChatMarkersElements.MarkableExtension());
                    }
                }, OUTGOING_MESSAGE_FILTER);
            }
        });
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof Message) {
            final Message message = (Message) packet;

            if (ChatMarkersElements.MarkableExtension.from(message) != null) {
                // markable
                sendReceived(message, connection.getAccount());
            } else if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                // received
                markAsDelivered(ChatMarkersElements.ReceivedExtension.from(message).getId());
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                // displayed
                markAsDisplayed(ChatMarkersElements.DisplayedExtension.from(message).getId());
            } else if (ChatMarkersElements.AcknowledgedExtension.from(message) != null) {
                // acknowledged
            }
        }
    }

    public void sendDisplayed(MessageItem messageItem) {
        if (messageItem.getStanzaId() == null && messageItem.getOriginId() == null) return;

        String id = messageItem.getOriginId() != null ?
                messageItem.getOriginId() : messageItem.getStanzaId();
        if (id == null || id.isEmpty()) return;

        Message displayed = new Message(messageItem.getUser().getJid());
        Message originalMessage = null;
        try {
            originalMessage = PacketParserUtils.parseStanza(messageItem.getOriginalStanza());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ChatMarkersElements.DisplayedExtension displayedExtension = new ChatMarkersElements.DisplayedExtension(id);

        if (originalMessage != null) {
            displayedExtension.setStanzaIdExtensions(originalMessage.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE));
        }

        displayed.addExtension(displayedExtension);
        displayed.setType(Message.Type.chat);

        sendMessageInBackgroundUserRequest(displayed, messageItem.getAccount());
    }

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        if (direction == CarbonExtension.Direction.sent) {
            ChatMarkersElements.DisplayedExtension extension =
                    ChatMarkersElements.DisplayedExtension.from(message);
            if (extension != null) {
                UserJid companion;
                try {
                    companion = UserJid.from(message.getTo()).getBareUserJid();
                } catch (UserJid.UserJidCreateException e) {
                    return;
                }
                AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, companion);
                if (chat != null) {
                    chat.markAsReadTest(extension.getId(), extension.getStanzaId(), false);
                    //chat.markAsRead(extension.getId(), false);
                    MessageNotificationManager.getInstance().removeChatWithTimer(account, companion);

                    // start grace period
                    AccountManager.getInstance().startGracePeriod(account);
                }
            }
        }
        else if (direction == CarbonExtension.Direction.received) {
            if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                // received
                BackpressureMessageMarker.getInstance().markMessage(ChatMarkersElements.ReceivedExtension.from(message).getId(),
                        ChatMarkersElements.ReceivedExtension.from(message).getStanzaId(),
                        ChatMarkersState.received, account);
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                // displayed
                BackpressureMessageMarker.getInstance().markMessage(ChatMarkersElements.DisplayedExtension.from(message).getId(),
                        ChatMarkersElements.DisplayedExtension.from(message).getStanzaId(),
                        ChatMarkersState.displayed, account);
            }
        }
    }

    private boolean isClientSupportChatMarkers(AccountJid account, UserJid user) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) return false;

        XMPPConnection connection = accountItem.getConnection();
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, user.getJid());
        boolean isChatMarkersSupport = false;

        for (Presence presence : allPresences) {
            Jid fromJid = presence.getFrom();
            DiscoverInfo discoverInfo = null;
            try {
                discoverInfo = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(fromJid);
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException |
                    SmackException.NotConnectedException | InterruptedException | ClassCastException e) {
                e.printStackTrace();
            }

            isChatMarkersSupport = discoverInfo != null && discoverInfo.containsFeature(ChatMarkersElements.NAMESPACE);
            if (isChatMarkersSupport) break;
        }
        return isChatMarkersSupport;
    }

    private void sendReceived(Message message, AccountJid account) {
        if (message.getStanzaId() == null || message.getStanzaId().isEmpty()) return;

        Message received = new Message(message.getFrom());
        received.addExtension(new ChatMarkersElements.ReceivedExtension(message.getStanzaId()));
        received.setThread(message.getThread());
        received.setType(Message.Type.chat);

        sendMessageInBackgroundUserRequest(received, account);
    }

    private void sendMessageInBackgroundUserRequest(final Message message, final AccountJid account) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                try {
                    StanzaSender.sendStanza(account, message);
                } catch (NetworkException e) {
                    LogManager.exception(this, e);
                }
            }
        });
    }

    private void markAsDisplayed(final String messageID) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            MessageItem first = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.ORIGIN_ID, messageID).findFirst();

                            if (first != null) {
                                RealmResults<MessageItem> results = realm.where(MessageItem.class)
                                        .equalTo(MessageItem.Fields.ACCOUNT, first.getAccount().toString())
                                        .equalTo(MessageItem.Fields.USER, first.getUser().toString())
                                        .equalTo(MessageItem.Fields.INCOMING, false)
                                        .equalTo(MessageItem.Fields.DISPLAYED, false)
                                        .equalTo(MessageItem.Fields.IS_IN_PROGRESS, false)
                                        .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, first.getTimestamp())
                                        .findAll();

                                if (results != null) {
                                    for (MessageItem item : results) {
                                        item.setDisplayed(true);
                                    }
                                    EventBus.getDefault().post(new MessageUpdateEvent());
                                }
                            }
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); } //TODO maybe should close!
            }
        });

    }

    private void markAsDelivered(final String stanzaID) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            MessageItem first = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.ORIGIN_ID, stanzaID).findFirst();

                            if (first != null) {
                                RealmResults<MessageItem> results = realm.where(MessageItem.class)
                                        .equalTo(MessageItem.Fields.ACCOUNT, first.getAccount().toString())
                                        .equalTo(MessageItem.Fields.USER, first.getUser().toString())
                                        .equalTo(MessageItem.Fields.INCOMING, false)
                                        .equalTo(MessageItem.Fields.DELIVERED, false)
                                        .equalTo(MessageItem.Fields.IS_IN_PROGRESS, false)
                                        .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, first.getTimestamp())
                                        .findAll();

                                if (results != null) {
                                    for (MessageItem item : results) {
                                        item.setDelivered(true);
                                    }
                                    EventBus.getDefault().post(new MessageUpdateEvent());
                                }
                            }
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); } //TODO maybe should close!
            }
        });

    }
}
