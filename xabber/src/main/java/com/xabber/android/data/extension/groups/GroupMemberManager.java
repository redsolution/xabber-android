package com.xabber.android.data.extension.groups;

import android.content.Context;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.BaseIqResultUiListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.repositories.GroupMemberRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.xmpp.groups.block.BlockGroupMemberIQ;
import com.xabber.xmpp.groups.block.KickGroupMemberIQ;
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistItemElement;
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistQueryIQ;
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistResultIQ;
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistUnblockIQ;
import com.xabber.xmpp.groups.create.CreateGroupchatIQ;
import com.xabber.xmpp.groups.create.CreatePtpGroupIQ;
import com.xabber.xmpp.groups.members.ChangeGroupchatMemberPreferencesIQ;
import com.xabber.xmpp.groups.members.GroupchatMembersQueryIQ;
import com.xabber.xmpp.groups.members.GroupchatMembersResultIQ;
import com.xabber.xmpp.groups.rights.GroupRequestMemberRightsChangeIQ;
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsQueryIQ;
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsReplyIQ;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.ui.OnGroupMemberRightsListener;
import com.xabber.android.ui.OnGroupSelectorListToolbarActionResultListener;
import com.xabber.android.ui.OnGroupchatRequestListener;
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
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import kotlin.collections.CollectionsKt;

import static com.xabber.xmpp.avatar.UserAvatarManager.DATA_NAMESPACE;
import static com.xabber.xmpp.avatar.UserAvatarManager.METADATA_NAMESPACE;

public class GroupMemberManager implements OnLoadListener {

    private static final String LOG_TAG = GroupMemberManager.class.getSimpleName();

    private static GroupMemberManager instance;
    private final Map<String, GroupMember> members = new HashMap<>();

    public static GroupMemberManager getInstance() {
        if (instance == null) instance = new GroupMemberManager();
        return instance;
    }

    public static GroupMember getGroupMemberFromGroupMemberExtensionElement(
            GroupMemberExtensionElement groupMemberExtensionElement, BareJid groupJid) {

        GroupMember user = new GroupMember(groupMemberExtensionElement.getId());

        if (groupJid != null) user.setGroupJid(groupJid.toString());

        if (groupMemberExtensionElement.getAvatarInfo() != null) {
            user.setAvatarHash(groupMemberExtensionElement.getAvatarInfo().getId());
            user.setAvatarUrl(groupMemberExtensionElement.getAvatarInfo().getUrl().toString());
        }

        user.setLastPresent(groupMemberExtensionElement.getLastPresent());
        user.setBadge(groupMemberExtensionElement.getBadge());
        user.setJid(groupMemberExtensionElement.getJid());
        user.setNickname(groupMemberExtensionElement.getNickname());
        user.setRole(groupMemberExtensionElement.getRole());

        return user;
    }

    @Override
    public void onLoad() {
        for (GroupMember gm : GroupMemberRepository.getAllGroupMembersFromRealm()){
            this.members.put(gm.getId(), gm);
        }
    }

    public void createFakeMemberForIncomingInvite(){

    }

