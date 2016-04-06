package com.xabber.xmpp.blocking;

import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.List;

abstract class BasicBlockingIq extends IQ {

    private List<Jid> items;

    public BasicBlockingIq(String elementName) {
        super(elementName, XmlConstants.NAMESPACE);
        items = new ArrayList<>();
    }

    public void addItem(Jid item) {
        items.add(item);
    }

    public void addItems(List<Jid> items) {
        this.items.addAll(items);
    }

    public List<Jid> getItems() {
        return items;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        for (Jid itemJid : getItems()) {
            xml.halfOpenElement(XmlConstants.ITEM).attribute(XmlConstants.ITEM_JID, itemJid).closeEmptyElement();
        }
        return xml;
    }

}
