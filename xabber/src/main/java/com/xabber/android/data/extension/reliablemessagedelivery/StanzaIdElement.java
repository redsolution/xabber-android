package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;
import java.util.Map;

public class StanzaIdElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:sid:0";
    public static final String ELEMENT = "stanza-id";
    public static final String ATTRIBUTE_BY = "by";
    public static final String ATTRIBUTE_ID = "id";
    private String by = null;
    private String id = null;

    public StanzaIdElement(String by, String id) {
        this.by = by;
        this.id = id;
    }

    public StanzaIdElement() {
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

    public static class StanzaIdElementProvider extends EmbeddedExtensionProvider<StanzaIdElement> {
        @Override
        protected StanzaIdElement createReturnExtension(String currentElement, String currentNamespace, Map attributeMap, List content) {
            StanzaIdElement stanzaIdElement = new StanzaIdElement();
            stanzaIdElement.setBy((String) attributeMap.get(ATTRIBUTE_BY));
            stanzaIdElement.setId((String) attributeMap.get(ATTRIBUTE_ID));
            return stanzaIdElement;
        }
    }

}
