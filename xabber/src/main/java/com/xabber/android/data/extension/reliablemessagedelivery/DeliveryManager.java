package com.xabber.android.data.extension.reliablemessagedelivery;

import androidx.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.ui.OnMessageUpdatedListener;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.xmpp.sid.UniqueIdsHelper;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

public class DeliveryManager implements OnPacketListener, OnConnectedListener {

    public static final String NAMESPACE = "https://xabber.com/protocol/delivery";
    public static final String LOG_TAG = DeliveryManager.class.getSimpleName();

    private static DeliveryManager instance;

    public static DeliveryManager getInstance() {
        if (instance == null)
            instance = new DeliveryManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection xmpptcpConnection) {
        if (xmpptcpConnection.isAuthenticated())
            try {
                return ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).serverSupportsFeature(NAMESPACE);
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
        LogManager.d(LOG_TAG, "To check supporting connection should be connected!");
        return false;
    }

    public boolean isSupported(AccountItem accountItem) { return isSupported(accountItem.getConnection()); }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid));
    }

    public void resendMessagesWithoutReceipt() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()){
                        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                        if (accountItem != null
                                && accountItem.isSuccessfulConnectionHappened()
                                && isSupported(accountItem)){

                            RealmResults<MessageRealmObject> messagesUndelivered = realm1
                                    .where(MessageRealmObject.class)
                                    .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.getFullJid().toString())
                                    .equalTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.SENT.toString())
                                    .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.DELIVERED.toString())
                                    .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.RECEIVED.toString())
                                    .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.DISPLAYED.toString())
                                    .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                    .findAll(); //todo check this statements

                            if (messagesUndelivered.size() != 0)
                                for (MessageRealmObject messageRealmObject : messagesUndelivered){
                                    if (messageRealmObject != null
                                            && !messageRealmObject.getStanzaId().equals(
                                                    messageRealmObject.getOriginId())
                                            && messageRealmObject.getTimestamp() + 5000 <= new Date(
                                                    System.currentTimeMillis()).getTime()) {

                                        ChatManager.getInstance().getChat(messageRealmObject.getAccount(),
                                                messageRealmObject.getUser()).sendMessage(messageRealmObject);

                                        LogManager.d(LOG_TAG, "Retry sending message with stanza: "
                                                + messageRealmObject.getOriginalStanza());
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

    private void markMessageReceivedInDatabase(final String time, @NonNull final String originId, final String stanzaId) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    MessageRealmObject messageRealmObject = realm1
                            .where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.ORIGIN_ID, originId)
                            .or()
                            .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, originId)
                            .findFirst();
                    if (messageRealmObject == null){
                        LogManager.exception(LOG_TAG, new NullPointerException("Warning! Got delivery receipt for " +
                                "origin id: " + originId + ", but can't find message in database"));
                    } else {
                        messageRealmObject.setStanzaId(stanzaId);
                        messageRealmObject.setMessageStatus(MessageStatus.DELIVERED);
                        if (time != null && !time.isEmpty())
                            messageRealmObject.setTimestamp(StringUtils.parseReceivedReceiptTimestampString(time).getTime());
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    @Override
    public void onConnected(ConnectionItem connection) {
        resendMessagesWithoutReceipt();
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza instanceof Message && ((Message) stanza).getType().equals(Message.Type.headline)
                && stanza.hasExtension(ReceivedExtensionElement.NAMESPACE)) {
            String timestamp = "";
            String originId = "";
            String stanzaId = "";
            try{
                if (stanza.hasExtension(ReceivedExtensionElement.ELEMENT, ReceivedExtensionElement.NAMESPACE)){
                    ReceivedExtensionElement receipt = (ReceivedExtensionElement) stanza.getExtension(NAMESPACE);
                    timestamp = receipt.getTimeElement().getTimeStamp();
                    originId = receipt.getOriginIdElement().getId();
                    stanzaId = receipt.getStanzaIdElement().getId();
                } else if (stanza.hasExtension(GroupExtensionElement.ELEMENT, NAMESPACE)) {
                    StandardExtensionElement echoElement = (StandardExtensionElement) stanza.getExtensions().get(0);
                    Message message = PacketParserUtils.parseStanza(echoElement.getElements().get(0).toXML().toString());
                    originId = UniqueIdsHelper.getOriginId(message);
                    stanzaId = UniqueIdsHelper.getStanzaIdBy(message, stanza.getFrom().asBareJid().toString());
                    if (echoElement.getFirstElement(TimeElement.ELEMENT, TimeElement.NAMESPACE) != null){
                        timestamp = echoElement.getFirstElement(TimeElement.ELEMENT, TimeElement.NAMESPACE)
                                .getAttributeValue(TimeElement.ATTRIBUTE_STAMP);
                    }
                }
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
            LogManager.d(LOG_TAG, "Received receipt to message with origin id : " + originId);
            markMessageReceivedInDatabase(timestamp, originId, stanzaId);
            for (OnMessageUpdatedListener listener :
                    Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                listener.onAction();
            }
        }
    }

}
