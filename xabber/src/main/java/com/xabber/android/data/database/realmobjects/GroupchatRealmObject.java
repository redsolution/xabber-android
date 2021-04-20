package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groups.GroupIndexType;
import com.xabber.android.data.extension.groups.GroupMembershipType;
import com.xabber.android.data.extension.groups.GroupPrivacyType;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.NotificationState;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class GroupchatRealmObject extends RealmObject {

    private static final String LOG_TAG = GroupchatRealmObject.class.getSimpleName();

    public static final class Fields {
        public static final String PRIMARY_KEY = "primaryKey";
        public static final String GROUPCHAT_JID = "groupchatJid";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String OWNER = "owner";
        public static final String NOTIFICATION_MODE = "notificationMode";
        public static final String NOTIFICATION_TIMESTAMP = "notificationTimestamp";
    }

    @PrimaryKey
    private String primary;
    private String groupchatJid;
    private String accountJid;
    private String owner;
    private String name;
    private String privacy = GroupPrivacyType.NONE.toString();
    private String index = GroupIndexType.NONE.toString();
    private String membership = GroupMembershipType.NONE.toString();
    private String description;
    private String pinnedMessageId;
    private String membersListVersion;
    private boolean canInvite;
    private boolean canChangeSettings;
    private boolean canChangeUsersSettings;
    private boolean canChangeNicknames;
    private boolean canChangeBadge;
    private boolean canBlockUsers;
    private boolean canChangeAvatars;
    private int membersCount;
    private long present;
    private boolean collect;
    private boolean peerToPeer;
    private RealmList<String> domains;
    private RealmList<String> invited;
    private String status;
    private String resource;
    private String notificationMode;
    private long notificationTimestamp;

    public GroupchatRealmObject() {
        this.primary = UUID.randomUUID().toString();
    }

    public GroupchatRealmObject(AccountJid accountJid, ContactJid contactJid) {
        this.primary = UUID.randomUUID().toString();
        this.accountJid = accountJid.toString();
        this.groupchatJid = contactJid.getBareJid().toString();
    }

    public String getPrimary() {
        return primary;
    }
    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public ContactJid getGroupchatJid() {
        try {
            return ContactJid.from(groupchatJid);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }
    public void setGroupchatJid(ContactJid contactJid) {
        this.groupchatJid = contactJid.toString();
    }

    public AccountJid getAccountJid() {
        try {
            return AccountJid.from(accountJid);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }
    public void setAccountJid(AccountJid accountJid) {
        this.accountJid = accountJid.toString();
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

    public GroupPrivacyType getPrivacy() { return GroupPrivacyType.fromString(privacy); }
    public void setPrivacy(GroupPrivacyType privacy) {
        if (privacy != null) this.privacy = privacy.toString();
    }

    public GroupIndexType getIndex() { return GroupIndexType.fromString(index); }
    public void setIndex(GroupIndexType index) { if (index != null) this.index = index.toString(); }

    public GroupMembershipType getMembership() { return  GroupMembershipType.fromString(membership); }
    public void setMembership(GroupMembershipType membership) {
        if (membership != null) this.membership = membership.toString();
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getPinnedMessageId() {
        return pinnedMessageId;
    }
    public void setPinnedMessageId(String pinnedMessageId) {
        this.pinnedMessageId = pinnedMessageId;
    }

    public int getMembersCount() {
        return membersCount;
    }
    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }

    public RealmList<String> getDomains() {
        return domains;
    }
    public void setDomains(RealmList<String> domains) {
        this.domains = domains;
    }

    public long getPresent() {
        return present;
    }
    public void setPresent(long present) {
        this.present = present;
    }

    public boolean isCollect() {
        return collect;
    }
    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public boolean isPeerToPeer() {
        return peerToPeer;
    }
    public void setPeerToPeer(boolean peerToPeer) {
        this.peerToPeer = peerToPeer;
    }

    public String getMembersListVersion() {
        return membersListVersion;
    }
    public void setMembersListVersion(String membersListVersion) {
        this.membersListVersion = membersListVersion;
    }

    public RealmList<String> getInvited() {
        return invited;
    }
    public void setInvited(RealmList<String> invited) {
        this.invited = invited;
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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Resourcepart getResource() throws XmppStringprepException { return Resourcepart.from(resource); }
    public void setResource(Resourcepart resource) { this.resource = resource.toString(); }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationMode = notificationState.getMode().toString();
        this.notificationTimestamp = notificationState.getTimestamp();
    }
    public NotificationState getNotificationState(){
        if (notificationMode != null){
            return new NotificationState(NotificationState.NotificationMode.valueOf(notificationMode),
                    notificationTimestamp);
        } else return new NotificationState(NotificationState.NotificationMode.byDefault, 0);
    }

}
