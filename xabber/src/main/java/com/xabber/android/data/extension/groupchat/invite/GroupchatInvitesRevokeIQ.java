package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatInvitesRevokeIQ extends IQ {

    public static final String ELEMENT = "revoke";
    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE + "#invite";
    public static final String SUB_ELEMENT_JID = "jid";

    private String inviteJid;

    public GroupchatInvitesRevokeIQ(ContactJid groupchatContact, String jid) {
        super(ELEMENT, NAMESPACE);
        inviteJid = jid;
        setTo(groupchatContact.getBareJid());
        setType(Type.set);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(SUB_ELEMENT_JID, inviteJid);
        return xml;
    }
}
