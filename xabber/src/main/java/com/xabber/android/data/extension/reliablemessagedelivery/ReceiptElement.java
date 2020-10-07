package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.pubsub.EmbeddedPacketExtension;

import java.util.List;
import java.util.Map;

public class ReceiptElement implements EmbeddedPacketExtension {

    public static final String NAMESPACE = ReliableMessageDeliveryManager.NAMESPACE;
    public static final String ELEMENT = "received";

    private TimeElement timeElement;
    private OriginIdElement originIdElement;
    private StanzaIdElement stanzaIdElement;

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public TimeElement getTimeElement() {
        return timeElement;
    }

    public void setTimeElement(TimeElement timeElement) {
        this.timeElement = timeElement;
    }

    public OriginIdElement getOriginIdElement() {
        return originIdElement;
    }

    public void setOriginIdElement(OriginIdElement originId) {
        this.originIdElement = originId;
    }

    public StanzaIdElement getStanzaIdElement() {
        return stanzaIdElement;
    }

    public void setStanzaIdElement(StanzaIdElement stanzaId) {
        this.stanzaIdElement = stanzaId;
    }

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
                switch (extensionElement.getNamespace()) {
                    case TimeElement.NAMESPACE:
                        receiptElement.setTimeElement((TimeElement)extensionElement);
                        break;
                    case StanzaIdElement.NAMESPACE: //both Stanza-id and Origin-id have the same namespace
                        if (extensionElement instanceof StanzaIdElement) {
                            receiptElement.setStanzaIdElement((StanzaIdElement)extensionElement);
                        } else if (extensionElement instanceof OriginIdElement){
                            receiptElement.setOriginIdElement((OriginIdElement)extensionElement);
                        }
                        break;
                }
            }
            return receiptElement;
        }
    }

}
