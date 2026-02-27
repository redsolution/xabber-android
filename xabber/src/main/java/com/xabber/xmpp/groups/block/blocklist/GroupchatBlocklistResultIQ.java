package com.xabber.xmpp.groups.block.blocklist;

import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;

import org.jivesoftware.smack.packet.IQ;

import java.util.ArrayList;

public class GroupchatBlocklistResultIQ extends IQ {

    public static final String NAMESPACE = GroupExtensionElement.NAMESPACE
            + GroupchatBlocklistQueryIQ.HASH_BLOCK;
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String ELEMENT_JID = GroupMemberExtensionElement.ELEMENT_JID;
    public static final String ELEMENT_ID = GroupMemberExtensionElement.ATTR_ID;
    public static final String ELEMENT_DOMAIN = "domain";

    public static final String ATTRIBUTE_JID = GroupMemberExtensionElement.ELEMENT_JID;
    public static final String ATTRIBUTE_NAME = "name";

    private ArrayList<GroupchatBlocklistItemElement> blockedItems;

    GroupchatBlocklistResultIQ() {
        super(ELEMENT, NAMESPACE);
    }

    public ArrayList<GroupchatBlocklistItemElement> getBlockedItems() {
        return blockedItems;
    }

    public void setBlockedItems(ArrayList<GroupchatBlocklistItemElement> blockedItems) {
        this.blockedItems = blockedItems;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (blockedItems != null && !blockedItems.isEmpty()) {
            xml.rightAngleBracket();
            for (GroupchatBlocklistItemElement item : blockedItems) {
                item.toXML(xml);
            }
        } else {
            xml.setEmptyElement();
        }
        return xml;
    }
}
