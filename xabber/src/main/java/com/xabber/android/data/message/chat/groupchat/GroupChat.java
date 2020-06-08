package com.xabber.android.data.message.chat.groupchat;

import androidx.annotation.NonNull;

import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.chat.AbstractChat;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;

public class GroupChat extends AbstractChat {

    private GroupchatIndexType indexType;
    private GroupchatMembershipType membershipType;
    private GroupchatPrivacyType privacyType;

    //TODO may be Jid type
    private String owner;

    private String name;
    private String description;
    private MessageRealmObject pinnedMessage;
    private ArrayList<GroupchatMember> members;
    private String membersListVersion;

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

    //todo add members list into constructor and change correspondent method at repository
    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user,
                     GroupchatIndexType indexType, GroupchatMembershipType membershipType,
                     GroupchatPrivacyType privacyType, String owner, String name,
                     String description, MessageRealmObject pinnedMessage, String membersListVersion,
                     boolean canInvite, boolean canChangeSettings, boolean canChangeUsersSettings,
                     boolean canChangeNicknames, boolean canChangeBadge, boolean canBlockUsers,
                     boolean canChangeAvatars) {
        super(account, user);
        this.indexType = indexType;
        this.membershipType = membershipType;
        this.privacyType = privacyType;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.pinnedMessage = pinnedMessage;
        this.membersListVersion = membersListVersion;
        this.canInvite = canInvite;
        this.canChangeSettings = canChangeSettings;
        this.canChangeUsersSettings = canChangeUsersSettings;
        this.canChangeNicknames = canChangeNicknames;
        this.canChangeBadge = canChangeBadge;
        this.canBlockUsers = canBlockUsers;
        this.canChangeAvatars = canChangeAvatars;
    }

    @NonNull
    @Override
    public Jid getTo() { return user.getBareJid(); }

    @Override
    public Message.Type getType() {
        return Message.Type.groupchat;
    }

    @Override
    public MessageRealmObject createNewMessageItem(String text) {
        String id = UUID.randomUUID().toString();
        return createMessageItem(null, text, null, null, null, false,
                false, false, false, id, id, null, null, null,
                account.getFullJid().toString(), false, null);
    }

    @Override
    public RealmList<ForwardIdRealmObject> parseForwardedMessage(boolean ui, Stanza packet,
                                                                 String parentMessageId) {
        return super.parseForwardedMessage(ui, packet, parentMessageId);
    }

    @Override
    public String parseInnerMessage(boolean ui, Message message, Date timestamp,
                                    String parentMessageId) {
        //todo maybe like regularchat with changes
        return null;
    }

    public GroupchatIndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(GroupchatIndexType indexType) {
        this.indexType = indexType;
    }

    public GroupchatMembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(GroupchatMembershipType membershipType) {
        this.membershipType = membershipType;
    }

    public GroupchatPrivacyType getPrivacyType() {
        return privacyType;
    }

    public void setPrivacyType(GroupchatPrivacyType privacyType) {
        this.privacyType = privacyType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MessageRealmObject getPinnedMessage() {
        return pinnedMessage;
    }

    public void setPinnedMessage(MessageRealmObject pinnedMessage) {
        this.pinnedMessage = pinnedMessage;
    }

    public ArrayList<GroupchatMember> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<GroupchatMember> members) {
        this.members = members;
    }

    public String getMembersListVersion() {
        return membersListVersion;
    }

    public void setMembersListVersion(String membersListVersion) {
        this.membersListVersion = membersListVersion;
    }

    public boolean isCanInvite() {
        return canInvite;
    }

    public void setCanInvite(boolean canInvite) {
        this.canInvite = canInvite;
    }

    public boolean isCanChangeSettings() {
        return canChangeSettings;
    }

    public void setCanChangeSettings(boolean canChangeSettings) {
        this.canChangeSettings = canChangeSettings;
    }

    public boolean isCanChangeUsersSettings() {
        return canChangeUsersSettings;
    }

    public void setCanChangeUsersSettings(boolean canChangeUsersSettings) {
        this.canChangeUsersSettings = canChangeUsersSettings;
    }

    public boolean isCanChangeNicknames() {
        return canChangeNicknames;
    }

    public void setCanChangeNicknames(boolean canChangeNicknames) {
        this.canChangeNicknames = canChangeNicknames;
    }

    public boolean isCanChangeBadge() {
        return canChangeBadge;
    }

    public void setCanChangeBadge(boolean canChangeBadge) {
        this.canChangeBadge = canChangeBadge;
    }

    public boolean isCanBlockUsers() {
        return canBlockUsers;
    }

    public void setCanBlockUsers(boolean canBlockUsers) {
        this.canBlockUsers = canBlockUsers;
    }

    public boolean isCanChangeAvatars() {
        return canChangeAvatars;
    }

    public void setCanChangeAvatars(boolean canChangeAvatars) {
        this.canChangeAvatars = canChangeAvatars;
    }

}
