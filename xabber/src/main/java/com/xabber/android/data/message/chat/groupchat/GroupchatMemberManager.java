package com.xabber.android.data.message.chat.groupchat;

import android.content.Context;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.repositories.GroupchatMemberRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.groupchat.GroupchatMemberExtensionElement;
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistItemElement;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistQueryIQ;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistResultIQ;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistUnblockIQ;
import com.xabber.android.data.extension.groupchat.create.CreateGroupchatIQ;
import com.xabber.android.data.extension.groupchat.create.CreatePtpGroupIQ;
import com.xabber.android.data.extension.groupchat.invite.outgoing.GroupchatInviteListQueryIQ;
import com.xabber.android.data.extension.groupchat.invite.outgoing.GroupchatInviteListResultIQ;
import com.xabber.android.data.extension.groupchat.invite.outgoing.GroupchatInviteListRevokeIQ;
import com.xabber.android.data.extension.groupchat.invite.outgoing.GroupchatInviteRequestIQ;
import com.xabber.android.data.extension.groupchat.invite.outgoing.OnGroupchatSelectorListToolbarActionResult;
import com.xabber.android.data.extension.groupchat.members.ChangeGroupchatMemberPreferencesIQ;
import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ;
import com.xabber.android.data.extension.groupchat.members.GroupchatMembersResultIQ;
import com.xabber.android.data.extension.groupchat.rights.GroupMemberRightsListener;
import com.xabber.android.data.extension.groupchat.rights.GroupRequestMemberRightsChangeIQ;
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsQueryIQ;
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.xmpp.avatar.DataExtension;
import com.xabber.xmpp.avatar.MetadataExtension;
import com.xabber.xmpp.avatar.MetadataInfo;
import com.xabber.xmpp.avatar.UserAvatarManager;

import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.BareJid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.xabber.xmpp.avatar.UserAvatarManager.DATA_NAMESPACE;
import static com.xabber.xmpp.avatar.UserAvatarManager.METADATA_NAMESPACE;

public class GroupchatMemberManager implements OnLoadListener, OnPacketListener {

    private static final String LOG_TAG = GroupchatMemberManager.class.getSimpleName();

    private static GroupchatMemberManager instance;
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

