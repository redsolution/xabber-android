package com.xabber.xmpp.blocking;

import com.xabber.xmpp.IQ;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class BasicBlockingIq extends IQ {

    private List<String> items;
    private String elementName;

    public BasicBlockingIq(String elementName) {
        super(elementName, XmlConstants.NAMESPACE);
        this.elementName = elementName;
        items = new ArrayList<>();
    }

    @Override
    public String getElementName() {
        return elementName;
    }

    @Override
    public String getNamespace() {
        return XmlConstants.NAMESPACE;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void addItem(String item) {
        items.add(item);
    }

    public void addItems(List<String> items) {
        this.items.addAll(items);
    }

    public List<String> getItems() {
        return items;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        for (String itemJid : getItems()) {
            xml.halfOpenElement(XmlConstants.ITEM).attribute(XmlConstants.ITEM_JID, itemJid).closeEmptyElement();
        }
        return xml;
    }

    @Override
    public void serializeContent(XmlSerializer serializer) throws IOException {

    }

}
