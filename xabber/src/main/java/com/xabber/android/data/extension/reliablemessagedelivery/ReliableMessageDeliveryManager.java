package com.xabber.android.data.extension.reliablemessagedelivery;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
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
                realm = DatabaseManager.getInstance().getRealmDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()){
                        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                        if (isSupported(accountItem) && accountItem.isSuccessfulConnectionHappened()){
                            RealmResults<MessageItem> messagesUndelivered = realm1.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString())
                                    .equalTo(MessageItem.Fields.SENT, true)
                                    .equalTo(MessageItem.Fields.INCOMING, false)
                                    .equalTo(MessageItem.Fields.DELIVERED, false)
                                    .equalTo(MessageItem.Fields.IS_RECEIVED_FROM_MAM, false)
                                    .equalTo(MessageItem.Fields.READ, false)
                                    .equalTo(MessageItem.Fields.DISPLAYED, false)
                                    .findAll()
                                    .sort(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
                            if (messagesUndelivered.size() != 0)
                                for (MessageItem messageItem : messagesUndelivered){
                                    if (messageItem != null
                                            && !messageItem.getStanzaId().equals(messageItem.getOriginId())
                                            && messageItem.getTimestamp() + 5000 <= new Date(System.currentTimeMillis()).getTime()) {
                                        MessageManager.getInstance().getChat(messageItem.getAccount(), messageItem.getUser()).sendMessage(messageItem);
                                        LogManager.d(LOG_TAG, "Retry sending message with stanza: " + messageItem.getOriginalStanza());
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
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getRealmDefaultInstance();
                final Long millis = StringUtils.parseReceivedReceiptTimestampString(time).getTime();
                realm.executeTransaction(realm1 -> {
                    MessageItem messageItem = realm1
                            .where(MessageItem.class)
                            .equalTo(MessageItem.Fields.ORIGIN_ID, originId)
                            .findFirst();
                    messageItem.setStanzaId(stanzaId);
                    messageItem.setTimestamp(millis);
                    messageItem.setAcknowledged(true);
                    LogManager.d(LOG_TAG, "Message marked as received with original stanza" + messageItem.getOriginalStanza());
                });
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
