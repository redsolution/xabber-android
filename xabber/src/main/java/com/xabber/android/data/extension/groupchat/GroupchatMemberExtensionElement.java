package com.xabber.android.data.extension.groupchat;

import com.xabber.xmpp.avatar.MetadataInfo;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatMemberExtensionElement implements ExtensionElement {

    public static final String ELEMENT = "user";
    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";
    public static final String ATTR_ID = "id";

    public static final String ELEMENT_JID = "jid";
    public static final String ELEMENT_NICKNAME = "nickname";
    public static final String ELEMENT_ROLE = "role";
    public static final String ELEMENT_BADGE = "badge";
    public static final String ELEMENT_SUBSCRIPTION = "subscription";
    public static final String ELEMENT_METADATA = "metadata";
    public static final String ELEMENT_PRESENT = "present";
    public static final String NAMESPACE_METADATA = "urn:xmpp:avatar:metadata";
    public static final String ELEMENT_INFO = "info";
    public static final String ATTR_URL = "url";

    private String id;
    private String jid;
    private String nickname;
    private String role;
    private String badge;
    private String lastPresent;
    private String subscriprion;
    private MetadataInfo avatar;

    public GroupchatMemberExtensionElement(String id, String nickname, String role) {
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
        if (subscriprion != null) {
            xml.openElement(ELEMENT_SUBSCRIPTION);
            xml.append(subscriprion);
            xml.closeElement(ELEMENT_SUBSCRIPTION);
        }
        if (avatar != null) {
            xml.halfOpenElement(ELEMENT_METADATA);
            xml.xmlnsAttribute(NAMESPACE_METADATA);
            xml.rightAngleBracket();
            xml.halfOpenElement(ELEMENT_INFO);
            xml.optElement("id", avatar.getId());
            xml.optElement("bytes", avatar.getBytes());
            xml.optElement("type", avatar.getType());
            xml.optElement("url", avatar.getUrl().toString());
            if (avatar.getHeight() > 0) xml.attribute("height", avatar.getHeight());
            if (avatar.getWidth() > 0) xml.attribute("width", avatar.getWidth());
            xml.closeEmptyElement();
            xml.closeElement(ELEMENT_METADATA);
        }
        xml.closeElement(this);
        return xml;
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

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getLastPresent() {
        return lastPresent;
    }

    public void setLastPresent(String lastPresent) {
        this.lastPresent = lastPresent;
    }

    public MetadataInfo getAvatarInfo() {
        return avatar;
    }

    public void setAvatarInfo(MetadataInfo avatar) {
        this.avatar = avatar;
    }

    public String getSubscriprion() {
        return subscriprion;
    }

    public void setSubscriprion(String subscriprion) {
        this.subscriprion = subscriprion;
    }
}
