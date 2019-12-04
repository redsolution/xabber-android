package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;
import java.util.Map;

public class OriginIdElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:sid:0";
    public static final String ELEMENT = "origin-id";
    public static final String ATTRIBUTE_BY = "by";
    public static final String ATTRIBUTE_ID = "id";
    private String by = null;
    private String id = null;

    @Override
    public String getNamespace() { return NAMESPACE; }
    @Override
    public String getElementName() { return ELEMENT; }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    OriginIdElement(String by, String id){
        this.by = by;
        this.id = id;
    }

    OriginIdElement(){}

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.attribute(ATTRIBUTE_BY, by);
        xmlStringBuilder.attribute(ATTRIBUTE_ID, id);
        xmlStringBuilder.closeElement(ELEMENT);
        return xmlStringBuilder;
    }

    public static class OriginIdElementProvider extends EmbeddedExtensionProvider<OriginIdElement> {
        @Override
        protected OriginIdElement createReturnExtension(String currentElement, String currentNamespace, Map attributeMap, List content) {
            OriginIdElement originIdElement = new OriginIdElement();
            originIdElement.setBy((String) attributeMap.get(ATTRIBUTE_BY));
            originIdElement.setId((String) attributeMap.get(ATTRIBUTE_ID));
            return originIdElement;
        }
    }
}
