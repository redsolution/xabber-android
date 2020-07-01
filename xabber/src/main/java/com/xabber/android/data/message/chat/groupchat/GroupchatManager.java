package com.xabber.android.data.message.chat.groupchat;

import android.widget.Toast;

import androidx.annotation.IntDef;

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
import com.xabber.android.data.extension.groupchat.GroupchatEchoExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatPinnedMessageElement;
import com.xabber.android.data.extension.groupchat.GroupchatPresence;
import com.xabber.android.data.extension.groupchat.GroupchatUpdateIQ;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistItemElement;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistQueryIQ;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistResultIQ;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistUnblockIQ;
import com.xabber.android.data.extension.groupchat.invite.GroupchatInviteListQueryIQ;
import com.xabber.android.data.extension.groupchat.invite.GroupchatInviteListResultIQ;
import com.xabber.android.data.extension.groupchat.invite.GroupchatInviteListRevokeIQ;
import com.xabber.android.data.extension.groupchat.invite.GroupchatInviteRequestIQ;
import com.xabber.android.data.extension.groupchat.invite.OnGroupchatSelectorListToolbarActionResult;
import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ;
import com.xabber.android.data.extension.groupchat.members.GroupchatMembersResultIQ;
import com.xabber.android.data.extension.mam.NextMamManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.xmpp.sid.UniqStanzaHelper;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupchatManager implements OnPacketListener {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";
    private static Set<GroupchatRequest> groupchatRequests = new ConcurrentSkipListSet<>();

    static private GroupchatManager instance;

    public static GroupchatManager getInstance() {
        if (instance == null)
            instance = new GroupchatManager();
        return instance;
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof Presence && packet.hasExtension(GroupchatPresence.NAMESPACE)) {
            processPresence(connection, packet);
        } else if (packet instanceof Message
                && ((Message) packet).getType().equals(Message.Type.headline)
                && packet.hasExtension(GroupchatEchoExtensionElement.ELEMENT, GroupchatEchoExtensionElement.NAMESPACE)){
            processHeadlineEchoMessage(connection, packet);
        }
    }

    private void processHeadlineEchoMessage(ConnectionItem connectionItem, Stanza packet){
        try{
            StandardExtensionElement echoElement = (StandardExtensionElement) packet.getExtensions().get(0);
            Message message = PacketParserUtils.parseStanza(echoElement.getElements().get(0).toXML().toString());
            String originId = UniqStanzaHelper.getOriginId(message);
            String stanzaId = UniqStanzaHelper.getContactStanzaId(message);
            MessageRepository.setStanzaIdByOriginId(originId, stanzaId);
        } catch (Exception e){
            LogManager.exception(LOG_TAG, e);
        }
    }

    private void processPresence(ConnectionItem connection, Stanza packet){
        try {
            GroupchatPresence presence = (GroupchatPresence) packet.getExtension(GroupchatPresence.NAMESPACE);

            AccountJid accountJid = AccountJid.from(packet.getTo().toString());
            ContactJid contactJid = ContactJid.from(packet.getFrom());

            if (ChatManager.getInstance().getChat(accountJid, contactJid) instanceof RegularChat) {
                ChatManager.getInstance().removeChat(accountJid, contactJid);
                ChatManager.getInstance().createGroupChat(accountJid, contactJid);
            }

            GroupChat groupChat = (GroupChat) ChatManager.getInstance().getChat(accountJid, contactJid);

            if (presence.getPinnedMessageId() != null
                    && !presence.getPinnedMessageId().isEmpty()
                    && !presence.getPinnedMessageId().equals("0")){
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
                                if (((IQ) packet).getType().equals(IQ.Type.error)) {
                                    LogManager.d(LOG_TAG, "Failed to pin message");
                                    Toast.makeText(Application.getInstance().getBaseContext(),
                                            "Failed to retract message", Toast.LENGTH_SHORT).show();
                                }
                                if (((IQ) packet).getType().equals(IQ.Type.result)) {
                                    LogManager.d(LOG_TAG, "Message successfully pinned");
                                }
                            }

                        });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendGroupchatInvitations(AccountJid account, ContactJid groupchatJid, List<ContactJid> contactsToInvite, String reason) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();

                    for (ContactJid invite : contactsToInvite) {
                        GroupchatInviteRequestIQ requestIQ = new GroupchatInviteRequestIQ(groupchatJid, invite);
                        requestIQ.setLetGroupchatSendInviteMessage(true);
                        if (reason != null && !reason.isEmpty()) {
                            requestIQ.setReason(reason);
                        }
                        try {
                            connection.sendStanza(requestIQ);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void requestGroupchatMembers(AccountJid account, ContactJid groupchatJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                ArrayList<GroupchatMember> list = ((GroupChat) chat).getMembers();
                if (list != null && list.size() > 0) {
                    Application.getInstance().runOnUiThread(() -> {
                        // notify listeners with the locally saved list of members
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
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
    public void requestGroupchatInvitationsList(AccountJid account, ContactJid groupchatJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                if (checkIfHasActiveInviteListRequest(account, groupchatJid)) {
                    return;
                }
                addInviteListRequest(account, groupchatJid);

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();
                    GroupchatInviteListQueryIQ queryIQ = new GroupchatInviteListQueryIQ(groupchatJid);
                    GroupchatInvitesResultListener listener = new GroupchatInvitesResultListener(account, groupchatJid);
                    try {
                        connection.sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                        removeInviteListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        removeInviteListRequest(account, groupchatJid);
                    }
                }
            } else {
                removeInviteListRequest(account, groupchatJid);
            }
        });
    }
    public void requestGroupchatBlocklistList(AccountJid account, ContactJid groupchatJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                if (checkIfHasActiveBlockListRequest(account, groupchatJid)) {
                    return;
                }
                addBlockListRequest(account, groupchatJid);

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();
                    GroupchatBlocklistQueryIQ queryIQ = new GroupchatBlocklistQueryIQ(groupchatJid);
                    GroupchatBlocklistResultListener listener = new GroupchatBlocklistResultListener(account, groupchatJid);
                    try {
                        connection.sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                        removeBlockListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        removeBlockListRequest(account, groupchatJid);
                    }
                }
            } else {
                removeBlockListRequest(account, groupchatJid);
            }
        });
    }

    public void revokeGroupchatInvitation(AccountJid account, ContactJid groupchatJid, String inviteJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                GroupchatInviteListRevokeIQ revokeIQ =
                        new GroupchatInviteListRevokeIQ(groupchatJid, inviteJid);

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    accountItem.getConnection().sendIqWithResponseCallback(revokeIQ, packet -> {
                        if (packet instanceof IQ) {
                            final boolean success;
                            if (IQ.Type.result.equals(((IQ) packet).getType())) {
                                success = true;
                                AbstractChat chat = ChatManager
                                        .getInstance().getChat(account, groupchatJid);
                                if (chat instanceof GroupChat) {
                                    ((GroupChat) chat).getListOfInvites().remove(inviteJid);
                                }
                            } else {
                                success = false;
                            }
                            Application.getInstance().runOnUiThread(() -> {
                                for (OnGroupchatSelectorListToolbarActionResult listener :
                                        Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                    if (success) {
                                        listener.onActionSuccess(account, groupchatJid, Collections.singletonList(inviteJid));
                                    } else {
                                        listener.onActionFailure(account, groupchatJid, Collections.singletonList(inviteJid));
                                    }
                                }
                            });
                        }
                    }, exception -> Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatSelectorListToolbarActionResult listener :
                                Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                            listener.onActionFailure(account, groupchatJid, Collections.singletonList(inviteJid));
                        }
                    }));
                }
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }
    public void revokeGroupchatInvitations(AccountJid account, ContactJid groupchatJid, Set<String> inviteJids) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            AccountItem accountItem = AccountManager.getInstance().getAccount(account);
            if (accountItem == null) return;

            ArrayList<String> failedRevokeRequests = new ArrayList<>();
            ArrayList<String> successfulRevokeRequests = new ArrayList<>();

            AtomicInteger unfinishedRequestCount = new AtomicInteger(inviteJids.size());
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);

            final GroupChat groupChat;
            if (chat instanceof GroupChat) {
                groupChat = (GroupChat) chat;
            } else {
                groupChat = null;
            }

            for (String inviteJid : inviteJids) {
                try {
                    GroupchatInviteListRevokeIQ revokeIQ =
                            new GroupchatInviteListRevokeIQ(groupchatJid, inviteJid);
                    accountItem.getConnection().sendIqWithResponseCallback(revokeIQ, packet -> {
                        if (packet instanceof IQ) {
                            if (groupChat != null) groupChat.getListOfInvites().remove(inviteJid);
                            successfulRevokeRequests.add(inviteJid);
                            unfinishedRequestCount.getAndDecrement();
                            if (unfinishedRequestCount.get() == 0) {
                                Application.getInstance().runOnUiThread(() -> {
                                    for (OnGroupchatSelectorListToolbarActionResult listener :
                                            Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                        if (failedRevokeRequests.size() == 0) {
                                            listener.onActionSuccess(account, groupchatJid, successfulRevokeRequests);
                                        } else if (successfulRevokeRequests.size() > 0) {
                                            listener.onPartialSuccess(account, groupchatJid, successfulRevokeRequests, failedRevokeRequests);
                                        } else {
                                            listener.onActionFailure(account, groupchatJid, failedRevokeRequests);
                                        }
                                    }
                                });
                            }
                        }
                    }, exception -> {
                        failedRevokeRequests.add(inviteJid);
                        unfinishedRequestCount.getAndDecrement();
                        if (unfinishedRequestCount.get() == 0) {
                            Application.getInstance().runOnUiThread(() -> {
                                for (OnGroupchatSelectorListToolbarActionResult listener :
                                        Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                    if (successfulRevokeRequests.size() > 0) {
                                        listener.onPartialSuccess(account, groupchatJid, successfulRevokeRequests, failedRevokeRequests);
                                    } else {
                                        listener.onActionFailure(account, groupchatJid, failedRevokeRequests);
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    LogManager.exception(LOG_TAG, e);
                    failedRevokeRequests.add(inviteJid);
                    unfinishedRequestCount.getAndDecrement();
                }
            }
        });
    }

    public void unblockGroupchatBlockedElement(AccountJid account, ContactJid groupchatJid, GroupchatBlocklistItemElement blockedElement) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                GroupchatBlocklistUnblockIQ revokeIQ =
                        new GroupchatBlocklistUnblockIQ(groupchatJid, blockedElement);

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    accountItem.getConnection().sendIqWithResponseCallback(revokeIQ, packet -> {
                        if (packet instanceof IQ) {
                            final boolean success;
                            if (IQ.Type.result.equals(((IQ) packet).getType())) {
                                success = true;
                                AbstractChat chat = ChatManager
                                        .getInstance().getChat(account, groupchatJid);
                                if (chat instanceof GroupChat) {
                                    ((GroupChat) chat).getListOfBlockedElements().remove(blockedElement);
                                }
                            } else {
                                success = false;
                            }
                            Application.getInstance().runOnUiThread(() -> {
                                for (OnGroupchatSelectorListToolbarActionResult listener :
                                        Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                    if (success) {
                                        listener.onActionSuccess(account, groupchatJid, Collections.singletonList(blockedElement.getBlockedItem()));
                                    } else {
                                        listener.onActionFailure(account, groupchatJid, Collections.singletonList(blockedElement.getBlockedItem()));
                                    }
                                }
                            });
                        }
                    }, exception -> Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatSelectorListToolbarActionResult listener :
                                Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                            listener.onActionFailure(account, groupchatJid, Collections.singletonList(blockedElement.getBlockedItem()));
                        }
                    }));
                }
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });

    }
    public void unblockGroupchatBlockedElements(AccountJid account, ContactJid groupchatJid, List<GroupchatBlocklistItemElement> blockedElements) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            AccountItem accountItem = AccountManager.getInstance().getAccount(account);
            if (accountItem == null) return;

            ArrayList<String> failedUnblockRequests = new ArrayList<>();
            ArrayList<String> successfulUnblockRequests = new ArrayList<>();

            AtomicInteger unfinishedRequestCount = new AtomicInteger(blockedElements.size());
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);

            final GroupChat groupChat;
            if (chat instanceof GroupChat) {
                groupChat = (GroupChat) chat;
            } else {
                groupChat = null;
            }

            for (GroupchatBlocklistItemElement blockedElement : blockedElements) {
                try {
                    GroupchatBlocklistUnblockIQ revokeIQ =
                            new GroupchatBlocklistUnblockIQ(groupchatJid, blockedElement);
                    accountItem.getConnection().sendIqWithResponseCallback(revokeIQ, packet -> {
                        if (packet instanceof IQ) {
                            if (groupChat != null) groupChat.getListOfBlockedElements().remove(blockedElement);
                            successfulUnblockRequests.add(blockedElement.getBlockedItem());
                            unfinishedRequestCount.getAndDecrement();
                            if (unfinishedRequestCount.get() == 0) {
                                Application.getInstance().runOnUiThread(() -> {
                                    for (OnGroupchatSelectorListToolbarActionResult listener :
                                            Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                        if (failedUnblockRequests.size() == 0) {
                                            listener.onActionSuccess(account, groupchatJid, successfulUnblockRequests);
                                        } else if (successfulUnblockRequests.size() > 0) {
                                            listener.onPartialSuccess(account, groupchatJid, successfulUnblockRequests, failedUnblockRequests);
                                        } else {
                                            listener.onActionFailure(account, groupchatJid, failedUnblockRequests);
                                        }
                                    }
                                });
                            }
                        }
                    }, exception -> {
                        failedUnblockRequests.add(blockedElement.getBlockedItem());
                        unfinishedRequestCount.getAndDecrement();
                        if (unfinishedRequestCount.get() == 0) {
                            Application.getInstance().runOnUiThread(() -> {
                                for (OnGroupchatSelectorListToolbarActionResult listener :
                                        Application.getInstance().getUIListeners(OnGroupchatSelectorListToolbarActionResult.class)) {
                                    if (successfulUnblockRequests.size() > 0) {
                                        listener.onPartialSuccess(account, groupchatJid, successfulUnblockRequests, failedUnblockRequests);
                                    } else {
                                        listener.onActionFailure(account, groupchatJid, failedUnblockRequests);
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    LogManager.exception(LOG_TAG, e);
                    failedUnblockRequests.add(blockedElement.getBlockedItem());
                    unfinishedRequestCount.getAndDecrement();
                }
            }
        });
    }

    public static boolean checkIfHasActiveInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, InviteListRequest));
    }
    public static boolean checkIfHasActiveBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, BlockListRequest));
    }
    public static boolean checkIfHasActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, MemberListRequest));
    }

    private static void removeInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, InviteListRequest));
    }
    private static void removeActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, MemberListRequest));
    }
    private static void removeBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, BlockListRequest));
    }

    private static void addInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, InviteListRequest));
    }
    private static void addMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, MemberListRequest));
    }
    private static void addBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, BlockListRequest));
    }

    private static GroupchatRequest createRequest(AccountJid account, ContactJid groupchatJid,
                                                  @GroupchatRequestTypes int requestType) {
        GroupchatRequest request = null;
        switch (requestType) {
            case MemberListRequest:
                request = new GroupchatMemberListRequest(account, groupchatJid);
                break;
            case InviteListRequest:
                request = new GroupchatInviteListRequest(account, groupchatJid);
                break;
            case BlockListRequest:
                request = new GroupchatBlockListRequest(account, groupchatJid);
                break;
            default:
                throw new RuntimeException("Wrong groupchat request type = " + requestType);
        }
        return request;
    }

    protected static class GroupchatInviteListRequest extends GroupchatRequest {
        GroupchatInviteListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, InviteListRequest);
        }
    }

    protected static class GroupchatBlockListRequest extends GroupchatRequest {
        GroupchatBlockListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, BlockListRequest);
        }
    }

    protected static class GroupchatMemberListRequest extends GroupchatRequest {
        GroupchatMemberListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, MemberListRequest);
        }
    }

    private static class GroupchatRequest implements Comparable<GroupchatRequest> {
        private AccountJid accountJid;
        private ContactJid groupchatJid;
        private int requestType;
        private int hash = 0;

        GroupchatRequest(AccountJid accountJid, ContactJid groupchatJid, int requestType) {
            this.accountJid = accountJid;
            this.groupchatJid = groupchatJid;
            this.requestType = requestType;
        }

        @Override
        public int hashCode() {
            int result = hash;
            if (result == 0) {
                result = 17;

                result = result * 31 + accountJid.getBareJid().toString().hashCode();
                result = result * 31 + groupchatJid.getBareJid().toString().hashCode();
                result = result * 31 + requestType;
                hash = result;
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupchatRequest that = (GroupchatRequest) o;
            return requestType == that.requestType &&
                    accountJid.getBareJid().equals(that.accountJid.getBareJid()) &&
                    groupchatJid.getBareJid().equals(that.groupchatJid.getBareJid());
        }

        @Override
        public int compareTo(GroupchatRequest o) {
            return Integer.compare(hashCode(), o.hashCode());
        }
    }

    private static class GroupchatInvitesResultListener implements StanzaListener {
        private AccountJid account;
        private ContactJid groupchatJid;

        GroupchatInvitesResultListener(AccountJid account, ContactJid groupchatJid) {
            this.account = account;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
            if (packet instanceof GroupchatInviteListResultIQ) {
                GroupchatInviteListResultIQ resultIQ = (GroupchatInviteListResultIQ) packet;

                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                        && account.getBareJid().equals(packet.getTo().asBareJid())) {

                    ArrayList<String> listOfInvites = resultIQ.getListOfInvitedJids();

                    AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
                    if (chat instanceof GroupChat) {
                        if (listOfInvites != null) {
                            ((GroupChat) chat).setListOfInvites(listOfInvites);
                        } else {
                            ((GroupChat) chat).setListOfInvites(new ArrayList<>());
                        }
                    }

                    removeInviteListRequest(account, groupchatJid);
                    Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onGroupchatInvitesReceived(account, groupchatJid, listOfInvites);
                        }
                    });

                }
                removeInviteListRequest(account, groupchatJid);
            }
        }
    }

    private static class GroupchatBlocklistResultListener implements StanzaListener {

        private AccountJid account;
        private ContactJid groupchatJid;

        GroupchatBlocklistResultListener(AccountJid account, ContactJid groupchatJid) {
            this.account = account;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
            if (packet instanceof GroupchatBlocklistResultIQ) {
                GroupchatBlocklistResultIQ resultIQ = (GroupchatBlocklistResultIQ) packet;

                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                        && account.getBareJid().equals(packet.getTo().asBareJid())) {

                    ArrayList<GroupchatBlocklistItemElement> blockList = resultIQ.getBlockedItems();

                    AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
                    if (chat instanceof GroupChat) {
                        if (blockList != null) {
                            ((GroupChat) chat).setListOfBlockedElements(blockList);
                        } else {
                            ((GroupChat) chat).setListOfBlockedElements(new ArrayList<>());
                        }
                    }

                    removeBlockListRequest(account, groupchatJid);
                    Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onGroupchatBlocklistReceived(account, groupchatJid, blockList);
                        }
                    });

                }
                removeBlockListRequest(account, groupchatJid);
            }
        }
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
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
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

        GroupchatPresenceUpdatedEvent(AccountJid account, ContactJid groupchatJid) {
            this.account = account;
            this.groupJid = groupchatJid;
        }

        public AccountJid getAccount() {
            return account;
        }

        public ContactJid getGroupJid() {
            return groupJid;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MemberListRequest, InviteListRequest, BlockListRequest})
    protected @interface GroupchatRequestTypes {
    }

    static final int MemberListRequest = 1;
    static final int InviteListRequest = 2;
    static final int BlockListRequest = 3;
}