    @Override
    public void onLoad() {
        for (GroupchatMember gm : GroupchatMemberRepository.getAllGroupchatMembersFromRealm()){
            this.members.put(gm.getId(), gm);
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
//        if (packet instanceof Message
//                && packet.hasExtension(GroupchatExtensionElement.ELEMENT,
//                GroupchatExtensionElement.SYSTEM_MESSAGE_NAMESPACE)){
//            //GroupchatExtensionElement groupchatExtensionElement = packet
//                    //.getExtension(GroupchatExtensionElement.ELEMENT, GroupchatExtensionElement.SYSTEM_MESSAGE_NAMESPACE);
//        }
    }

    public void removeMemberAvatar(GroupChat groupChat, String memberId){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                PayloadItem<MetadataExtension> item = new PayloadItem<>(null, new MetadataExtension(null));
                PublishItem<PayloadItem<MetadataExtension>> publishItem = new PublishItem<>(METADATA_NAMESPACE + "#" + memberId, item);

                PubSub packet = PubSub.createPubsubPacket(groupChat.getContactJid().getBareJid(),
                        IQ.Type.set, publishItem, null);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .createStanzaCollectorAndSend(packet).nextResultOrThrow(45000);
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void publishMemberAvatar(GroupChat groupChat, String memberId, byte[] data, int height,
                                    int width, UserAvatarManager.ImageType type){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                XMPPConnection connectionItem = AccountManager.getInstance()
                        .getAccount(groupChat.getAccount()).getConnection();

                String avatarHash = AvatarManager.getAvatarHash(data);

                DataExtension dataExtension = new DataExtension(data);
                PayloadItem<DataExtension> dataItem = new PayloadItem<>(avatarHash, dataExtension);
                PublishItem<PayloadItem<DataExtension>> dataPublishItem = new PublishItem<>(
                        DATA_NAMESPACE + "#" + memberId, dataItem);
                PubSub dataPacket = PubSub.createPubsubPacket(groupChat.getContactJid().getBareJid(),
                        IQ.Type.set, dataPublishItem, null);

                connectionItem.createStanzaCollectorAndSend(dataPacket).nextResultOrThrow(60000);

                MetadataInfo metadataInfo = new MetadataInfo(avatarHash,
                        null, data.length, type.getValue(), height, width);
                MetadataExtension metadataExtension = new MetadataExtension(
                        Collections.singletonList(metadataInfo), null);
                PayloadItem<MetadataExtension> metadataItem = new PayloadItem<>(avatarHash,
                        metadataExtension);
                PublishItem<PayloadItem<MetadataExtension>> metadataPublishItem = new PublishItem<>(
                        METADATA_NAMESPACE + "#" + memberId, metadataItem);
                PubSub metadataPacket = PubSub.createPubsubPacket(
                        groupChat.getContactJid().getBareJid(), IQ.Type.set, metadataPublishItem,
                        null);

                connectionItem.createStanzaCollectorAndSend(metadataPacket).nextResultOrThrow(45000);

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
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
                        GroupchatInviteRequestIQ requestIQ = new GroupchatInviteRequestIQ((GroupChat) chat, invite);
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

    public void requestGroupchatInvitationsList(AccountJid account, ContactJid groupchatJid, StanzaListener listener,
                                                ExceptionCallback exceptionCallback) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();
                    GroupchatInviteListQueryIQ queryIQ = new GroupchatInviteListQueryIQ((GroupChat) chat);
                    try {
                        connection.sendIqWithResponseCallback(queryIQ, packet -> {
                            if (packet instanceof GroupchatInviteListResultIQ) {
                                GroupchatInviteListResultIQ resultIQ = (GroupchatInviteListResultIQ) packet;

                                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                                        && account.getBareJid().equals(packet.getTo().asBareJid())) {

                                    ArrayList<String> listOfInvites = resultIQ.getListOfInvitedJids();

                                    if (listOfInvites != null) {
                                        ((GroupChat) chat).setListOfInvites(listOfInvites);
                                    } else {
                                        ((GroupChat) chat).setListOfInvites(new ArrayList<>());
                                    }
                                }
                            }
                            listener.processStanza(packet);
                        }, exceptionCallback::processException);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
            }
        });
    }

    public void requestGroupchatBlocklistList(AccountJid account, ContactJid groupchatJid, StanzaListener listener,
                                              ExceptionCallback exceptionCallback) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatJid);
            if (chat instanceof GroupChat) {

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    XMPPConnection connection = accountItem.getConnection();
                    GroupchatBlocklistQueryIQ queryIQ = new GroupchatBlocklistQueryIQ((GroupChat) chat);
                    try {
                        connection.sendIqWithResponseCallback(queryIQ, packet -> {
                            if (packet instanceof GroupchatBlocklistResultIQ) {
                                GroupchatBlocklistResultIQ resultIQ = (GroupchatBlocklistResultIQ) packet;
                                if (groupchatJid.getBareJid().equals(packet.getFrom().asBareJid())
                                        && account.getBareJid().equals(packet.getTo().asBareJid())) {
                                    ArrayList<GroupchatBlocklistItemElement> blockList = resultIQ.getBlockedItems();
                                    if (blockList != null) {
                                        ((GroupChat) chat).setListOfBlockedElements(blockList);
                                    } else {
                                        ((GroupChat) chat).setListOfBlockedElements(new ArrayList<>());
                                    }
                                }
                            }
                            listener.processStanza(packet);
                        }, exceptionCallback);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
            }
        });
    }

    public void revokeGroupchatInvitation(AccountJid account, ContactJid groupchatJid, String inviteJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                GroupChat groupChat = (GroupChat) ChatManager.getInstance().getChat(account, groupchatJid);
                GroupchatInviteListRevokeIQ revokeIQ = new GroupchatInviteListRevokeIQ(groupChat, inviteJid);

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
                            new GroupchatInviteListRevokeIQ(groupChat, inviteJid);
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
                GroupChat groupChat = (GroupChat) ChatManager.getInstance().getChat(account, groupchatJid);
                GroupchatBlocklistUnblockIQ revokeIQ =
                        new GroupchatBlocklistUnblockIQ(groupChat, blockedElement);

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
                    GroupchatBlocklistUnblockIQ revokeIQ = new GroupchatBlocklistUnblockIQ(groupChat, blockedElement);
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

    public void createChatWithIncognitoMember(GroupChat groupChat, GroupchatMember groupchatMember){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                CreatePtpGroupIQ iq = new CreatePtpGroupIQ(groupChat, groupchatMember.getId());
                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ
                                    && ((IQ) packet).getType().equals(IQ.Type.result)
                                    && packet instanceof CreateGroupchatIQ.ResultIq){
                                try{
                                    ContactJid contactJid = ContactJid.from(((CreateGroupchatIQ.ResultIq) packet).getJid());
                                    AccountJid account = AccountJid.from(packet.getTo().toString());
                                    PresenceManager.getInstance().addAutoAcceptSubscription(account, contactJid);
                                    PresenceManager.getInstance().acceptSubscription(account, contactJid, true);
                                    PresenceManager.getInstance().requestSubscription(account, contactJid);
                                    Context context = Application.getInstance().getApplicationContext();
                                    context.startActivity(ChatActivity.createSendIntent(context, groupChat.getAccount(), contactJid, null));
                                } catch (Exception e){
                                    LogManager.exception(LOG_TAG, e);
                                }
                            }
                        }, exception -> {
                            LogManager.e(LOG_TAG, "Exception while creating direct chat \n");
                            exception.printStackTrace();
                        });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendSetMemberBadgeIqRequest(GroupChat groupChat, GroupchatMember groupchatMember, String badge){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{

                ChangeGroupchatMemberPreferencesIQ iq =
                        new ChangeGroupchatMemberPreferencesIQ(groupChat, groupchatMember.getId(), badge, null);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result)){
                                groupchatMember.setBadge(badge);
                                GroupchatMemberRepository.saveOrUpdateGroupchatMember(groupchatMember);
                                for (OnGroupchatRequestListener listener : Application.getInstance().getUIListeners(OnGroupchatRequestListener.class))
                                    listener.onGroupchatMemberUpdated(groupChat.getAccount(), groupChat.getContactJid(), groupchatMember.getId());
                            }
                        }, exception -> {
                            LogManager.exception(LOG_TAG, exception);
                            if (exception instanceof XMPPException.XMPPErrorException &&
                                    ((XMPPException.XMPPErrorException)exception).getXMPPError()
                                            .getCondition().equals(XMPPError.Condition.not_allowed))

                                Application.getInstance().runOnUiThread(() -> Toast.makeText(Application.getInstance().getApplicationContext(),
                                        Application.getInstance().getApplicationContext().getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                        Toast.LENGTH_SHORT).show());

                        });
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
        });
    }

    public void sendSetMemberNicknameIqRequest(GroupChat groupChat, GroupchatMember groupchatMember, String nickname){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{

                ChangeGroupchatMemberPreferencesIQ iq =
                        new ChangeGroupchatMemberPreferencesIQ(groupChat, groupchatMember.getId(), null, nickname);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result)){
                                groupchatMember.setNickname(nickname);
                                GroupchatMemberRepository.saveOrUpdateGroupchatMember(groupchatMember);
                                for (OnGroupchatRequestListener listener : Application.getInstance().getUIListeners(OnGroupchatRequestListener.class))
                                    listener.onGroupchatMemberUpdated(groupChat.getAccount(), groupChat.getContactJid(), groupchatMember.getId());
                            }
                        }, exception -> {
                            LogManager.exception(LOG_TAG, exception);
                            if (exception instanceof XMPPException.XMPPErrorException &&
                                    ((XMPPException.XMPPErrorException)exception).getXMPPError()
                                            .getCondition().equals(XMPPError.Condition.not_allowed))

                                Application.getInstance().runOnUiThread(() -> Toast.makeText(Application.getInstance().getApplicationContext(),
                                        Application.getInstance().getApplicationContext().getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                        Toast.LENGTH_SHORT).show());
                        });
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
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

                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ((GroupChat) chat);
                    queryIQ.setQueryId("");

                    GroupchatMeResultListener listener = new GroupchatMeResultListener(accountJid, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
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

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem != null) {
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ((GroupChat) chat);
                    String version = ((GroupChat) chat).getMembersListVersion();
                    if (version != null && !version.isEmpty()) {
                        queryIQ.setQueryVersion(version);
                    } else queryIQ.setQueryVersion("1");

                    GroupchatMembersResultListener listener = new GroupchatMembersResultListener(account, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
            }
        });
    }

    public void requestGroupchatMemberInfo(GroupChat groupChat, String memberId){
        final AccountJid accountJid = groupChat.getAccount();
        final ContactJid groupchatJid = groupChat.getContactJid();
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

                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    GroupchatMembersQueryIQ queryIQ = new GroupchatMembersQueryIQ((GroupChat) groupChat);
                    queryIQ.setQueryId(memberId);

                    GroupchatMembersResultListener listener = new GroupchatMembersResultListener(accountJid, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
            }
        });
    }

    public void requestGroupchatMemberRightsChange(GroupChat groupChat, DataForm dataForm){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new GroupRequestMemberRightsChangeIQ(
                                groupChat, dataForm), packet -> {
                            for (GroupMemberRightsListener listener : Application.getInstance().getUIListeners(GroupMemberRightsListener.class))
                                listener.onSuccessfullyChanges(groupChat);

                        }, exception -> {
                            for (GroupMemberRightsListener listener : Application.getInstance().getUIListeners(GroupMemberRightsListener.class))
                                listener.onError(groupChat);
                        });
            } catch (Exception e) {
                for (GroupMemberRightsListener listener : Application.getInstance().getUIListeners(GroupMemberRightsListener.class))
                    listener.onError(groupChat);
            }
        });
    }

    public void requestGroupchatMemberRightsForm(AccountJid accountJid, ContactJid groupchatJid, GroupchatMember groupchatMember){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            AbstractChat chat = ChatManager.getInstance().getChat(accountJid, groupchatJid);

            if (chat instanceof GroupChat) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    GroupchatMemberRightsQueryIQ queryIQ = new GroupchatMemberRightsQueryIQ((GroupChat) chat,
                            groupchatMember.getId());

                    GroupchatMemberRightsFormResultListener listener = new GroupchatMemberRightsFormResultListener(accountJid, groupchatJid);
                    try {
                        accountItem.getConnection().sendIqWithResponseCallback(queryIQ, listener);
                    } catch (SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }
            }
        });
    }

    private static class GroupchatMemberRightsFormResultListener implements StanzaListener {
        private final AccountJid account;
        private final ContactJid groupchatJid;

        GroupchatMemberRightsFormResultListener(AccountJid accountJid, ContactJid groupchatJid){
            this.account = accountJid;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) {
            if (packet instanceof GroupchatMemberRightsReplyIQ){
                for (GroupMemberRightsListener listener :
                        Application.getInstance().getUIListeners(GroupMemberRightsListener.class)) {
                    listener.onGroupchatMemberRightsFormReceived(((GroupChat)ChatManager.getInstance().getChat(account, groupchatJid)), (GroupchatMemberRightsReplyIQ) packet);
                }
            }
        }
    }

    private static class GroupchatMeResultListener implements StanzaListener {
        private final AccountJid accountJid;
        private final ContactJid groupchatJid;

        public GroupchatMeResultListener(AccountJid accountJid, ContactJid groupchatJid){
            this.accountJid = accountJid;
            this.groupchatJid = groupchatJid;
        }

        @Override
        public void processStanza(Stanza packet) {
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
            }
        }
    }

    private static class GroupchatMembersResultListener implements StanzaListener {

        private final AccountJid account;
        private final ContactJid groupchatJid;

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

                        GroupchatMember groupchatMember = getInstance().members.get(id);

                        groupchatMember.setGroupchatJid(groupchatJid.toString());
                        if (memberExtension.getRole() != null)
                            groupchatMember.setRole(memberExtension.getRole());
                        if (memberExtension.getNickname() != null)
                            groupchatMember.setNickname(memberExtension.getNickname());
                        if (memberExtension.getBadge() != null)
                            groupchatMember.setBadge(memberExtension.getBadge());
                        if (memberExtension.getJid() != null)
                            groupchatMember.setJid(memberExtension.getJid());
                        if (memberExtension.getLastPresent() != null)
                            groupchatMember.setLastPresent(memberExtension.getLastPresent());
                        if (memberExtension.getAvatarInfo() != null){
                            groupchatMember.setAvatarHash(memberExtension.getAvatarInfo().getId());
                            groupchatMember.setAvatarUrl(memberExtension.getAvatarInfo().getUrl().toString());
                        }

                        if (memberExtension.getSubscriprion() != null && !memberExtension.getSubscriprion().equals("both")){
                            LogManager.exception(LOG_TAG, new Exception());
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
            }
        }
    }

}
