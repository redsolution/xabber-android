package com.xabber.android.data.extension.groupchat.block;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatBlocklistUnblockIQ extends IQ {

    public static final String ELEMENT = "unblock";
    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE + GroupchatBlocklistQueryIQ.HASH_BLOCK;
    public static final String ELEMENT_USER = GroupchatUserExtension.ELEMENT;
    public static final String ELEMENT_JID = GroupchatUserExtension.ELEMENT_JID;
    public static final String ELEMENT_DOMAIN = GroupchatBlocklistResultIQ.ELEMENT_DOMAIN;
    public static final String ELEMENT_ID = GroupchatBlocklistResultIQ.ELEMENT_ID;

    public static final String ATTRIBUTE_NAME = GroupchatBlocklistResultIQ.ATTRIBUTE_NAME;

    private GroupchatBlocklistItemElement blockedElement;

    public GroupchatBlocklistUnblockIQ(ContactJid groupchatContact, GroupchatBlocklistItemElement blockedElement) {
        super(ELEMENT, NAMESPACE);
        this.blockedElement = blockedElement;
        setTo(groupchatContact.getBareJid());
        setType(Type.set);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (blockedElement != null) {
            xml.rightAngleBracket();
            blockedElement.toXML(xml);
        } else {
            xml.setEmptyElement();
        }
        return xml;
    }


}
