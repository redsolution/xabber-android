package com.xabber.android.data.extension.chat_markers;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.chat_markers.filter.ChatMarkersFilter;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnMessageUpdatedListener;
import com.xabber.xmpp.sid.StanzaIdElement;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
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
        XMPPConnectionRegistry.addConnectionCreationListener(connection -> {
            ServiceDiscoveryManager.getInstanceFor(connection).addFeature(ChatMarkersElements.NAMESPACE);
            connection.addPacketInterceptor(packet -> {
                Message message = (Message) packet;
                message.addExtension(new ChatMarkersElements.MarkableExtension());
            }, OUTGOING_MESSAGE_FILTER);
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
                markAsReceived(ChatMarkersElements.ReceivedExtension.from(message));
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                // displayed
                markAsDisplayed(ChatMarkersElements.DisplayedExtension.from(message));
            } else if (ChatMarkersElements.AcknowledgedExtension.from(message) != null) {
                LogManager.i(LOG_TAG, "Got \"Acknowledged\" stanza, but no actions performed!");
            }
        }
    }

    public void sendDisplayed(MessageRealmObject messageRealmObject) {
        if (messageRealmObject.getStanzaId() == null && messageRealmObject.getOriginId() == null) return;

        Message displayedNotification = new Message(messageRealmObject.getUser().getJid());
        Message originalMessage = null;
        try {
            originalMessage = PacketParserUtils.parseStanza(messageRealmObject.getOriginalStanza());
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }

        if (originalMessage == null) return;

        String messageId = originalMessage.getStanzaId();

        List<ExtensionElement> stanzaIds =
                new ArrayList<>(originalMessage.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE));

        if (messageId == null || messageId.isEmpty()) {
            if (stanzaIds.isEmpty()){
                LogManager.exception(LOG_TAG, new NullPointerException("Can't find any stanza id in message!"));
                return;
            }
            for (ExtensionElement stanzaIdElement : stanzaIds) {
                if (stanzaIdElement instanceof StanzaIdElement) {
                    String idBy = ((StanzaIdElement) stanzaIdElement).getBy();
                    if (idBy != null && idBy.equals(messageRealmObject.getUser().getBareJid().toString())) {
                        messageId = ((StanzaIdElement) stanzaIdElement).getId();
                        break;
                    } else messageId = ((StanzaIdElement) stanzaIdElement).getId();
                }
            }
        }

        ChatMarkersElements.DisplayedExtension displayedExtension =
                new ChatMarkersElements.DisplayedExtension(messageId);

        displayedExtension.setStanzaIdExtensions(stanzaIds);

        displayedNotification.addExtension(displayedExtension);
        displayedNotification.setType(Message.Type.chat);

        if (Looper.myLooper() != Looper.getMainLooper()) {
            try {
                StanzaSender.sendStanza(messageRealmObject.getAccount(), displayedNotification);
            } catch (NetworkException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else sendMessageInBackgroundUserRequest(displayedNotification, messageRealmObject.getAccount());

    }

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        if (direction == CarbonExtension.Direction.sent) {
            ChatMarkersElements.DisplayedExtension extension = ChatMarkersElements.DisplayedExtension.from(message);
            if (extension != null) {
                ContactJid companion;
                try {
                    companion = ContactJid.from(message.getTo()).getBareUserJid();
                } catch (ContactJid.ContactJidCreateException e) {
                    LogManager.exception(LOG_TAG, e);
                    return;
                }
                AbstractChat chat = ChatManager.getInstance().getChat(account, companion);
                if (chat != null) {
                    chat.markAsRead(extension.getId(), extension.getStanzaId(), false);
                    MessageNotificationManager.getInstance().removeChatWithTimer(account, companion);

                    // start grace period
                    AccountManager.getInstance().startGracePeriod(account);
                }
            }
        }
        else if (direction == CarbonExtension.Direction.received) {
            if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                BackpressureMessageMarker.getInstance().markMessage(
                        ChatMarkersElements.ReceivedExtension.from(message).getId(),
                        ChatMarkersElements.ReceivedExtension.from(message).getStanzaId(),
                        ChatMarkersState.received, account);
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                BackpressureMessageMarker.getInstance().markMessage(
                        ChatMarkersElements.DisplayedExtension.from(message).getId(),
                        ChatMarkersElements.DisplayedExtension.from(message).getStanzaId(),
                        ChatMarkersState.displayed, account);
            }
        }
    }

    private boolean isClientSupportChatMarkers(AccountJid account, ContactJid user) {
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
                LogManager.exception(getClass().getSimpleName(), e);
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
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                StanzaSender.sendStanza(account, message);
            } catch (NetworkException e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    private void markAsDisplayed(ChatMarkersElements.DisplayedExtension displayedExtension) {
        if (displayedExtension.getId() == null || displayedExtension.getId().isEmpty()) {
            if (!displayedExtension.getStanzaId().isEmpty()) markAsDisplayed(displayedExtension.getStanzaId().get(0));
        } else markAsDisplayed(displayedExtension.getId());
    }

    private void markAsDisplayed(final String messageID) {
        Application.getInstance().runInBackground(() ->  {
                Realm realm = null;
                try {
                    realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                    realm.executeTransaction(realm1 ->  {
                        MessageRealmObject first = realm1.where(MessageRealmObject.class)
                                .equalTo(MessageRealmObject.Fields.ORIGIN_ID, messageID).findFirst();

                        if (first != null) {
                            RealmResults<MessageRealmObject> results = realm1.where(MessageRealmObject.class)
                                    .equalTo(MessageRealmObject.Fields.ACCOUNT, first.getAccount().toString())
                                    .equalTo(MessageRealmObject.Fields.USER, first.getUser().toString())
                                    .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                    .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS,
                                            MessageStatus.DISPLAYED.toString()) //todo check this
                                    .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS,
                                            MessageStatus.UPLOADING.toString()) //todo check this
                                    .lessThanOrEqualTo(MessageRealmObject.Fields.TIMESTAMP, first.getTimestamp())
                                    .findAll();

                            if (results != null) {
                                results.setString(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.DISPLAYED.toString());
                                for (OnMessageUpdatedListener listener :
                                        Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                                    listener.onAction();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    LogManager.exception(LOG_TAG, e);
                } finally {
                    if (realm != null) realm.close();
                }
        });
    }

    private void markAsReceived(ChatMarkersElements.ReceivedExtension receivedExtension) {
        if (receivedExtension.getId() == null || receivedExtension.getId().isEmpty()) {
            if (!receivedExtension.getStanzaId().isEmpty()) markAsReceived(receivedExtension.getStanzaId().get(0));
        } else markAsReceived(receivedExtension.getId());
    }

    private void markAsReceived(final String stanzaID) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                    MessageRealmObject first = realm1.where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.ORIGIN_ID, stanzaID).findFirst();

                    if (first != null) {
                        RealmResults<MessageRealmObject> results = realm1.where(MessageRealmObject.class)
                                .equalTo(MessageRealmObject.Fields.ACCOUNT, first.getAccount().toString())
                                .equalTo(MessageRealmObject.Fields.USER, first.getUser().toString())
                                .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.RECEIVED.toString())
                                .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.UPLOADING.toString())
                                .lessThanOrEqualTo(MessageRealmObject.Fields.TIMESTAMP, first.getTimestamp())
                                .findAll();

                        if (results != null) {
                            results.setString(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.RECEIVED.toString());
                            for (OnMessageUpdatedListener listener : Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                                listener.onAction();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

}
