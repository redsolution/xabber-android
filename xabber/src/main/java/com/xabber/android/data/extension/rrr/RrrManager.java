package com.xabber.android.data.extension.rrr;

import com.xabber.android.data.Application;
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
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.List;

import io.realm.Realm;

public class RrrManager implements OnPacketListener {

    public static final String NAMESPACE = "http://xabber.com/protocol/rewrite";
    public static final String NAMESPACE_NOTIFY = NAMESPACE.concat("#notify");
    public static final String RETRACT_MESSAGE_ELEMENT = "retract-message";
    public static final String REWRITE_MESSAGE_ELEMENT = "replace";
    public static final String REPLACED_STAMP_ELEMENT = "replaced";
    public static final String BODY_MESSAGE_ELEMENT = "body";
    public static final String BY_ATTRIBUTE = "by";
    public static final String CONVERSATION_ATTRIBUTE = "conversation";
    public static final String ID_ATTRIBUTE = "id";
    public static final String STAMP_ATTRIBUTE = "stamp";
    public static final String TO_ATTRIBUTE = "to";
    private static final String LOG_TAG = "RRRManager";
    private static RrrManager instance;

    public static RrrManager getInstance() {
        if (instance == null)
            instance = new RrrManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection connection) {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid).getConnection());
    }

    public void subscribeForUpdates() {
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()) {
            if (RrrManager.getInstance().isSupported(accountJid))
                subscribeForUpdates(accountJid);
        }
    }

    public boolean subscribeForUpdates(final AccountJid accountJid) {
        XMPPTCPConnection xmpptcpConnection = AccountManager.getInstance().getAccount(accountJid).getConnection();
        try {
            xmpptcpConnection.sendIqWithResponseCallback(new SubscribeUpdatesIQ(accountJid), new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                    if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.error)) {
                        LogManager.d(LOG_TAG, "Failed to subscribe for RRR updates for account " + accountJid + "! Received error IQ");
                    }
                }
            });
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
        return true;
    }

    public void sendRetractRequest(AccountJid accountJid, List<String> list, boolean symmetrically) {
        for (String id : list)
            sendRetractRequest(accountJid, id, symmetrically);
    }

    public void sendRetractRequest(final AccountJid accountJid, final String id, final boolean symmetrically) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            final MessageItem messageItem = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.UNIQUE_ID, id).findFirst();
                            try {
                                RetractMessageIQ iq = new RetractMessageIQ(accountJid.toString(), messageItem.getStanzaId(), symmetrically);
                                AccountManager.getInstance().getAccount(accountJid).getConnection()
                                        .sendIqWithResponseCallback(iq, new StanzaListener() {
                                            @Override
                                            public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                                                if (packet instanceof IQ) {
                                                    messageItem.deleteFromRealm();
                                                    if (((IQ) packet).getType().equals(IQ.Type.error))
                                                        LogManager.d(LOG_TAG, "Failed to retract message");
                                                    if (((IQ) packet).getType().equals(IQ.Type.result))
                                                        LogManager.d(LOG_TAG, "Message successfully retracted");
                                                }
                                            }
                                        });
                            } catch (Exception e) {
                                LogManager.exception(LOG_TAG, e);
                            }
                        }
                    });

                } catch (Exception e) {
                    LogManager.exception(LOG_TAG, e);
                } finally {
                    if (realm != null)
                        realm.close();
                }
            }
        });
    }

    public void sendRetractAllMessagesRequest(final AccountJid accountJid, final UserJid userJid, final boolean symmetric){
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                RetractAllMessagesIQ retractAllMessagesIQ = new RetractAllMessagesIQ(userJid.toString(), symmetric);
                try {
                    AccountManager.getInstance().getAccount(accountJid).getConnection()
                            .sendIqWithResponseCallback(retractAllMessagesIQ, new StanzaListener() {
                                @Override
                                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                                    if (packet instanceof IQ ) {
                                        if (((IQ) packet).getType().equals(IQ.Type.error))
                                            LogManager.d(LOG_TAG, "Failed to retract message");
                                        if (((IQ) packet).getType().equals(IQ.Type.result))
                                            LogManager.d(LOG_TAG, "Message successfully retracted");
                                    }
                                }
                            });
                } catch (Exception e) {LogManager.exception(LOG_TAG, e); }
            }
        });
        MessageManager.getInstance().clearHistory(accountJid, userJid);
    }

    private void handleIncomingRetractMessage(final String id, final String by, final String conversation) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            MessageItem messageItem = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.USER, conversation)
                                    .equalTo(MessageItem.Fields.STANZA_ID, id)
                                    .findFirst();
                            if (messageItem != null)
                                messageItem.deleteFromRealm();
                            EventBus.getDefault().post(new MessageUpdateEvent());
                        }
                    });
                } finally {
                    if (realm != null)
                        realm.close();
                }
            }
        });
    }

    private void handleIncomingRewriteMessage(final String stanzaId, final String conversation, final String stamp, final String body) {
        //TODO rewrite this
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            MessageItem messageItem = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.USER, conversation)
                                    .equalTo(MessageItem.Fields.STANZA_ID, stanzaId)
                                    .findFirst();
                            if (messageItem != null)
                                if (body != null)
                                    messageItem.setText(body);
                            EventBus.getDefault().post(new MessageUpdateEvent());
                        }
                    });
                } finally {
                    if (realm != null)
                        realm.close();
                }
            }
        });
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {

        if (packet instanceof Message && ((Message) packet).getType().equals(Message.Type.headline)) {

            if (packet.hasExtension(RETRACT_MESSAGE_ELEMENT, NAMESPACE_NOTIFY)) {
                LogManager.d(LOG_TAG, "Received retract request with stanza id" + packet.toString());
                StandardExtensionElement retractElement = packet.getExtension(RETRACT_MESSAGE_ELEMENT, NAMESPACE_NOTIFY);
                String by = retractElement.getAttributeValue(BY_ATTRIBUTE);
                String conversation = retractElement.getAttributeValue(CONVERSATION_ATTRIBUTE);
                String id = retractElement.getAttributeValue(ID_ATTRIBUTE);
                handleIncomingRetractMessage(id, by, conversation);
                return;
            }

            if (packet.hasExtension(REWRITE_MESSAGE_ELEMENT, NAMESPACE_NOTIFY)) {
                LogManager.d(LOG_TAG, "Received rewrite request with stanza " + packet.toXML().toString());
                StandardExtensionElement rewriteElement = packet.getExtension(REWRITE_MESSAGE_ELEMENT, NAMESPACE_NOTIFY);
                StandardExtensionElement newMessage = rewriteElement.getFirstElement(Message.ELEMENT);
                String conversation = rewriteElement.getAttributeValue(CONVERSATION_ATTRIBUTE);
                String by = rewriteElement.getAttributeValue(BY_ATTRIBUTE);
                String stanzaId = rewriteElement.getAttributeValue(ID_ATTRIBUTE);
                String stamp = newMessage.getFirstElement(REPLACED_STAMP_ELEMENT, NAMESPACE).getAttributeValue(STAMP_ATTRIBUTE);
                String body = newMessage.getFirstElement(BODY_MESSAGE_ELEMENT).getText();
                handleIncomingRewriteMessage(stanzaId, conversation, stamp, body);

            }
        }
    }

}
