package com.xabber.android.data.message.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groups.GroupIndexType;
import com.xabber.android.data.extension.groups.GroupInviteManager;
import com.xabber.android.data.extension.groups.GroupMembershipType;
import com.xabber.android.data.extension.groups.GroupPrivacyType;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistItemElement;

import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.Date;

import io.realm.RealmResults;

public class GroupChat extends AbstractChat {

    private static final String LOG_TAG = GroupChat.class.getSimpleName();

    private GroupIndexType indexType;
    private GroupMembershipType membershipType;
    private GroupPrivacyType privacyType = GroupPrivacyType.NONE;

    private String owner;

    private String name;
    private String description;
    private String pinnedMessageId;
    private ArrayList<String> listOfInvites;
    private ArrayList<GroupchatBlocklistItemElement> listOfBlockedElements;
    private String membersListVersion;
    private int numberOfMembers;
    private int numberOfOnlineMembers;

    private String resource;

    //Permissions and restrictions
    private boolean canInvite;
    private boolean canChangeSettings;
    private boolean canChangeUsersSettings;
    private boolean canChangeNicknames;
    private boolean canChangeBadge;
    private boolean canBlockUsers;
    private boolean canChangeAvatars;

    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user) {
        super(account, user);
    }

    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user, GroupIndexType indexType,
                     GroupMembershipType membershipType, GroupPrivacyType privacyType, String owner,
                     String name, String description, int numberOfMembers, String pinnedMessageId,
                     String membersListVersion, boolean canInvite, boolean canChangeSettings,
                     boolean canChangeUsersSettings, boolean canChangeNicknames, boolean canChangeBadge,
                     boolean canBlockUsers, boolean canChangeAvatars, String resource,
                     NotificationState notificationState) {
        super(account, user);
        this.indexType = indexType;
        this.membershipType = membershipType;
        this.privacyType = privacyType;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.numberOfMembers = numberOfMembers;
        this.pinnedMessageId = pinnedMessageId;
        this.membersListVersion = membersListVersion;
        this.canInvite = canInvite;
        this.canChangeSettings = canChangeSettings;
        this.canChangeUsersSettings = canChangeUsersSettings;
        this.canChangeNicknames = canChangeNicknames;
        this.canChangeBadge = canChangeBadge;
        this.canBlockUsers = canBlockUsers;
        this.canChangeAvatars = canChangeAvatars;
        this.resource = resource;
        this.setNotificationState(notificationState, false);
    }

    @Override
    public RealmResults<MessageRealmObject> getMessages() {
        if (messages == null) {
            messages = MessageRepository.getGroupChatMessages(account, contactJid);
            updateLastMessage();
            messages.addChangeListener(this);
        }

        return messages;
    }

    @NonNull
    @Override
    public Jid getTo() {
        return contactJid.getBareJid();
    }

    @Override
    public Message.Type getType() {
        return Message.Type.chat;
    }

    @Nullable
    public FullJid getFullJidIfPossible(){
        try{
            if (resource != null && !resource.isEmpty()) {
                return JidCreate.fullFrom(contactJid.getBareJid().toString() + "/" + resource);
            } else return JidCreate.fullFrom(contactJid.getBareJid().toString() + "/Group");
        } catch (Exception e){
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }

    /* Getters and setters */
    public GroupIndexType getIndexType() { return indexType; }
    public void setIndexType(GroupIndexType indexType) { this.indexType = indexType; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public GroupMembershipType getMembershipType() { return membershipType; }
    public void setMembershipType(GroupMembershipType membershipType) { this.membershipType = membershipType; }

    public GroupPrivacyType getPrivacyType() { return privacyType; }
    public void setPrivacyType(GroupPrivacyType privacyType) { this.privacyType = privacyType; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPinnedMessageId() { return pinnedMessageId; }
    public void setPinnedMessageId(String pinnedMessageId) {
        this.pinnedMessageId = pinnedMessageId;
    }

    public String getMembersListVersion() { return membersListVersion; }
    public void setMembersListVersion(String membersListVersion) {
        this.membersListVersion = membersListVersion;
    }

    public boolean isCanInvite() { return canInvite; }
    public void setCanInvite(boolean canInvite) { this.canInvite = canInvite; }

    public boolean isCanChangeSettings() { return canChangeSettings; }
    public void setCanChangeSettings(boolean canChangeSettings) {
        this.canChangeSettings = canChangeSettings;
    }

    public boolean isCanChangeUsersSettings() { return canChangeUsersSettings; }
    public void setCanChangeUsersSettings(boolean canChangeUsersSettings) {
        this.canChangeUsersSettings = canChangeUsersSettings;
    }

    public boolean isCanChangeNicknames() { return canChangeNicknames; }
    public void setCanChangeNicknames(boolean canChangeNicknames) {
        this.canChangeNicknames = canChangeNicknames;
    }

    public boolean isCanChangeBadge() { return canChangeBadge; }
    public void setCanChangeBadge(boolean canChangeBadge) { this.canChangeBadge = canChangeBadge; }

    public boolean isCanBlockUsers() { return canBlockUsers; }
    public void setCanBlockUsers(boolean canBlockUsers) { this.canBlockUsers = canBlockUsers; }

    public boolean isCanChangeAvatars() { return canChangeAvatars; }
    public void setCanChangeAvatars(boolean canChangeAvatars) {
        this.canChangeAvatars = canChangeAvatars;
    }

    public int getNumberOfMembers() { return numberOfMembers; }
    public void setNumberOfMembers(int numberOfMembers) { this.numberOfMembers = numberOfMembers; }

    public int getNumberOfOnlineMembers() { return numberOfOnlineMembers; }
    public void setNumberOfOnlineMembers(int numberOfOnlineMembers) {
        this.numberOfOnlineMembers = numberOfOnlineMembers;
    }

    public ArrayList<String> getListOfInvites() { return listOfInvites; }
    public void setListOfInvites(ArrayList<String> listOfInvites) {
        this.listOfInvites = listOfInvites;
    }

    public ArrayList<GroupchatBlocklistItemElement> getListOfBlockedElements() {
        return listOfBlockedElements;
    }
    public void setListOfBlockedElements(ArrayList<GroupchatBlocklistItemElement> listOfBlockedElements) {
        this.listOfBlockedElements = listOfBlockedElements;
    }

    @Override
    public Date getLastTime() {
        MessageRealmObject lastMessage = getLastMessage();
        if (lastMessage != null && lastMessage.isValid()) {
            return new Date(lastMessage.getTimestamp());
        } else {
            if (lastActionTimestamp != null) return new Date(getLastActionTimestamp());

            if (GroupInviteManager.INSTANCE.hasActiveIncomingInvites(account, contactJid)){
                return new Date(GroupInviteManager.INSTANCE.getLastInvite(account, contactJid).getDate());
            }
            return null;
        }
    }

    @Override
    public int getUnreadMessageCount() {
        if (GroupInviteManager.INSTANCE.hasActiveIncomingInvites(account, contactJid)){
            return 1;
        } else return super.getUnreadMessageCount();
    }

}
