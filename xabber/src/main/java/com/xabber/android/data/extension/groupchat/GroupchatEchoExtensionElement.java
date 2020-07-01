package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.message.chat.groupchat.GroupchatManager;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatEchoExtensionElement implements ExtensionElement {

    public static final String NAMESPACE = GroupchatManager.NAMESPACE + "#system-message";
    public static final String ELEMENT = "x";
    public static final String TYPE = "type";
    public static final String ECHO = "echo";

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
        xml.attribute(TYPE, ECHO);
        xml.rightAngleBracket();
        xml.closeElement(this);
        return xml;
    }



}
