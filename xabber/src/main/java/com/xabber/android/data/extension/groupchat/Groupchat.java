package com.xabber.android.data.extension.groupchat;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Groupchat implements ExtensionElement {

    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";
    public static final String ELEMENT = "x";

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
        xml.rightAngleBracket();
        appendToXML(xml);
        xml.closeElement(this);
        return xml;
    }

    public void appendToXML(XmlStringBuilder xml) { }
}
