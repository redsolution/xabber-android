package com.xabber.android.data.extension.chat_markers;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jxmpp.jid.Jid;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatMarkerManager implements OnPacketListener {

    private static ChatMarkerManager instance;

    public static ChatMarkerManager getInstance() {
        if (instance == null) instance = new ChatMarkerManager();
        return instance;
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

    public void sendDisplayedIfNeed(AccountJid account, UserJid user) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.USER, user.toString())
                .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                .equalTo(MessageItem.Fields.INCOMING, true)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        MessageItem lastIncomingMessage = results.last();
        if (!lastIncomingMessage.isRead()) {
            sendDisplayed(lastIncomingMessage);
            realm.beginTransaction();
            lastIncomingMessage.setRead(true);
            realm.commitTransaction();
        }
    }

    private void sendDisplayed(MessageItem messageItem) {
        Message displayed = new Message(messageItem.getUser().getJid());
        displayed.addExtension(new ChatMarkersElements.DisplayedExtension(messageItem.getStanzaId()));
        //displayed.setThread(messageItem.getThread());
        displayed.setType(Message.Type.chat);

        try {
            StanzaSender.sendStanza(messageItem.getAccount(), displayed);
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
    }

    private void sendReceived(Message message, AccountJid account) {
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
        // TODO: 05.03.19 optimizations
        // if (have subscription)
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        MessageItem first = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.STANZA_ID, messageID).findFirst();

        if (first != null) {
            UserJid jid = first.getUser();
            RealmResults<MessageItem> results = realm.where(MessageItem.class)
                    .equalTo(MessageItem.Fields.USER, jid.toString())
                    .equalTo(MessageItem.Fields.INCOMING, false)
                    .equalTo(MessageItem.Fields.DISPLAYED, false).findAll();

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
