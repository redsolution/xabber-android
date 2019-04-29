package com.xabber.android.data.extension.chat_markers;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.chat_markers.filter.ChatMarkersFilter;
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
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class ChatMarkerManager implements OnPacketListener {

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
        if (messageItem.getStanzaId() == null || messageItem.getStanzaId().isEmpty()) return;

        Message displayed = new Message(messageItem.getUser().getJid());
        displayed.addExtension(new ChatMarkersElements.DisplayedExtension(messageItem.getStanzaId()));
        displayed.setType(Message.Type.chat);
        try {
            StanzaSender.sendStanza(messageItem.getAccount(), displayed);
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
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
                    chat.markAsRead(extension.getId(), false);
                    MessageNotificationManager.getInstance().removeChatWithTimer(account, companion);

                    // start grace period
                    AccountManager.getInstance().startGracePeriod(account);
                }
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

        try {
            StanzaSender.sendStanza(account, received);
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
    }

    private void markAsDisplayed(final String messageID) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        MessageItem first = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.STANZA_ID, messageID).findFirst();

        if (first != null) {
            RealmResults<MessageItem> results = realm.where(MessageItem.class)
                    .equalTo(MessageItem.Fields.ACCOUNT, first.getAccount().toString())
                    .equalTo(MessageItem.Fields.USER, first.getUser().toString())
                    .equalTo(MessageItem.Fields.INCOMING, false)
                    .equalTo(MessageItem.Fields.DISPLAYED, false)
                    .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, first.getTimestamp())
                    .findAll();

            if (results != null) {
                realm.beginTransaction();
                for (MessageItem item : results) {
                    item.setDisplayed(true);
                }
                realm.commitTransaction();
                EventBus.getDefault().post(new MessageUpdateEvent());
            }
        }
    }

    private void markAsDelivered(final String stanzaID) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();

        MessageItem first = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.STANZA_ID, stanzaID).findFirst();

        if (first != null) {
            realm.beginTransaction();
            first.setDelivered(true);
            realm.commitTransaction();
        }
        EventBus.getDefault().post(new MessageUpdateEvent());
    }
}
