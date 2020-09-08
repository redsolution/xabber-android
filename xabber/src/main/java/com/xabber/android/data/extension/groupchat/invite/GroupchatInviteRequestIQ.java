package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatMemberExtensionElement;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatInviteRequestIQ extends IQ {

    public static final String ELEMENT = "invite";
    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE + GroupchatInviteListQueryIQ.HASH_INVITE;
    public static final String CHILD_ELEMENT_JID = GroupchatMemberExtensionElement.ELEMENT_JID;
    public static final String CHILD_ELEMENT_REASON = "reason";
    public static final String CHILD_ELEMENT_SEND = "send";

    private String jid;
    private String reason;
    private boolean letGroupchatSendInviteMessage = false;

    public GroupchatInviteRequestIQ(ContactJid groupchatJid, ContactJid inviteJid) {
        super(ELEMENT, NAMESPACE);
        setType(Type.set);
        setTo(groupchatJid.getBareJid());
        jid = inviteJid.getBareJid().toString();
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setLetGroupchatSendInviteMessage(boolean letGroupchatSendInviteMessage) {
        this.letGroupchatSendInviteMessage = letGroupchatSendInviteMessage;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(CHILD_ELEMENT_JID, jid);
        xml.optElement(CHILD_ELEMENT_REASON, reason);
        xml.optElement(CHILD_ELEMENT_SEND, letGroupchatSendInviteMessage ? "true" : "");
        return xml;
    }
}
