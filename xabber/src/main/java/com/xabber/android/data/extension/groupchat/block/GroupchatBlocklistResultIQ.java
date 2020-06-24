package com.xabber.android.data.extension.groupchat.block;

import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;

import org.jivesoftware.smack.packet.IQ;

import java.util.ArrayList;

public class GroupchatBlocklistResultIQ extends IQ {

    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE
            + GroupchatBlocklistQueryIQ.HASH_BLOCK;
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String ELEMENT_JID = GroupchatUserExtension.ELEMENT_JID;
    public static final String ELEMENT_ID = GroupchatUserExtension.ATTR_ID;
    public static final String ELEMENT_DOMAIN = "domain";

    public static final String ATTRIBUTE_JID = GroupchatUserExtension.ELEMENT_JID;
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
