package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class GroupchatUserRealmObject extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String JID = "jid";
        public static final String GROUPCHAT_JID = "groupchatJid";
        public static final String NICKNAME = "nickname";
        public static final String ROLE = "role";
        public static final String BADGE = "badge";
        public static final String AVATAR_HASH = "avatarHash";
        public static final String AVATAR_URL = "avatarUrl";
        public static final String LAST_PRESENT = "lastPresent";
        public static final String TIMESTAMP = "timestamp";
    }

    @PrimaryKey
    @Required
    private String uniqueId;
    private String jid;
    private String groupchatJid;
    private String nickname;
    private String role;
    private String badge;
    private String avatarHash;
    private String avatarUrl;
    private String lastPresent;
    private long timestamp;

    public GroupchatUserRealmObject(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public GroupchatUserRealmObject() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() { return uniqueId;  }

    public String getJid() { return jid; }

    public void setJid(String jid) { this.jid = jid; }

    public String getNickname() { return nickname; }

    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public String getBadge() { return badge; }

    public String getGroupchatJid() {
        return groupchatJid;
    }

    public void setGroupchatJid(String groupchatJid) {
        this.groupchatJid = groupchatJid;
    }

    public void setBadge(String badge) { this.badge = badge; }

    public String getAvatarHash() { return avatarHash; }

    public void setAvatarHash(String avatarHash) {this.avatarHash = avatarHash; }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastPresent() {
        return lastPresent;
    }

    public void setLastPresent(String lastPresent) {
        this.lastPresent = lastPresent;
    }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
