package com.xabber.xmpp.groups.block.blocklist;

import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.android.data.message.chat.GroupChat;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatBlocklistUnblockIQ extends IQ {

    public static final String ELEMENT = "unblock";
    public static final String NAMESPACE = GroupExtensionElement.NAMESPACE + GroupchatBlocklistQueryIQ.HASH_BLOCK;
    public static final String ELEMENT_USER = GroupMemberExtensionElement.ELEMENT;
    public static final String ELEMENT_JID = GroupMemberExtensionElement.ELEMENT_JID;
    public static final String ELEMENT_DOMAIN = GroupchatBlocklistResultIQ.ELEMENT_DOMAIN;
    public static final String ELEMENT_ID = GroupchatBlocklistResultIQ.ELEMENT_ID;

    public static final String ATTRIBUTE_NAME = GroupchatBlocklistResultIQ.ATTRIBUTE_NAME;

    private GroupchatBlocklistItemElement blockedElement;

    public GroupchatBlocklistUnblockIQ(GroupChat groupChat, GroupchatBlocklistItemElement blockedElement) {
        super(ELEMENT, NAMESPACE);
        this.blockedElement = blockedElement;
        if (groupChat.getFullJidIfPossible() != null)
            setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
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