    public void removeMemberAvatar(GroupChat group, String memberId){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                PayloadItem<MetadataExtension> item = new PayloadItem<>(null, new MetadataExtension(null));
                PublishItem<PayloadItem<MetadataExtension>> publishItem = new PublishItem<>(METADATA_NAMESPACE + "#" + memberId, item);

                PubSub packet = PubSub.createPubsubPacket(group.getContactJid().getBareJid(),
                        IQ.Type.set, publishItem, null);

                AccountManager.getInstance().getAccount(group.getAccount()).getConnection()
                        .createStanzaCollectorAndSend(packet).nextResultOrThrow(45000);
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void publishMemberAvatar(GroupChat group, String memberId, byte[] data, int height,
                                    int width, UserAvatarManager.ImageType type){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                XMPPConnection connectionItem = AccountManager.getInstance()
                        .getAccount(group.getAccount()).getConnection();

                String avatarHash = AvatarManager.getAvatarHash(data);

                DataExtension dataExtension = new DataExtension(data);
                PayloadItem<DataExtension> dataItem = new PayloadItem<>(avatarHash, dataExtension);
                PublishItem<PayloadItem<DataExtension>> dataPublishItem = new PublishItem<>(
                        DATA_NAMESPACE + "#" + memberId, dataItem);
                PubSub dataPacket = PubSub.createPubsubPacket(group.getContactJid().getBareJid(),
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
                        group.getContactJid().getBareJid(), IQ.Type.set, metadataPublishItem,
                        null);

                connectionItem.createStanzaCollectorAndSend(metadataPacket).nextResultOrThrow(45000);

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public GroupMember getGroupMemberById(String id) {
        return members.get(id);
    }

    public GroupMember getMe(ContactJid groupJid){
        try {
            return CollectionsKt.first(getGroupMembers(groupJid), GroupMember::isMe);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public Collection<GroupMember> getGroupMembers(ContactJid groupJid){
        Collection<GroupMember> resultList = new ArrayList<>();
        for (Map.Entry<String, GroupMember> entry : members.entrySet()){
            if (entry.getValue().getGroupJid().equals(groupJid.toString())) {
                resultList.add(entry.getValue());
            }
        }
        return resultList;
    }

    public void removeGroupMember(String id){
        members.remove(id);
        GroupMemberRepository.removeGroupMemberById(id);
    }

    public void saveGroupUser(GroupMemberExtensionElement user, BareJid groupJid) {
        saveGroupUser(user, groupJid, System.currentTimeMillis());
    }

    public void saveGroupUser(GroupMemberExtensionElement user, BareJid groupJid, long timestamp) {

        GroupMember groupMember = getGroupMemberFromGroupMemberExtensionElement(user, groupJid);

        members.put(user.getId(), groupMember);
        GroupMemberRepository.saveOrUpdateGroupMember(groupMember);
    }

    public void kickMember(GroupMember groupMember, GroupChat groupChat, BaseIqResultUiListener listener){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AccountItem accountItem = AccountManager.getInstance().getAccount(groupChat.getAccount());
            try {
                if (groupMember.getJid() != null && !groupMember.getJid().isEmpty()){
                    Jid memberJid = JidCreate.bareFrom(groupMember.getJid());
                    accountItem.getConnection().sendIqWithResponseCallback(new KickGroupMemberIQ(memberJid,
                            groupChat.getFullJidIfPossible()), listener, listener);
                } else {
                    accountItem.getConnection().sendIqWithResponseCallback(new KickGroupMemberIQ(groupMember.getId(),
                            groupChat.getFullJidIfPossible()), listener, listener);
                }
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
                listener.onOtherError(e);
            }
        });
    }

    public void kickAndBlockMember(GroupMember groupMember, GroupChat groupChat, BaseIqResultUiListener listener){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AccountItem accountItem = AccountManager.getInstance().getAccount(groupChat.getAccount());
            try {
                if (groupMember.getJid() != null && !groupMember.getJid().isEmpty()){
                    Jid memberJid = JidCreate.bareFrom(groupMember.getJid());
                    accountItem.getConnection().sendIqWithResponseCallback(new BlockGroupMemberIQ(groupChat.getFullJidIfPossible(),
                            memberJid), listener, listener);
                } else {
                    accountItem.getConnection().sendIqWithResponseCallback(new BlockGroupMemberIQ(groupChat.getFullJidIfPossible(),
                            groupMember.getId()), listener, listener);
                }
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
                listener.onOtherError(e);
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
                                for (OnGroupSelectorListToolbarActionResultListener listener :
                                        Application.getInstance().getUIListeners(OnGroupSelectorListToolbarActionResultListener.class)) {
                                    if (success) {
                                        listener.onActionSuccess(account, groupchatJid, Collections.singletonList(blockedElement.getBlockedItem()));
                                    } else {
                                        listener.onActionFailure(account, groupchatJid, Collections.singletonList(blockedElement.getBlockedItem()));
                                    }
                                }
                            });
                        }
                    }, exception -> Application.getInstance().runOnUiThread(() -> {
                        for (OnGroupSelectorListToolbarActionResultListener listener :
                                Application.getInstance().getUIListeners(OnGroupSelectorListToolbarActionResultListener.class)) {
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
                                    for (OnGroupSelectorListToolbarActionResultListener listener :
                                            Application.getInstance().getUIListeners(OnGroupSelectorListToolbarActionResultListener.class)) {
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
                                for (OnGroupSelectorListToolbarActionResultListener listener :
                                        Application.getInstance().getUIListeners(OnGroupSelectorListToolbarActionResultListener.class)) {
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

    public void createChatWithIncognitoMember(GroupChat groupChat, GroupMember groupMember){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                CreatePtpGroupIQ iq = new CreatePtpGroupIQ(groupChat, groupMember.getId());
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

    public void sendSetMemberBadgeIqRequest(GroupChat groupChat, GroupMember groupMember, String badge){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{

                ChangeGroupchatMemberPreferencesIQ iq =
                        new ChangeGroupchatMemberPreferencesIQ(groupChat, groupMember.getId(), badge, null);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result)){
                                groupMember.setBadge(badge);
                                GroupMemberRepository.saveOrUpdateGroupMember(groupMember);
                                for (OnGroupchatRequestListener listener :
                                        Application.getInstance().getUIListeners(OnGroupchatRequestListener.class))
                                    listener.onGroupchatMemberUpdated(groupChat.getAccount(), groupChat.getContactJid(), groupMember.getId());
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

    public void sendSetMemberNicknameIqRequest(GroupChat groupChat, GroupMember groupMember, String nickname){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{

                ChangeGroupchatMemberPreferencesIQ iq =
                        new ChangeGroupchatMemberPreferencesIQ(groupChat, groupMember.getId(), null, nickname);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result)){
                                groupMember.setNickname(nickname);
                                GroupMemberRepository.saveOrUpdateGroupMember(groupMember);
                                for (OnGroupchatRequestListener listener : Application.getInstance().getUIListeners(OnGroupchatRequestListener.class))
                                    listener.onGroupchatMemberUpdated(groupChat.getAccount(), groupChat.getContactJid(), groupMember.getId());
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
                ArrayList<GroupMember> list = new ArrayList<>(getGroupMembers(groupchatJid));
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
                ArrayList<GroupMember> list = new ArrayList<>(getGroupMembers(groupchatJid));
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
                ArrayList<GroupMember> list = new ArrayList<>(getGroupMembers(groupchatJid));
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
                            for (OnGroupMemberRightsListener listener :
                                    Application.getInstance().getUIListeners(OnGroupMemberRightsListener.class))
                                listener.onSuccessfullyChanges(groupChat);

                        }, exception -> {
                            for (OnGroupMemberRightsListener listener :
                                    Application.getInstance().getUIListeners(OnGroupMemberRightsListener.class))
                                listener.onError(groupChat);
                        });
            } catch (Exception e) {
                for (OnGroupMemberRightsListener listener : Application.getInstance().getUIListeners(OnGroupMemberRightsListener.class))
                    listener.onError(groupChat);
            }
        });
    }

    public void requestGroupchatMemberRightsForm(AccountJid accountJid, ContactJid groupchatJid, GroupMember groupMember){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            AbstractChat chat = ChatManager.getInstance().getChat(accountJid, groupchatJid);

            if (chat instanceof GroupChat) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    GroupchatMemberRightsQueryIQ queryIQ = new GroupchatMemberRightsQueryIQ((GroupChat) chat,
                            groupMember.getId());

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
                for (OnGroupMemberRightsListener listener :
                        Application.getInstance().getUIListeners(OnGroupMemberRightsListener.class)) {
                    listener.onGroupchatMemberRightsFormReceived(((GroupChat)ChatManager.getInstance().getChat(account, groupchatJid)), (GroupchatMemberRightsReplyIQ) packet);
                }
            }
        }
    }

