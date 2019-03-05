package com.xabber.android.data.extension.chat_markers;

import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.MessageUpdateEvent;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;

import io.realm.Realm;
import io.realm.RealmResults;

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
            } else if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                // received
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                markAsDisplayed(ChatMarkersElements.DisplayedExtension.from(message).getId());
            } else if (ChatMarkersElements.AcknowledgedExtension.from(message) != null) {
                // acknowledged
            }
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
}
