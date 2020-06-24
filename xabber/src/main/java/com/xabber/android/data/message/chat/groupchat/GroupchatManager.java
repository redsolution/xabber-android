package com.xabber.android.data.message.chat.groupchat;

import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realmobjects.GroupchatUserRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatMembersQueryIQ;
import com.xabber.android.data.extension.groupchat.GroupchatMembersResultIQ;
import com.xabber.android.data.extension.groupchat.GroupchatPinnedMessageElement;
import com.xabber.android.data.extension.groupchat.GroupchatPresence;
import com.xabber.android.data.extension.groupchat.GroupchatUpdateIQ;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.groupchat.OnGroupchatMembersListener;
import com.xabber.android.data.extension.mam.NextMamManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

//todo add and implement other listeners

public class GroupchatManager implements OnPacketListener {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";
    private static Set<String> groupchatMemberListRequests = new ConcurrentSkipListSet<>();

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

                if (presence.getPinnedMessageId() != null && !presence.getPinnedMessageId().isEmpty()){
                    MessageRealmObject pinnedMessage = MessageRepository
                            .getMessageFromRealmByStanzaId(presence.getPinnedMessageId());
                    if (pinnedMessage == null || pinnedMessage.getTimestamp() == null){

                        NextMamManager.getInstance().requestSingleMessageAsync(connection,
                                groupChat, presence.getPinnedMessageId());
                    } else groupChat.setPinnedMessage(pinnedMessage);
                }

                groupChat.setDescription(presence.getDescription());
                groupChat.setName(presence.getName());
                groupChat.setIndexType(presence.getIndex());
                groupChat.setPrivacyType(presence.getPrivacy());
                groupChat.setMembershipType(presence.getMembership());
                groupChat.setNumberOfMembers(presence.getAllMembers());
                groupChat.setNumberOfOnlineMembers(presence.getPresentMembers());

                EventBus.getDefault().post(new GroupchatPresenceUpdatedEvent(accountJid, contactJid));
                //todo etc...

                ChatManager.getInstance().saveOrUpdateChatDataToRealm(groupChat);
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

    public void sendUnPinMessageRequest(GroupChat groupChat){
        //todo add privilege checking

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                GroupchatPinnedMessageElement groupchatPinnedMessageElement =
                        new GroupchatPinnedMessageElement("");

                GroupchatUpdateIQ iq = new GroupchatUpdateIQ(groupChat.getAccount().getFullJid(),
                        groupChat.getUser().getJid(), groupchatPinnedMessageElement);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {

                            if (packet instanceof IQ) {
                                if (((IQ) packet).getType().equals(IQ.Type.error)){
                                    LogManager.d(LOG_TAG, "Failed to pin message");
                                    Toast.makeText(Application.getInstance().getBaseContext(),
                                            "Failed to retract message", Toast.LENGTH_SHORT).show();
                                }
                                if (((IQ) packet).getType().equals(IQ.Type.result)){
                                    LogManager.d(LOG_TAG, "Message successfully unpinned");
                                }
                            }

                        });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendPinMessageRequest(MessageRealmObject message){
        //todo add privilege checking

        final String stanzaId = message.getStanzaId();
        final AccountJid account = message.getAccount();
        final Jid contact = message.getUser().getJid();

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            try {
                GroupchatPinnedMessageElement groupchatPinnedMessageElement =
                        new GroupchatPinnedMessageElement(stanzaId);

                GroupchatUpdateIQ iq = new GroupchatUpdateIQ(account.getFullJid(), contact,
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
                                }
                            }

                        });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public static boolean checkIfHasActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatMemberListRequests.contains(modifyRequestData(account, groupchatJid));
    }

    private static void removeActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatMemberListRequests.remove(modifyRequestData(account, groupchatJid));
    }

    private static void addMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatMemberListRequests.add(modifyRequestData(account, groupchatJid));
    }

    private static String modifyRequestData(AccountJid account, ContactJid groupchatJid) {
        return account.getBareJid().toString() + groupchatJid.getBareJid().toString();
    }

    public void requestGroupchatMembers(AccountJid account, ContactJid groupchatJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                ArrayList<GroupchatMember> list = ((GroupChat) chat).getMembers();
                if (list != null && list.size() > 0) {
                    Application.getInstance().runOnUiThread(() -> {
                        // notify listeners with the locally saved list of members
                        for (OnGroupchatMembersListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatMembersListener.class)) {
                            listener.onGroupchatMembersReceived(account, groupchatJid, list);
                        }
                    });
                }

                //if (checkIfHasActiveMemberListRequest(account, groupchatJid)) {
                //    return;
                //}

                addMemberListRequest(account, groupchatJid);

                //String version;
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ(groupchatJid);
                    //version = ((GroupChat) chat).getMembersListVersion();
                    //if (version != null && !version.isEmpty()) {
                    //    queryIQ.setQueryVersion(version);
                    //} else {
                    queryIQ.setQueryVersion("1");
                    //}
                    GroupchatMembersResultListener listener = new GroupchatMembersResultListener(account, groupchatJid);
                    try {
                        connection.sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                        removeActiveMemberListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        removeActiveMemberListRequest(account, groupchatJid);
                    }
                }
            } else {
                removeActiveMemberListRequest(account, groupchatJid);
            }
        });
    }

    private static class GroupchatMembersResultListener implements StanzaListener {

        private AccountJid account;
        private ContactJid groupchatJid;

        public GroupchatMembersResultListener(AccountJid account, ContactJid groupchatJid) {
            this.account = account;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) {
            if (packet instanceof GroupchatMembersResultIQ) {
                GroupchatMembersResultIQ groupchatMembers = (GroupchatMembersResultIQ) packet;

                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                        && account.getBareJid().equals(packet.getTo().asBareJid())) {

                    ArrayList<GroupchatMember> listOfMembers =
                            new ArrayList<>(groupchatMembers.getListOfMembers().size());
                    ArrayList<GroupchatUserRealmObject> listOfRealmMembers =
                            new ArrayList<>(groupchatMembers.getListOfMembers().size());

                    for (GroupchatUserExtension userExtension : groupchatMembers.getListOfMembers()) {
                        listOfMembers.add(GroupchatMemberManager.refUserToUser(userExtension, groupchatJid.getBareJid()));
                    }

                    AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
                    if (chat instanceof GroupChat) {
                        ((GroupChat) chat).setMembers(listOfMembers);
                        ((GroupChat) chat).setMembersListVersion(groupchatMembers.getQueryVersion());
                        chat.requestSaveToRealm();
                    }

                    removeActiveMemberListRequest(account, groupchatJid);
                    Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatMembersListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatMembersListener.class)) {
                            listener.onGroupchatMembersReceived(account, groupchatJid, listOfMembers);
                        }
                    });
                }
                removeActiveMemberListRequest(account, groupchatJid);
            }
        }
    }

    public static class GroupchatPresenceUpdatedEvent {
        private AccountJid account;
        private ContactJid groupJid;
        GroupchatPresenceUpdatedEvent(AccountJid account, ContactJid groupchatJid){
            this.account = account;
            this.groupJid = groupchatJid;
        }
        public AccountJid getAccount() { return account; }
        public ContactJid getGroupJid() { return groupJid; }
    }
}