    private class GroupchatMeResultListener implements StanzaListener {
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

                    for (GroupMemberExtensionElement memberExtension : groupchatMembersIQ.getListOfMembers()) {
                        String id = memberExtension.getId();

                        if (getInstance().members.get(id) == null)
                            getInstance().members.put(id, new GroupMember(id));

                        members.get(id).setGroupJid(groupchatJid.toString());
                        members.get(id).setRole(memberExtension.getRole());
                        members.get(id).setNickname(memberExtension.getNickname());
                        members.get(id).setBadge(memberExtension.getBadge());
                        if (memberExtension.getJid() != null)
                            getInstance().members.get(id).setJid(memberExtension.getJid());
                        if (memberExtension.getLastPresent() != null)
                            getInstance().members.get(id).setLastPresent(memberExtension.getLastPresent());
                        if (memberExtension.getAvatarInfo() != null){
                            getInstance().members.get(id).setAvatarHash(memberExtension.getAvatarInfo().getId());
                            getInstance().members.get(id).setAvatarUrl(memberExtension.getAvatarInfo().getUrl().toString());
                        }

                        getInstance().members.get(id).setMe(true);

                        if (memberExtension.getSubscription() != null && !memberExtension.getSubscription().equals("both")){
                            getInstance().removeGroupMember(id);
                        } else GroupMemberRepository.saveOrUpdateGroupMember(getInstance().members.get(id));

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

                    for (GroupMemberExtensionElement memberExtension : groupchatMembersIQ.getListOfMembers()) {
                        String id = memberExtension.getId();

                        if (getInstance().members.get(id) == null)
                            getInstance().members.put(id, new GroupMember(id));

                        GroupMember groupMember = getInstance().members.get(id);

                        groupMember.setGroupJid(groupchatJid.toString());
                        groupMember.setRole(memberExtension.getRole());
                        groupMember.setNickname(memberExtension.getNickname());
                        groupMember.setBadge(memberExtension.getBadge());
                        if (memberExtension.getJid() != null)
                            groupMember.setJid(memberExtension.getJid());
                        if (memberExtension.getLastPresent() != null)
                            groupMember.setLastPresent(memberExtension.getLastPresent());
                        if (memberExtension.getAvatarInfo() != null){
                            groupMember.setAvatarHash(memberExtension.getAvatarInfo().getId());
                            groupMember.setAvatarUrl(memberExtension.getAvatarInfo().getUrl().toString());
                        }

                        if (memberExtension.getSubscription() != null && !memberExtension.getSubscription().equals("both")){
                            LogManager.exception(LOG_TAG, new Exception());
                        } else GroupMemberRepository.saveOrUpdateGroupMember(getInstance().members.get(id));

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
