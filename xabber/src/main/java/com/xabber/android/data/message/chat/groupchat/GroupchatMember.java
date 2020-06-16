package com.xabber.android.data.message.chat.groupchat;

public class GroupchatMember {

    private String id;
    private String jid;
    private String groupchatJid;
    private String nickname;
    private String role;
    private String badge;
    private String avatarHash;
    private String avatarUrl;
    private String lastPresent;
    private long timestamp;

    public GroupchatMember(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getGroupchatJid() {
        return groupchatJid;
    }

    public void setGroupchatJid(String groupchatJid) {
        this.groupchatJid = groupchatJid;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastPresent() {
        return lastPresent;
    }

    public void setLastPresent(String lastPresent) {
        this.lastPresent = lastPresent;
    }

    public String getBestName() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        } else if (jid != null && !jid.isEmpty()) {
            return jid;
        } else {
            return id;
        }
    }
}
