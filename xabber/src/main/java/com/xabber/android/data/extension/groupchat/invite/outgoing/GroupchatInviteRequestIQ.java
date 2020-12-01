package com.xabber.android.data.extension.groupchat.invite.outgoing;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatMemberExtensionElement;
import com.xabber.android.data.message.chat.groupchat.GroupChat;

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

    public GroupchatInviteRequestIQ(GroupChat groupchat, ContactJid inviteJid) {
        super(ELEMENT, NAMESPACE);
        setType(Type.set);
        if (groupchat.getFullJidIfPossible() != null)
            setTo(groupchat.getFullJidIfPossible());
        else setTo(groupchat.getContactJid().getJid());
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
