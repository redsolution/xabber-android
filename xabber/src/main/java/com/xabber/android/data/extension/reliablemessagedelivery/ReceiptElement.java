package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.pubsub.EmbeddedPacketExtension;

import java.util.List;
import java.util.Map;

public class ReceiptElement implements EmbeddedPacketExtension {

    public static final String NAMESPACE = "http://xabber.com/protocol/delivery";
    public static final String ELEMENT = "received";

    private TimeElement timeElement;
    private OriginIdElement originIdElement;
    private StanzaIdElement stanzaIdElement;

    @Override
    public String getNamespace() { return NAMESPACE; }

    @Override
    public String getElementName() { return ELEMENT; }

    public TimeElement getTimeElement() { return timeElement; }
    public OriginIdElement getOriginIdElement() { return originIdElement; }
    public StanzaIdElement getStanzaIdElement() { return stanzaIdElement; }

    public void setTimeElement(TimeElement timeElement) { this.timeElement = timeElement; }
    public void setOriginIdElement(OriginIdElement originId) { this.originIdElement = originId; }
    public void setStanzaIdElement(StanzaIdElement stanzaId) { this.stanzaIdElement = stanzaId; }

    @Override
    public List<ExtensionElement> getExtensions() {
        return null; //TODO desegn this really important
    }

    @Override
    public CharSequence toXML() {
        return null;
    }

    public static class ReceiptElementProvider extends EmbeddedExtensionProvider<ReceiptElement> {

        @Override
        protected ReceiptElement createReturnExtension(String currentElement, String currentNamespace, Map<String, String> attributeMap, List<? extends ExtensionElement> content) {
            ReceiptElement receiptElement = new ReceiptElement();
            for (ExtensionElement extensionElement : content) {
                StandardExtensionElement standardExtensionElement = (StandardExtensionElement) extensionElement;
                if (standardExtensionElement.getElementName().equals(TimeElement.ELEMENT)) {
                    String by = standardExtensionElement.getAttributeValue(TimeElement.ATTRIBUTE_BY);
                    String stamp = standardExtensionElement.getAttributeValue(TimeElement.ATTRIBUTE_STAMP);
                    receiptElement.setTimeElement(new TimeElement(by, stamp));
                } else if (standardExtensionElement.getElementName().equals(OriginIdElement.ELEMENT)){
                    String by = standardExtensionElement.getAttributeValue(OriginIdElement.ATTRIBUTE_BY);
                    String id = standardExtensionElement.getAttributeValue(OriginIdElement.ATTRIBUTE_ID);
                    receiptElement.setOriginIdElement(new OriginIdElement(by, id));
                } else if (standardExtensionElement.getElementName().equals(StanzaIdElement.ELEMENT)){
                    String by = standardExtensionElement.getAttributeValue(StanzaIdElement.ATTRIBUTE_BY);
                    String id = standardExtensionElement.getAttributeValue(StanzaIdElement.ATTRIBUTE_ID);
                    receiptElement.setStanzaIdElement(new StanzaIdElement(by, id));
                }
            }
            return receiptElement;
        }
    }

}
