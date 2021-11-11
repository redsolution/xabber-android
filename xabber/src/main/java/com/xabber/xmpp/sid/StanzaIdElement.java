package com.xabber.xmpp.sid;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class StanzaIdElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:sid:0";
    public static final String ELEMENT = "stanza-id";
    public static final String ATTRIBUTE_BY = "by";
    public static final String ATTRIBUTE_ID = "id";
    private String by;
    private String id;

    public StanzaIdElement(String by, String id) {
        this.by = by;
        this.id = id;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.attribute(ATTRIBUTE_BY, by);
        xmlStringBuilder.attribute(ATTRIBUTE_ID, id);
        xmlStringBuilder.closeEmptyElement();
        return xmlStringBuilder;
    }

}
