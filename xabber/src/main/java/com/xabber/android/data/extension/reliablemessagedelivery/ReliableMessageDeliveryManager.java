package com.xabber.android.data.extension.reliablemessagedelivery;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ReliableMessageDeliveryManager implements OnPacketListener {

    public static final String NAMESPACE = "http://xabber.com/protocol/delivery";
    public static final String LOG_TAG = ReliableMessageDeliveryManager.class.getSimpleName();

    private static ReliableMessageDeliveryManager instance;

    public static ReliableMessageDeliveryManager getInstance() {
        if (instance == null)
            instance = new ReliableMessageDeliveryManager();
        ProviderManager.addExtensionProvider(ReceiptElement.ELEMENT, ReceiptElement.NAMESPACE, new ReceiptElement.ReceiptElementProvider());
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection xmpptcpConnection) {
        if (xmpptcpConnection.isConnected())
            try {
                return ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).serverSupportsFeature(NAMESPACE);
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
        LogManager.d(LOG_TAG, "To check supporting connection should be connected!");
        return false;
    }

    public boolean isSupported(AccountItem accountItem) { return isSupported(accountItem.getConnection()); }

    public boolean isSupported(AccountJid accountJid) { return isSupported(AccountManager.getInstance().getAccount(accountJid)); }

    public void resendMessagesWithoutReceipt() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()){
                        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                        if (isSupported(accountItem) && accountItem.isSuccessfulConnectionHappened()){
                            RealmResults<MessageRealmObject> messagesUndelivered = realm1.where(MessageRealmObject.class)
                                    .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                                    .equalTo(MessageRealmObject.Fields.SENT, true)
                                    .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                    .equalTo(MessageRealmObject.Fields.DELIVERED, false)
                                    .equalTo(MessageRealmObject.Fields.IS_RECEIVED_FROM_MAM, false)
                                    .equalTo(MessageRealmObject.Fields.READ, false)
                                    .equalTo(MessageRealmObject.Fields.DISPLAYED, false)
                                    .findAll()
                                    .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
                            if (messagesUndelivered.size() != 0)
                                for (MessageRealmObject messageRealmObject : messagesUndelivered){
                                    if (messageRealmObject != null
                                            && !messageRealmObject.getStanzaId().equals(messageRealmObject.getOriginId())
                                            && messageRealmObject.getTimestamp() + 5000 <= new Date(System.currentTimeMillis()).getTime()) {
                                        ChatManager.getInstance().getChat(messageRealmObject.getAccount(), messageRealmObject.getUser()).sendMessage(messageRealmObject);
                                        LogManager.d(LOG_TAG, "Retry sending message with stanza: " + messageRealmObject.getOriginalStanza());
                                    }
                                }
                        }
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void markMessageReceivedInDatabase(final String time, final String originId, final String stanzaId) {
        Application.getInstance().runInBackgroundUserRequest(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                final Long millis = StringUtils.parseReceivedReceiptTimestampString(time).getTime();
                realm.executeTransaction(realm1 -> {
                    MessageRealmObject messageRealmObject = realm1
                            .where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.ORIGIN_ID, originId)
                            .findFirst();
                    messageRealmObject.setStanzaId(stanzaId);
                    messageRealmObject.setTimestamp(millis);
                    messageRealmObject.setAcknowledged(true);
                    LogManager.d(LOG_TAG, "Message marked as received with original stanza" + messageRealmObject.getOriginalStanza());
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza instanceof Message
                && ((Message) stanza).getType().equals(Message.Type.headline)
                && stanza.hasExtension(NAMESPACE)) {
            try {
                ReceiptElement receipt = (ReceiptElement) stanza.getExtension(NAMESPACE);
                String timestamp = receipt.getTimeElement().getStamp();
                String originId = receipt.getOriginIdElement().getId();
                String stanzaId = receipt.getStanzaIdElement().getId();
                LogManager.d(LOG_TAG, "Received receipt: " + stanza.toString());
                markMessageReceivedInDatabase(timestamp, originId, stanzaId);
                EventBus.getDefault().post(new MessageUpdateEvent());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }

}
