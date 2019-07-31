package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class RefUser implements ExtensionElement {

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

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_ID, id);
        xml.rightAngleBracket();
        if (role != null) {
            xml.openElement(ELEMENT_ROLE);
            xml.append(role);
            xml.closeElement(ELEMENT_ROLE);
        }
        if (nickname != null) {
            xml.openElement(ELEMENT_NICKNAME);
            xml.append(nickname);
            xml.closeElement(ELEMENT_NICKNAME);
        }
        if (badge != null) {
            xml.openElement(ELEMENT_BADGE);
            xml.append(badge);
            xml.closeElement(ELEMENT_BADGE);
        }
        if (jid != null) {
            xml.openElement(ELEMENT_JID);
            xml.append(jid);
            xml.closeElement(ELEMENT_JID);
        }
        if (avatar != null) {
            xml.halfOpenElement(ELEMENT_METADATA);
            xml.xmlnsAttribute(NAMESPACE_METADATA);
            xml.rightAngleBracket();
            xml.halfOpenElement(ELEMENT_INFO);
            xml.attribute(ATTR_URL, avatar);
            xml.rightAngleBracket();
            xml.closeElement(ELEMENT_INFO);
            xml.closeElement(ELEMENT_METADATA);
        }
        xml.closeElement(this);
        return xml;
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
