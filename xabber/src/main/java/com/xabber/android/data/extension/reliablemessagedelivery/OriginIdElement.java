package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;
import java.util.Map;

public class OriginIdElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:sid:0";
    public static final String ELEMENT = "origin-id";
    public static final String ATTRIBUTE_ID = "id";
    private String id = null;

    public OriginIdElement(String id) {
        this.id = id;
    }

    public OriginIdElement() {
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
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
        xmlStringBuilder.attribute(ATTRIBUTE_ID, id);
        xmlStringBuilder.closeEmptyElement();
        return xmlStringBuilder;
    }

    public static class OriginIdElementProvider extends EmbeddedExtensionProvider<OriginIdElement> {
        @Override
        protected OriginIdElement createReturnExtension(String currentElement, String currentNamespace, Map attributeMap, List content) {
            OriginIdElement originIdElement = new OriginIdElement();
            originIdElement.setId((String) attributeMap.get(ATTRIBUTE_ID));
            return originIdElement;
        }
    }
}
