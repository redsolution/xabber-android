package com.xabber.android.data.extension.references;

public class RefUser {

    public static final String ELEMENT            = "user";
    public static final String NAMESPACE          = "http://xabber.com/protocol/groupchat";
    public static final String ATTR_ID            = "id";

    public static final String ELEMENT_JID        = "jid";
    public static final String ELEMENT_NICKNAME   = "nickname";
    public static final String ELEMENT_ROLE       = "role";
    public static final String ELEMENT_BADGE      = "badge";
    public static final String ELEMENT_METADATA   = "metadata";
    public static final String NAMESPACE_METADATA = "urn:xmpp:avatar:metadata";
    public static final String ELEMENT_INFO       = "info";
    public static final String ATTR_URL           = "url";

    private String id;
    private String jid;
    private String nickname;
    private String role;
    private String badge;
    private String avatar;

    public RefUser(String id, String nickname, String role) {
        this.id = id;
        this.nickname = nickname;
        this.role = role;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    public String getBadge() {
        return badge;
    }

    public String getAvatar() {
        return avatar;
    }
}
