package com.xabber.android.data.database.realmobjects;

import androidx.annotation.NonNull;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class GroupMemberRealmObject extends RealmObject {

    @PrimaryKey
    @Required
    private String primaryKey;

    private String memberId;
    private String accountJid;
    private String groupJid;

    private String jid;
    private String nickname;
    private String role;
    private String badge;
    private String avatarHash;
    private String avatarUrl;
    private String lastSeen;

    private String subscriptionState;

    private boolean isMe = false;
    private boolean isBlocked = false;
    private boolean isKicked = false;

    private GroupMemberRealmObject(String uniqueId) {
        this.primaryKey = uniqueId;
    }
    public GroupMemberRealmObject() {
        this.primaryKey = UUID.randomUUID().toString();
    }

    public static GroupMemberRealmObject createGroupMemberRealmObject(AccountJid accountJid, ContactJid groupJid,
                                                                      String memberId){
        GroupMemberRealmObject gmro = new GroupMemberRealmObject(createPrimaryKey(accountJid, groupJid, memberId));

        gmro.accountJid = accountJid.toString();
        gmro.groupJid = groupJid.toString();
        gmro.memberId = memberId;

        return gmro;
    }

    private static String createPrimaryKey(AccountJid accountJid, ContactJid contactJid, String memberId){
        return accountJid.toString() + "#" + contactJid.toString() + "#" + memberId;
    }

    @NonNull
    public static GroupMemberRealmObject createFromMemberExtensionElement(GroupMemberExtensionElement extensionElement,
                                                                          AccountJid accountJid, ContactJid groupJid) {
        GroupMemberRealmObject gmro = createGroupMemberRealmObject(accountJid, groupJid, extensionElement.getId());

        if (extensionElement.getAvatarInfo() != null) {
            gmro.setAvatarHash(extensionElement.getAvatarInfo().getId());
            gmro.setAvatarUrl(extensionElement.getAvatarInfo().getUrl().toString());
        }

        if (extensionElement.getSubscription() != null) {
            gmro.setSubscriptionState(SubscriptionState.valueOf(extensionElement.getSubscription()));
        }

        gmro.setLastSeen(extensionElement.getLastPresent());
        gmro.setBadge(extensionElement.getBadge());
        gmro.setJid(extensionElement.getJid());
        gmro.setNickname(extensionElement.getNickname());
        gmro.setRole(Role.valueOf(extensionElement.getRole()));

        return gmro;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public ContactJid getGroupJid() {
        try {
            return ContactJid.from(groupJid);
        } catch (Exception e) {
            LogManager.exception(this, e);
            return null;
        }
    }

    public AccountJid getAccountJid() {
        try {
            return AccountJid.from(accountJid);
        } catch (Exception e) {
            LogManager.exception(this, e);
            return null;
        }
    }

    public String getMemberId() { return memberId; }

    public boolean isMe() { return isMe; }
    public void setMe(boolean me) {
        if (me) isMe = true;
    }

    public String getJid() {
        return jid;
    }
    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Role getRole() {
        return Role.valueOf(role);
    }
    public void setRole(Role role) {
        this.role = role.toString();
    }

    public String getBadge() {
        return badge;
    }
    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getAvatarHash() {
        return avatarHash;
    }
    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public boolean isKicked() { return isKicked; }
    public void setKicked(boolean kicked) { isKicked = kicked; }

    public SubscriptionState getSubscriptionState() { return SubscriptionState.valueOf(subscriptionState); }
    public void setSubscriptionState(SubscriptionState subscriptionState) {
        this.subscriptionState = subscriptionState.toString();
    }

    public static class Fields {
        public static final String PRIMARY_KEY = "primaryKey";
        public static final String MEMBER_ID = "memberId";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String GROUP_JID = "groupJid";
        public static final String JID = "jid";
        public static final String NICKNAME = "nickname";
        public static final String ROLE = "role";
        public static final String BADGE = "badge";
        public static final String AVATAR_HASH = "avatarHash";
        public static final String AVATAR_URL = "avatarUrl";
        public static final String LAST_SEEN = "lastSeen";
        public static final String TIMESTAMP = "timestamp";
        public static final String SUBSCRIPTION_STATE = "subscriptionState";
        public static final String IS_ME = "isMe";
    }

    public String getBestName() {
        if (nickname != null) return nickname;
        else if (jid != null) return jid;
        else return memberId;
    }

    public enum SubscriptionState {
        both, none
    }

    public enum Role {
        owner, admin, member, none
    }

}
