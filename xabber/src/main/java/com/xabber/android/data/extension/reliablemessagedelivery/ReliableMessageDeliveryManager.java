package com.xabber.android.data.extension.reliablemessagedelivery;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
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

import java.util.LinkedHashMap;
import java.util.Map;

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
        try {
            if (xmpptcpConnection.getUser() == null){
                LogManager.d(LOG_TAG, "To check supporting connection should be connected!");
                return false;
            }
            return ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountItem accountItem) {
        return isSupported(accountItem.getConnection());
    }

    private LinkedHashMap<AccountJid, UserJid> getChatsWithEnabledXep() {
        final LinkedHashMap<AccountJid, UserJid> list = new LinkedHashMap<>();
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()){
                                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                                if (isSupported(accountItem) && accountItem.isSuccessfulConnectionHappened()){
                                    RealmResults<MessageItem> messagesUndelivered = realm.where(MessageItem.class)
                                        .equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString())
                                        .equalTo(MessageItem.Fields.SENT, true)
                                        .equalTo(MessageItem.Fields.DELIVERED, false)
                                        .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
                                    for (MessageItem messageItem : messagesUndelivered){
                                        list.put(messageItem.getAccount(), messageItem.getUser());
                                    }
                                }
                            }
                            LogManager.d(LOG_TAG, Integer.toString(Realm.getGlobalInstanceCount(realm.getConfiguration())));
                        }
                    });
                } finally {
                    if (realm != null){
                        realm.close();
                    } LogManager.d(LOG_TAG, Integer.toString(Realm.getGlobalInstanceCount(realm.getConfiguration())));
                }
            }
        });
        return list;
    }

    public void resendMessagesWithoutReceipt() {
        for (Map.Entry<AccountJid, UserJid> entry : getChatsWithEnabledXep().entrySet()){
            LogManager.d(LOG_TAG, "Found messages without receipt for chats with: " + entry.getKey() + " and: " + entry.getValue());
            MessageManager.getInstance().getChat(entry.getKey(), entry.getValue()).sendMessages();
        }

    }

    private void markMessageReceivedInDatabase(final String time, final String originId, final String stanzaId) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                    final Long millis = StringUtils.parseReceivedReceiptTimestampString(time).getTime();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            MessageItem messageItem = realm
                                    .where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.STANZA_ID, originId)
                                    .findFirst();
                            messageItem.setStanzaId(stanzaId);
                            messageItem.setTimestamp(millis);
                            messageItem.setDelivered(true);
                        }
                    });
                    LogManager.d(LOG_TAG, Integer.toString(Realm.getGlobalInstanceCount(realm.getConfiguration())));
                } finally {
                    if (realm != null)
                        realm.close();
                    LogManager.d(LOG_TAG, Integer.toString(Realm.getGlobalInstanceCount(realm.getConfiguration())));
                }
            }
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
                String stanzaId = receipt.getOriginIdElement().getId();
                LogManager.d(LOG_TAG, "Receipt received with timestamp: " + timestamp + "; origin-id: " + originId + "; stanza-id: " + stanzaId + ". Trying to wite it to database");
                markMessageReceivedInDatabase(timestamp, originId, stanzaId);
                EventBus.getDefault().post(new MessageUpdateEvent());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }

}
