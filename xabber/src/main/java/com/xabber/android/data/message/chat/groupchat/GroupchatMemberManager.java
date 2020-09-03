package com.xabber.android.data.message.chat.groupchat;

import android.os.Looper;

import androidx.annotation.IntDef;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatMemberRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
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
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.BareJid;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupchatMemberManager implements OnLoadListener {

    private static final String LOG_TAG = GroupchatMemberManager.class.getSimpleName();
    static final int MemberListRequest = 1;
    static final int InviteListRequest = 2;
    static final int BlockListRequest = 3;
    private static GroupchatMemberManager instance;
    private static Set<GroupchatRequest> groupchatRequests = new ConcurrentSkipListSet<>();
    private final Map<String, GroupchatMember> users = new HashMap<>();

    public static GroupchatMemberManager getInstance() {
        if (instance == null) instance = new GroupchatMemberManager();
        return instance;
    }

    public static GroupchatMemberRealmObject refUserToRealm(GroupchatUserExtension user, BareJid groupchatJid) {
        GroupchatMemberRealmObject realmUser = new GroupchatMemberRealmObject(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        realmUser.setLastPresent(user.getLastPresent());
        if (groupchatJid != null) realmUser.setGroupchatJid(groupchatJid.toString());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatarInfo() != null) {
            realmUser.setAvatarHash(user.getAvatarInfo().getId());
            realmUser.setAvatarUrl(user.getAvatarInfo().getUrl().toString());
        }
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    public static GroupchatMemberRealmObject userToRealmUser(GroupchatMember user) {
        GroupchatMemberRealmObject realmUser = new GroupchatMemberRealmObject(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        realmUser.setLastPresent(user.getLastPresent());
        if (user.getGroupchatJid() != null) realmUser.setGroupchatJid(user.getGroupchatJid());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatarHash() != null) realmUser.setAvatarHash(user.getAvatarHash());
        if (user.getAvatarUrl() != null) realmUser.setAvatarUrl(user.getAvatarUrl());
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    public static GroupchatMember refUserToUser(GroupchatUserExtension groupchatUserExtension, BareJid groupchatJid) {
        GroupchatMember user = new GroupchatMember(groupchatUserExtension.getId());
        if (groupchatJid != null) user.setGroupchatJid(groupchatJid.toString());
        if (groupchatUserExtension.getAvatarInfo() != null) {
            user.setAvatarHash(groupchatUserExtension.getAvatarInfo().getId());
            user.setAvatarUrl(groupchatUserExtension.getAvatarInfo().getUrl().toString());
        }
        user.setLastPresent(groupchatUserExtension.getLastPresent());
        user.setBadge(groupchatUserExtension.getBadge());
        user.setJid(groupchatUserExtension.getJid());
        user.setNickname(groupchatUserExtension.getNickname());
        user.setRole(groupchatUserExtension.getRole());
        return user;
    }

    public static GroupchatMember realmUserToUser(GroupchatMemberRealmObject groupchatUser) {
        GroupchatMember user = new GroupchatMember(groupchatUser.getUniqueId());
        user.setAvatarHash(groupchatUser.getAvatarHash());
        user.setAvatarUrl(groupchatUser.getAvatarUrl());
        user.setLastPresent(groupchatUser.getLastPresent());
        user.setBadge(groupchatUser.getBadge());
        user.setJid(groupchatUser.getJid());
        user.setNickname(groupchatUser.getNickname());
        user.setRole(groupchatUser.getRole());
        user.setTimestamp(groupchatUser.getTimestamp());
        return user;
    }

    private static GroupchatRequest createRequest(AccountJid account, ContactJid groupchatJid,
                                                  @GroupchatRequestTypes int requestType) {
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
            default:
                throw new RuntimeException("Wrong groupchat request type = " + requestType);
        }
        return request;
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

    @Override
    public void onLoad() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<GroupchatMemberRealmObject> users = realm
                .where(GroupchatMemberRealmObject.class)
                .findAll();
        for (GroupchatMemberRealmObject user : users) {
            this.users.put(user.getUniqueId(), realmUserToUser(user));
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    public GroupchatMember getGroupchatUser(String id) {
        return users.get(id);
    }

    public void saveGroupchatUser(GroupchatUserExtension user, BareJid groupchatJid) {
        saveGroupchatUser(user, groupchatJid, System.currentTimeMillis());
    }

    public void saveGroupchatUser(GroupchatUserExtension user, BareJid groupchatJid, long timestamp) {
        if (!users.containsKey(user.getId())) {
            saveUser(user, groupchatJid, timestamp);
        } else if (timestamp > users.get(user.getId()).getTimestamp()) {
            saveUser(user, groupchatJid, timestamp);
        }
    }

    private void saveUser(GroupchatUserExtension user, BareJid groupchatJid, long timestamp) {
        users.put(user.getId(), refUserToUser(user, groupchatJid));
        saveGroupchatUserToRealm(refUserToRealm(user, groupchatJid), timestamp);
    }

    private void saveGroupchatUserToRealm(final GroupchatMemberRealmObject user, final long timestamp) {
        Application.getInstance().runInBackground(() -> {
            user.setTimestamp(timestamp);
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(user));
            } catch (Exception e) {
                LogManager.exception("GroupchatUserManager", e);
            } finally {
                if (realm != null) realm.close();
            }
        });
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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MemberListRequest, InviteListRequest, BlockListRequest})
    protected @interface GroupchatRequestTypes {
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
                            listener.onGroupchatBlocklistReceived(account, groupchatJid, blockList);
                        }
                    });

                }
                removeBlockListRequest(account, groupchatJid);
            }
        }
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

                    for (GroupchatUserExtension userExtension : groupchatMembers.getListOfMembers()) {
                        listOfMembers.add(GroupchatMemberManager.refUserToUser(userExtension, groupchatJid.getBareJid()));
                    }

                    AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
                    if (chat instanceof GroupChat) {
                        ((GroupChat) chat).setMembers(listOfMembers);
                        ((GroupChat) chat).setMembersListVersion(groupchatMembers.getQueryVersion());
                        //chat.requestSaveToRealm();
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

}
