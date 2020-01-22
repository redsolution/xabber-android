package com.xabber.android.data.database.messagerealm;

import android.os.Looper;

import com.xabber.android.data.log.LogManager;

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
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return uniqueId;
    }

    public String getJid() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return jid;
    }

    public void setJid(String jid) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.jid = jid;
    }

    public String getNickname() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return nickname;
    }

    public void setNickname(String nickname) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.nickname = nickname;
    }

    public String getRole() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return role;
    }

    public void setRole(String role) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.role = role;
    }

    public String getBadge() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return badge;
    }

    public void setBadge(String badge) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.badge = badge;
    }

    public String getAvatar() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return avatar;
    }

    public void setAvatar(String avatar) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.avatar = avatar;
    }

    public long getTimestamp() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(GroupchatUserRealm.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.timestamp = timestamp;
    }
}
