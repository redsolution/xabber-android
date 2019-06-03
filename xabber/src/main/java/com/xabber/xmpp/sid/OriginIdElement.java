package com.xabber.xmpp.sid;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class OriginIdElement implements ExtensionElement {

    private String id;

    public OriginIdElement(String id) {
        this.id = id;
    }

    @Override
    public String getNamespace() {
        return UniqStanzaHelper.NAMESPACE;
    }

    @Override
    public String getElementName() {
        return UniqStanzaHelper.ELEMENT_NAME_ORIGIN;
    }

    @Override
    public CharSequence toXML() {
        return new XmlStringBuilder(OriginIdElement.this)
                .attribute(UniqStanzaHelper.ATTRIBUTE_ID, id)
                .closeEmptyElement();
    }
}