package com.xabber.android.data.database.messagerealm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class GroupchatUserRealm extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String JID = "jid";
        public static final String NICKNAME = "nickname";
        public static final String ROLE = "role";
        public static final String BADGE = "badge";
        public static final String AVATAR = "avatar";
        public static final String TIMESTAMP = "timestamp";
    }

    @PrimaryKey
    @Required
    private String uniqueId;
    private String jid;
    private String nickname;
    private String role;
    private String badge;
    private String avatar;
    private long timestamp;

    public GroupchatUserRealm(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public GroupchatUserRealm() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
