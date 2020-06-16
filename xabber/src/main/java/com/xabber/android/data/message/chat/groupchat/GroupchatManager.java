package com.xabber.android.data.message.chat.groupchat;

import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.groupchat.GroupchatPinnedMessageElement;
import com.xabber.android.data.extension.groupchat.GroupchatUpdateIQ;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

//todo add and implement other listeners

public class GroupchatManager {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";

    static private GroupchatManager instance;

    public static GroupchatManager getInstance() {
        if (instance == null)
            instance = new GroupchatManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection connection) {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection)
                    .serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid).getConnection());
    }

    public void sendPinMessageRequest(MessageRealmObject message){
        //todo add privilege checking

        final String stanzaId = message.getStanzaId();
        final AccountJid account = message.getAccount();
        final String contact = message.getUser().toString();

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            try {
                GroupchatPinnedMessageElement groupchatPinnedMessageElement =
                        new GroupchatPinnedMessageElement(stanzaId);

                GroupchatUpdateIQ iq = new GroupchatUpdateIQ(account.toString(),
                        contact,
                        groupchatPinnedMessageElement);

                AccountManager.getInstance().getAccount(account).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {

                            if (packet instanceof IQ) {
                                if (((IQ) packet).getType().equals(IQ.Type.error)){
                                    LogManager.d(LOG_TAG, "Failed to pin message");
                                    Toast.makeText(Application.getInstance().getBaseContext(),
                                            "Failed to retract message", Toast.LENGTH_SHORT).show();
                                }
                                if (((IQ) packet).getType().equals(IQ.Type.result)){
                                    LogManager.d(LOG_TAG, "Message successfully pinned");
                                    Application.getInstance().runOnUiThread(() ->
                                            MessageRepository.setMessagePinned(message));
                                }
                            }

                        });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

}
