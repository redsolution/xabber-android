package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType;
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType;
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class GroupchatRealmObject extends RealmObject {

    private static final String LOG_TAG = GroupchatRealmObject.class.getSimpleName();
    @PrimaryKey
    private String primary;
    private String groupchatJid;
    private String accountJid;
    private String owner;
    private String name;
    private String privacy = GroupchatPrivacyType.NONE.toString();
    private String index = GroupchatIndexType.NONE.toString();
    private String membership = GroupchatMembershipType.NONE.toString();
    private String description;
    private MessageRealmObject pinnedMessage;
    private RealmList<String> membersIds = new RealmList<>();
    private String membersListVersion;
    private boolean canInvite;
    private boolean canChangeSettings;
    private boolean canChangeUsersSettings;
    private boolean canChangeNicknames;
    private boolean canChangeBadge;
    private boolean canBlockUsers;
    private boolean canChangeAvatars;
    //maybe should be NotificationState
    private String notificationMode;
    //maybe useless
    private int membersCount;
    //i dunno what is it
    private long present;
    private boolean collect;
    private boolean peerToPeer;
    private RealmList<String> domains;
    private RealmList<String> invited;
    private String status;
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

    //TODO must change return type
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

    public GroupchatPrivacyType getPrivacy() {
        if (privacy != null) {
            switch (privacy) {
                case "incognitoGroupChat":
                    return GroupchatPrivacyType.INCOGNITO;
                case "publicGroupChat":
                    return GroupchatPrivacyType.PUBLIC;
                default:
                    return GroupchatPrivacyType.NONE;
            }
        }
        return GroupchatPrivacyType.NONE;
    }
    public void setPrivacy(GroupchatPrivacyType privacy) {
        if (privacy != null)
            this.privacy = privacy.toString();
    }

    public GroupchatIndexType getIndex() {
        if (index != null) {
            switch (index) {
                case "global":
                    return GroupchatIndexType.GLOBAL;
                case "local":
                    return GroupchatIndexType.LOCAL;
                default:
                    return GroupchatIndexType.NONE;
            }
        }
        return GroupchatIndexType.NONE;
    }
    public void setIndex(GroupchatIndexType index) {
        if (index != null)
            this.index = index.toString();
    }

    public GroupchatMembershipType getMembership() {
        if (membership != null) {
            switch (membership) {
                case "open":
                    return GroupchatMembershipType.OPEN;
                case "memberOnly":
                    return GroupchatMembershipType.MEMBER_ONLY;
                default:
                    return GroupchatMembershipType.NONE;
            }
        }
        return GroupchatMembershipType.NONE;
    }
    public void setMembership(GroupchatMembershipType membership) {
        if (membership != null)
            this.membership = membership.toString();
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

    public RealmList<String> getMembersIds() { return membersIds; }
    public void setMembersIds(RealmList<String> membersIds) { this.membersIds = membersIds; }
    public void clearMembersIds(){ this.membersIds.clear(); }
    public void addMemberId(String memberId){ this.membersIds.add(memberId); }

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

    public NotificationState.NotificationMode getNotificationMode() {
        switch (this.notificationMode) {
            case "enabled":
                return NotificationState.NotificationMode.enabled;
            case "disabled":
                return NotificationState.NotificationMode.disabled;
            case "snooze15m":
                return NotificationState.NotificationMode.snooze15m;
            case "snooze1h":
                return NotificationState.NotificationMode.snooze1h;
            case "snooze2h":
                return NotificationState.NotificationMode.snooze2h;
            case "snooze1d":
                return NotificationState.NotificationMode.snooze1d;
            case "onlyMentions":
                return NotificationState.NotificationMode.onlyMentions;
            default:
                return NotificationState.NotificationMode.byDefault;
        }
    }
    public void setNotificationMode(NotificationState.NotificationMode notificationMode) {
        this.notificationMode = notificationMode.toString();
    }

    public static final class Fields {
        public static final String PRIMARY_KEY = "primaryKey";
        public static final String GROUPCHAT_JID = "groupchatJid";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String OWNER = "owner";
    }

}
