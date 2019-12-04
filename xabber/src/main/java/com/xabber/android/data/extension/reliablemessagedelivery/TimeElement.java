package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;
import java.util.Map;

public class TimeElement implements ExtensionElement {

    public static final String NAMESPACE = "";
    public static final String ELEMENT = "time";
    public static final String ATTRIBUTE_BY = "by";
    public static final String ATTRIBUTE_STAMP = "stamp";

    private String by = null;
    private String stamp = null;

    @Override
    public String getNamespace() { return NAMESPACE; }

    @Override
    public String getElementName() { return ELEMENT; }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }

    public String getStamp() { return stamp; }
    public void setStamp(String stamp) { this.stamp = stamp; }

    public TimeElement(String by, String timeStamp){
        this.by = by;
        this.stamp = timeStamp;
    }

    public TimeElement(StandardExtensionElement standardExtensionElement){
        if (!standardExtensionElement.getElementName().equals(ELEMENT))
            throw new ClassCastException("Not a Time Element!");
        this.by = standardExtensionElement.getAttributeValue(ATTRIBUTE_BY);
        this.stamp = standardExtensionElement.getAttributeValue(ATTRIBUTE_STAMP);
    }

    public TimeElement(){}

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.attribute(ATTRIBUTE_BY, by);
        xmlStringBuilder.attribute(ATTRIBUTE_STAMP, stamp);
        xmlStringBuilder.closeElement(ELEMENT);
        return xmlStringBuilder;
    }

    public static class TimeElementProvider extends EmbeddedExtensionProvider<TimeElement>{
        @Override
        protected TimeElement createReturnExtension(String currentElement, String currentNamespace, Map attributeMap, List content) {
            TimeElement timeElement = new TimeElement();
            timeElement.setBy((String) attributeMap.get(ATTRIBUTE_BY));
            timeElement.setStamp((String) attributeMap.get(ATTRIBUTE_STAMP));
            return timeElement;
        }
    }
}
