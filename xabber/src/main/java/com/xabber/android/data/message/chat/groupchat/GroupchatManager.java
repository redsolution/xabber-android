package com.xabber.android.data.message.chat.groupchat;

import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatPinnedMessageElement;
import com.xabber.android.data.extension.groupchat.GroupchatPresence;
import com.xabber.android.data.extension.groupchat.GroupchatUpdateIQ;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

//todo add and implement other listeners

public class GroupchatManager implements OnPacketListener {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";

    static private GroupchatManager instance;

    public static GroupchatManager getInstance() {
        if (instance == null)
            instance = new GroupchatManager();
        return instance;
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof Presence && packet.hasExtension(GroupchatPresence.NAMESPACE)){
            try {
                GroupchatPresence presence = (GroupchatPresence) packet.getExtension(GroupchatPresence.NAMESPACE);

                AccountJid accountJid = AccountJid.from(packet.getTo().toString());
                ContactJid contactJid = ContactJid.from(packet.getFrom());

                if (ChatManager.getInstance().getChat(accountJid, contactJid) instanceof RegularChat){
                    ChatManager.getInstance().removeChat(accountJid, contactJid);
                    ChatManager.getInstance().createGroupChat(accountJid, contactJid);
                }

                GroupChat groupChat = (GroupChat) ChatManager.getInstance().getChat(accountJid, contactJid);

                groupChat.setPinnedMessage(MessageRepository
                        .getMessageFromRealmByStanzaId(presence.getPinnedMessageId()));
                groupChat.setDescription(presence.getDescription());
                groupChat.setName(presence.getName());
                groupChat.setIndexType(presence.getIndex());
                //todo etc...

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        }
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

                GroupchatUpdateIQ iq = new GroupchatUpdateIQ(account.toString(), contact,
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
