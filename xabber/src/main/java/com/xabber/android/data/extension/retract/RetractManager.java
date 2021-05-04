package com.xabber.android.data.extension.retract;

import android.util.Pair;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.ui.OnMessageUpdatedListener;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class RetractManager implements OnPacketListener {

    public static final String NAMESPACE = "https://xabber.com/protocol/rewrite";
    private static final String NAMESPACE_NOTIFY = NAMESPACE.concat("#notify");
    private static final String RETRACT_MESSAGE_ELEMENT = "retract-message";
    private static final String REWRITE_MESSAGE_ELEMENT = "replace";
    private static final String REPLACED_STAMP_ELEMENT = "replaced";
    private static final String BY_ATTRIBUTE = "by";
    private static final String CONVERSATION_ATTRIBUTE = "conversation";
    private static final String ID_ATTRIBUTE = "id";
    private static final String STAMP_ATTRIBUTE = "stamp";
    private static final String LOG_TAG = "RRRManager";
    private static RetractManager instance;

    public static RetractManager getInstance() {
        if (instance == null)
            instance = new RetractManager();
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
            if (RetractManager.getInstance().isSupported(accountJid))
                subscribeForUpdates(accountJid);
        }
    }

    public boolean subscribeForUpdates(final AccountJid accountJid) {
        XMPPTCPConnection xmpptcpConnection = AccountManager.getInstance().getAccount(accountJid).getConnection();
        try {
            xmpptcpConnection.sendIqWithResponseCallback(new SubscribeUpdatesIQ(accountJid), packet -> {
                if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.error)) {
                    LogManager.d(LOG_TAG, "Failed to subscribe for RRR updates for account " + accountJid + "! Received error IQ");
                }
            });
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
        return true;
    }

    public void tryToRetractMessage(AccountJid accountJid, List<String> list, boolean symmetrically) {
        for (String id : list)
            tryToRetractMessage(accountJid, id, symmetrically);
    }

    public void tryToRetractMessage(final AccountJid accountJid, final String id, final boolean symmetrically) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() ->  {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                    final MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, id).findFirst();
                    try {
                        RetractMessageIQ iq = new RetractMessageIQ(accountJid.toString(),
                                messageRealmObject.getStanzaId(), symmetrically);
                        AccountManager.getInstance().getAccount(accountJid).getConnection()
                                .sendIqWithResponseCallback(iq, packet ->  {
                                    if (packet instanceof IQ) {
                                        if (((IQ) packet).getType().equals(IQ.Type.error)){
                                            LogManager.d(LOG_TAG, "Failed to retract message");
                                            Toast.makeText(Application.getInstance().getBaseContext(),
                                                    "Failed to retract message", Toast.LENGTH_SHORT).show();
                                        }
                                        if (((IQ) packet).getType().equals(IQ.Type.result)){
                                            LogManager.d(LOG_TAG, "Message successfully retracted");
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                    //TODO THIS IS REALLY BAD PLACE FOR DELETING SHOULD REPLACE INTO STANZA LISTENER
                    messageRealmObject.deleteFromRealm();
                });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public void sendEditedMessage(final AccountJid accountJid, final ContactJid contactJid,
                                  final String uniqueId, final String text){
        Application.getInstance().runInBackgroundNetworkUserRequest(() ->  {
            Realm realm = null;
            final Message[] message = {new Message()};
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                        MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                                .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, uniqueId)
                                .findFirst();
                        if (messageRealmObject != null) {
                            try {
                                message[0] = PacketParserUtils.parseStanza(messageRealmObject.getOriginalStanza());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (ReferencesManager.messageHasMutableReferences(message[0])) {
                                String body = message[0].getBody();
                                message[0].removeBody("");
                                String originalText = messageRealmObject.getText();
                                int lastMutable = body.length() - originalText.length();
                                if (lastMutable < 0) {
                                    lastMutable = 0;
                                    LogManager.exception("RrrManager", new Throwable("error in counting the end of the mutable reference"));
                                }
                                body = body.substring(0, lastMutable).concat(text);
                                message[0].setBody(body);
                            } else {
                                message[0].removeBody("");
                                message[0].setBody(text);
                            }
                            message[0].setStanzaId(messageRealmObject.getStanzaId());
                            messageRealmObject.setText(text);
                            messageRealmObject.setOriginalStanza(message[0].toXML().toString());
                            for (OnMessageUpdatedListener listener :
                                    Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                                listener.onAction();
                            }
                        }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
            //now try to send packet to server
            try {
                ReplaceMessageIQ replaceMessageIQ = new ReplaceMessageIQ(message[0].getStanzaId(),
                        accountJid.toString(), message[0]);
                AccountManager.getInstance().getAccount(accountJid).getConnection()
                        .sendIqWithResponseCallback(replaceMessageIQ, packet -> {
                                //TODO implement iq replies handler
                        });
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
        });
    }

    public void sendRetractAllMessagesRequest(final AccountJid accountJid, final ContactJid contactJid,
                                              final boolean symmetric){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            RetractAllMessagesIQ retractAllMessagesIQ = new RetractAllMessagesIQ(contactJid.toString(),
                    symmetric);
            try {
                AccountManager.getInstance().getAccount(accountJid).getConnection()
                        .sendIqWithResponseCallback(retractAllMessagesIQ, packet -> {
                            if (packet instanceof IQ ) {
                                if (((IQ) packet).getType().equals(IQ.Type.error))
                                    LogManager.d(LOG_TAG, "Failed to retract message");
                                else if (((IQ) packet).getType().equals(IQ.Type.result)){
                                    LogManager.d(LOG_TAG, "Message successfully retracted");
                                }
                            }
                        });
            } catch (Exception e) {LogManager.exception(LOG_TAG, e); }
        });
        //TODO ALSO AWFUL PLACE FOR DELETING!
        MessageManager.getInstance().clearHistory(accountJid, contactJid);
    }

    private void handleIncomingRetractMessage(final String id, final String by,
                                              final String conversation) {
        Application.getInstance().runInBackgroundUserRequest(() ->  {
                Realm realm = null;
                try {
                    realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                    realm.executeTransaction(realm1 ->  {
                            MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                                    .equalTo(MessageRealmObject.Fields.USER, conversation)
                                    .equalTo(MessageRealmObject.Fields.STANZA_ID, id)
                                    .findFirst();
                            if (messageRealmObject != null)
                                messageRealmObject.deleteFromRealm();
                            for (OnMessageUpdatedListener listener : Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                                listener.onAction();
                            }
                    });
                } catch (Exception e){
                    LogManager.exception(LOG_TAG, e);
                } finally { if (realm != null) realm.close(); }
        });
    }

    private void handleIncomingRewriteMessage(final String stanzaId, final String conversation,
                                              final String stamp, final String body,
                                              final String markupText, final String originalStanza,
                                              final RealmList<AttachmentRealmObject> attachmentRealmObjects) {
        //TODO pay attention to this code
        Application.getInstance().runInBackgroundUserRequest(() ->  {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                    MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.USER, conversation)
                            .equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId)
                            .findFirst();
                    if (messageRealmObject != null) {
                        if (markupText != null)
                            messageRealmObject.setMarkupText(markupText);
                        if (originalStanza != null)
                            messageRealmObject.setOriginalStanza(originalStanza);
                        if (attachmentRealmObjects != null)
                            messageRealmObject.setAttachmentRealmObjects(attachmentRealmObjects);
                        if (body != null)
                            messageRealmObject.setText(body);
                        if (stamp != null)
                            messageRealmObject.setEditedTimestamp(StringUtils
                                    .parseReceivedReceiptTimestampString(stamp).getTime());
                    }
                    for (OnMessageUpdatedListener listener : Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                        listener.onAction();
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {

        if (packet instanceof Message && ((Message) packet).getType().equals(Message.Type.headline)) {
            try {
                if (packet.hasExtension(RETRACT_MESSAGE_ELEMENT, NAMESPACE_NOTIFY)) {
                    StandardExtensionElement retractElement = packet
                            .getExtension(RETRACT_MESSAGE_ELEMENT, NAMESPACE_NOTIFY);
                    String by = retractElement.getAttributeValue(BY_ATTRIBUTE);
                    String conversation = retractElement.getAttributeValue(CONVERSATION_ATTRIBUTE);
                    String id = retractElement.getAttributeValue(ID_ATTRIBUTE);
                    String to = packet.getTo().toString();
                    MessageNotificationManager.getInstance().removeChat(AccountJid.from(to), ContactJid.from(conversation));
                    handleIncomingRetractMessage(id, by, conversation);
                    return;
                }

                if (packet.hasExtension(REWRITE_MESSAGE_ELEMENT, NAMESPACE_NOTIFY)) {
                    StandardExtensionElement rewriteElement = packet
                            .getExtension(REWRITE_MESSAGE_ELEMENT, NAMESPACE_NOTIFY);
                    StandardExtensionElement newMessage = rewriteElement.getFirstElement(Message.ELEMENT);
                    String conversation = rewriteElement.getAttributeValue(CONVERSATION_ATTRIBUTE);
                    String stanzaId = rewriteElement.getAttributeValue(ID_ATTRIBUTE);
                    String stamp = newMessage.getFirstElement(REPLACED_STAMP_ELEMENT, NAMESPACE)
                            .getAttributeValue(STAMP_ATTRIBUTE);
                    String originalStanza = newMessage.toXML().toString();
                    Message message = (Message) PacketParserUtils.parseStanza(newMessage.toXML().toString());
                    String text = message.getBody();
                    Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
                    text = bodies.first;
                    String markupText = bodies.second;
                    RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message);
                    //RealmList<ForwardId> forwardIds = parseForwardedMessage(ui, message, uid);
                    String forwardComment = ForwardManager.parseForwardComment(message);
                    if (forwardComment != null) text = forwardComment;
                    handleIncomingRewriteMessage(stanzaId, conversation, stamp, text, markupText,
                            originalStanza, attachmentRealmObjects);
                }
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
        }
    }

}
