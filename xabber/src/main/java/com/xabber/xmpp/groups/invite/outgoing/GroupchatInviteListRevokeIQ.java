package com.xabber.xmpp.groups.invite.outgoing;

import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.android.data.message.chat.GroupChat;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatInviteListRevokeIQ extends IQ {

    public static final String ELEMENT = "revoke";
    public static final String NAMESPACE = GroupExtensionElement.NAMESPACE + "#invite";
    public static final String SUB_ELEMENT_JID = "jid";

    private final String inviteJid;

    public GroupchatInviteListRevokeIQ(GroupChat groupChat, String jid) {
        super(ELEMENT, NAMESPACE);
        inviteJid = jid;
        if (groupChat.getFullJidIfPossible() != null)
            setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
        setType(Type.set);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(SUB_ELEMENT_JID, inviteJid);
        return xml;
    }
}
