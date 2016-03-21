package com.xabber.xmpp.blocking;

import org.jivesoftware.smack.packet.IQ;

import java.util.ArrayList;
import java.util.List;

abstract class BasicBlockingIq extends IQ {

    private List<String> items;

    public BasicBlockingIq(String elementName) {
        super(elementName, XmlConstants.NAMESPACE);
        items = new ArrayList<>();
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

}
