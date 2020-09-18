package com.xabber.android.data.message.chat.groupchat;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.repositories.GroupchatMemberRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatMemberExtensionElement;
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
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.BareJid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupchatMemberManager implements OnLoadListener {

    private static final String LOG_TAG = GroupchatMemberManager.class.getSimpleName();

    private static GroupchatMemberManager instance;
    private static Set<GroupchatRequest> groupchatRequests = new ConcurrentSkipListSet<>();
    private final Map<String, GroupchatMember> members = new HashMap<>();

    public static GroupchatMemberManager getInstance() {
        if (instance == null) instance = new GroupchatMemberManager();
        return instance;
    }

    public static GroupchatMember getGroupchatMemberFromGroupchatMemberExtensionElement(
            GroupchatMemberExtensionElement groupchatMemberExtensionElement, BareJid groupchatJid) {

        GroupchatMember user = new GroupchatMember(groupchatMemberExtensionElement.getId());

        if (groupchatJid != null) user.setGroupchatJid(groupchatJid.toString());

        if (groupchatMemberExtensionElement.getAvatarInfo() != null) {
            user.setAvatarHash(groupchatMemberExtensionElement.getAvatarInfo().getId());
            user.setAvatarUrl(groupchatMemberExtensionElement.getAvatarInfo().getUrl().toString());
        }

        user.setLastPresent(groupchatMemberExtensionElement.getLastPresent());
        user.setBadge(groupchatMemberExtensionElement.getBadge());
        user.setJid(groupchatMemberExtensionElement.getJid());
        user.setNickname(groupchatMemberExtensionElement.getNickname());
        user.setRole(groupchatMemberExtensionElement.getRole());

        return user;
    }

    private static GroupchatRequest createRequest(AccountJid account, ContactJid groupchatJid,
                                                  GroupchatRequestTypes requestType) {
        GroupchatRequest request;
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
            case MeRequest:
                request = new GroupchatMeRequest(account, groupchatJid);
                break;
            default:
                throw new RuntimeException("Wrong groupchat request type = " + requestType);
        }
        return request;
    }

    public static boolean checkIfHasActiveInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, GroupchatRequestTypes.InviteListRequest));
    }

    public static boolean checkIfHasActiveBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, GroupchatRequestTypes.BlockListRequest));
    }

    public static boolean checkIfHasActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        return groupchatRequests.contains(createRequest(account, groupchatJid, GroupchatRequestTypes.MemberListRequest));
    }

    public static boolean checkIfHasActiveMeRequest(AccountJid accountJid, ContactJid groupchatJid){
        return groupchatRequests.contains(createRequest(accountJid, groupchatJid, GroupchatRequestTypes.MeRequest));
    }

    private static void removeInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, GroupchatRequestTypes.InviteListRequest));
    }

    private static void removeActiveMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, GroupchatRequestTypes.MemberListRequest));
    }

    private static void removeBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.remove(createRequest(account, groupchatJid, GroupchatRequestTypes.BlockListRequest));
    }

    private static void removeMeRequest(AccountJid accountJid, ContactJid groupchatJid){
        groupchatRequests.remove(createRequest(accountJid, groupchatJid, GroupchatRequestTypes.MeRequest));
    }

    private static void addInviteListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, GroupchatRequestTypes.InviteListRequest));
    }

    private static void addMemberListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, GroupchatRequestTypes.MemberListRequest));
    }

    private static void addBlockListRequest(AccountJid account, ContactJid groupchatJid) {
        groupchatRequests.add(createRequest(account, groupchatJid, GroupchatRequestTypes.BlockListRequest));
    }

    private static void addMeRequest(AccountJid accountJid, ContactJid groupchatJid){
        groupchatRequests.add(createRequest(accountJid, groupchatJid, GroupchatRequestTypes.MeRequest));
    }

    @Override
    public void onLoad() {
        for (GroupchatMember gm : GroupchatMemberRepository.getAllGroupchatMembersFromRealm()){
            this.members.put(gm.getId(), gm);
        }
    }

    public GroupchatMember getGroupchatMemberById(String id) {
        return members.get(id);
    }

    public Collection<GroupchatMember> getGroupchatMembers(ContactJid groupchatJid){
        Collection<GroupchatMember> resultList = new ArrayList<>();
        for (Map.Entry<String, GroupchatMember> entry : members.entrySet()){
            if (entry.getValue().getGroupchatJid().equals(groupchatJid.toString()))
                resultList.add(entry.getValue());
        }
        return resultList;
    }

    public Collection<GroupchatMember> getGroupchatMembers(GroupChat groupChat){
        return getGroupchatMembers(groupChat.getContactJid());
    }

    public void removeGroupchatMember(String id){
        members.remove(id);
        GroupchatMemberRepository.removeGroupchatMemberById(id);
    }

    public void saveGroupchatUser(GroupchatMemberExtensionElement user, BareJid groupchatJid) {
        saveGroupchatUser(user, groupchatJid, System.currentTimeMillis());
    }

    public void saveGroupchatUser(GroupchatMemberExtensionElement user, BareJid groupchatJid,
                                  long timestamp) {

        GroupchatMember groupchatMember = getGroupchatMemberFromGroupchatMemberExtensionElement(user,
                groupchatJid);

        members.put(user.getId(), groupchatMember);
        GroupchatMemberRepository.saveOrUpdateGroupchatMember(groupchatMember);
    }

    public void sendGroupchatInvitations(AccountJid account, ContactJid groupchatJid,
                                         List<ContactJid> contactsToInvite, String reason) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();

                    for (ContactJid invite : contactsToInvite) {
                        GroupchatInviteRequestIQ requestIQ = new GroupchatInviteRequestIQ(groupchatJid, invite);
                        requestIQ.setLetGroupchatSendInviteMessage(true);

                        if (reason != null && !reason.isEmpty())
                            requestIQ.setReason(reason);

                        try {
                            connection.sendStanza(requestIQ);
                        } catch (SmackException.NotConnectedException e) {
                            LogManager.exception(LOG_TAG, e);
                        } catch (InterruptedException e) {
                            LogManager.exception(LOG_TAG, e);
                        }
                    }
                }
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
                        LogManager.exception(LOG_TAG, e);
                        removeInviteListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
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
                        LogManager.exception(LOG_TAG, e);
                        removeBlockListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
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
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
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
                            if (groupChat != null)
                                groupChat.getListOfBlockedElements().remove(blockedElement);
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

    public void requestMe(AccountJid accountJid, ContactJid groupchatJid){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(accountJid, groupchatJid);
            if (chat instanceof GroupChat) {
                ArrayList<GroupchatMember> list = new ArrayList<>(getGroupchatMembers(groupchatJid));
                if (list != null && list.size() > 0) {
                    Application.getInstance().runOnUiThread(() -> {
                        // notify listeners with the locally saved list of members
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onMeReceived(accountJid, groupchatJid);
                        }
                    });
                }

                //if (checkIfHasActiveMemberListRequest(account, groupchatJid)) {
                //    return;
                //}

                addMeRequest(accountJid, groupchatJid);

                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ(groupchatJid);
                    queryIQ.setQueryId("");

                    GroupchatMeResultListener listener = new GroupchatMeResultListener(accountJid, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                        removeMeRequest(accountJid, groupchatJid);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                        removeMeRequest(accountJid, groupchatJid);
                    }
                }
            } else {
                removeMeRequest(accountJid, groupchatJid);
            }
        });
    }

    public void requestGroupchatMembers(AccountJid account, ContactJid groupchatJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                ArrayList<GroupchatMember> list = new ArrayList<>(getGroupchatMembers(groupchatJid));
                if (list != null && list.size() > 0) {
                    Application.getInstance().runOnUiThread(() -> {
                        // notify listeners with the locally saved list of members
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onGroupchatMembersReceived(account, groupchatJid);
                        }
                    });
                }

                //if (checkIfHasActiveMemberListRequest(account, groupchatJid)) {
                //    return;
                //}

                addMemberListRequest(account, groupchatJid);

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ(groupchatJid);
                    String version = ((GroupChat) chat).getMembersListVersion();
                    if (version != null && !version.isEmpty()) {
                        queryIQ.setQueryVersion(version);
                    } else queryIQ.setQueryVersion("1");
                    //}
                    GroupchatMembersResultListener listener = new GroupchatMembersResultListener(account, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                        removeActiveMemberListRequest(account, groupchatJid);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                        removeActiveMemberListRequest(account, groupchatJid);
                    }
                }
            } else {
                removeActiveMemberListRequest(account, groupchatJid);
            }
        });
    }

    enum GroupchatRequestTypes{
        MemberListRequest, InviteListRequest, BlockListRequest, MeRequest
    }

    private static class GroupchatInvitesResultListener implements StanzaListener {
        private AccountJid account;
        private ContactJid groupchatJid;

        GroupchatInvitesResultListener(AccountJid account, ContactJid groupchatJid) {
            this.account = account;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) {
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
                            listener.onGroupchatInvitesReceived(account, groupchatJid);
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
        public void processStanza(Stanza packet) {
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
                            listener.onGroupchatBlocklistReceived(account, groupchatJid);
                        }
                    });

                }
                removeBlockListRequest(account, groupchatJid);
            }
        }
    }

    protected static class GroupchatMeRequest extends GroupchatRequest {
        GroupchatMeRequest(AccountJid accountJid, ContactJid groupchatJid){
            super(accountJid, groupchatJid, GroupchatRequestTypes.MeRequest);
        }
    }

    protected static class GroupchatInviteListRequest extends GroupchatRequest {
        GroupchatInviteListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, GroupchatRequestTypes.InviteListRequest);
        }
    }

    protected static class GroupchatBlockListRequest extends GroupchatRequest {
        GroupchatBlockListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, GroupchatRequestTypes.BlockListRequest);
        }
    }

    protected static class GroupchatMemberListRequest extends GroupchatRequest {
        GroupchatMemberListRequest(AccountJid accountJid, ContactJid groupchatJid) {
            super(accountJid, groupchatJid, GroupchatRequestTypes.MemberListRequest);
        }
    }

    private static class GroupchatMeResultListener implements StanzaListener {
        private AccountJid accountJid;
        private ContactJid groupchatJid;

        public GroupchatMeResultListener(AccountJid accountJid, ContactJid groupchatJid){
            this.accountJid = accountJid;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
            if (packet instanceof GroupchatMembersResultIQ) {
                GroupchatMembersResultIQ groupchatMembersIQ = (GroupchatMembersResultIQ) packet;

                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                        && accountJid.getBareJid().equals(packet.getTo().asBareJid())) {
                    if (groupchatMembersIQ.getListOfMembers().size() != 1){
                        LogManager.exception(LOG_TAG, new Exception("Strange response for groupchat me request"));
                        return;
                    }

                    for (GroupchatMemberExtensionElement memberExtension : groupchatMembersIQ.getListOfMembers()) {
                        String id = memberExtension.getId();

                        if (getInstance().members.get(id) == null)
                            getInstance().members.put(id, new GroupchatMember(id));

                        getInstance().members.get(id).setGroupchatJid(groupchatJid.toString());
                        if (memberExtension.getRole() != null)
                            getInstance().members.get(id).setRole(memberExtension.getRole());
                        if (memberExtension.getNickname() != null)
                            getInstance().members.get(id).setNickname(memberExtension.getNickname());
                        if (memberExtension.getBadge() != null)
                            getInstance().members.get(id).setBadge(memberExtension.getBadge());
                        if (memberExtension.getJid() != null)
                            getInstance().members.get(id).setJid(memberExtension.getJid());
                        if (memberExtension.getLastPresent() != null)
                            getInstance().members.get(id).setLastPresent(memberExtension.getLastPresent());
                        if (memberExtension.getAvatarInfo() != null){
                            getInstance().members.get(id).setAvatarHash(memberExtension.getAvatarInfo().getId());
                            getInstance().members.get(id).setAvatarUrl(memberExtension.getAvatarInfo().getUrl().toString());
                        }

                        getInstance().members.get(id).setMe(true);

                        if (memberExtension.getSubscriprion() != null && !memberExtension.getSubscriprion().equals("both")){
                            getInstance().removeGroupchatMember(id);
                        } else GroupchatMemberRepository.saveOrUpdateGroupchatMember(getInstance().members.get(id));

                    }

                    Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onMeReceived(accountJid, groupchatJid);
                        }
                    });
                }
                removeActiveMemberListRequest(accountJid, groupchatJid);
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
                GroupchatMembersResultIQ groupchatMembersIQ = (GroupchatMembersResultIQ) packet;

                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                        && account.getBareJid().equals(packet.getTo().asBareJid())) {

                    for (GroupchatMemberExtensionElement memberExtension : groupchatMembersIQ.getListOfMembers()) {
                        String id = memberExtension.getId();

                        if (getInstance().members.get(id) == null)
                            getInstance().members.put(id, new GroupchatMember(id));

                        getInstance().members.get(id).setGroupchatJid(groupchatJid.toString());
                        if (memberExtension.getRole() != null)
                            getInstance().members.get(id).setRole(memberExtension.getRole());
                        if (memberExtension.getNickname() != null)
                            getInstance().members.get(id).setNickname(memberExtension.getNickname());
                        if (memberExtension.getBadge() != null)
                            getInstance().members.get(id).setBadge(memberExtension.getBadge());
                        if (memberExtension.getJid() != null)
                            getInstance().members.get(id).setJid(memberExtension.getJid());
                        if (memberExtension.getLastPresent() != null)
                            getInstance().members.get(id).setLastPresent(memberExtension.getLastPresent());
                        if (memberExtension.getAvatarInfo() != null){
                            getInstance().members.get(id).setAvatarHash(memberExtension.getAvatarInfo().getId());
                            getInstance().members.get(id).setAvatarUrl(memberExtension.getAvatarInfo().getUrl().toString());
                        }

                        if (memberExtension.getSubscriprion() != null && !memberExtension.getSubscriprion().equals("both")){
                            getInstance().removeGroupchatMember(id);
                        } else GroupchatMemberRepository.saveOrUpdateGroupchatMember(getInstance().members.get(id));

                    }

                    AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
                    if (chat instanceof GroupChat) {
                        ((GroupChat) chat).setMembersListVersion(groupchatMembersIQ.getQueryVersion());
                        ChatManager.getInstance().saveOrUpdateChatDataToRealm(chat);
                    }

                    Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupchatRequestListener listener :
                                Application.getInstance().getUIListeners(OnGroupchatRequestListener.class)) {
                            listener.onGroupchatMembersReceived(account, groupchatJid);
                        }
                    });
                }
                removeActiveMemberListRequest(account, groupchatJid);
            }
        }
    }

    private static class GroupchatRequest implements Comparable<GroupchatRequest> {
        private AccountJid accountJid;
        private ContactJid groupchatJid;
        private GroupchatRequestTypes requestType;
        private int hash = 0;

        GroupchatRequest(AccountJid accountJid, ContactJid groupchatJid, GroupchatRequestTypes requestType) {
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
                result = result * 31 + requestType.toString().hashCode();
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

}
