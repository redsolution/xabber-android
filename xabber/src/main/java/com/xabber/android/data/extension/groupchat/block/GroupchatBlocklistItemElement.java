package com.xabber.android.data.extension.groupchat.block;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatBlocklistItemElement {

    public enum ItemType {
        jid, domain, id
    }

    private ItemType type;
    private String item;

    private int hashCode = 0;

    public GroupchatBlocklistItemElement(ItemType type, String blockedItem) {
        this.type = type;
        this.item = blockedItem;
    }

    public ItemType getItemType() {
        return type;
    }

    public String getBlockedItem() {
        return item;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        switch (type) {
            case id:
            case jid:
                xml.element(type.name(), item);
                break;
            case domain:
                xml.openElement(type.name());
                xml.attribute("name", item);
                xml.closeEmptyElement();
        }
        return xml;
    }

    public void toXML(XmlStringBuilder xml) {
        switch (type) {
            case id:
            case jid:
                xml.element(type.name(), item);
                break;
            case domain:
                xml.openElement(type.name());
                xml.attribute("name", item);
                xml.closeEmptyElement();
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupchatBlocklistItemElement element = (GroupchatBlocklistItemElement) o;
        return type == element.type &&
                item.equals(element.item);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 17;

            result = result * 31 + item.hashCode();
            result = result * 31 + type.hashCode();
            hashCode = result;
        }
        return result;
    }
}
