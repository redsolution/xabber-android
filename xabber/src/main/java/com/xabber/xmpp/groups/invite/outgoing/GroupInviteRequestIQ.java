package com.xabber.xmpp.groups.invite.outgoing;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.android.data.message.chat.GroupChat;

import org.jivesoftware.smack.packet.IQ;

public class GroupInviteRequestIQ extends IQ {

    public static final String ELEMENT = "invite";
    public static final String NAMESPACE = GroupExtensionElement.NAMESPACE + GroupchatInviteListQueryIQ.HASH_INVITE;
    public static final String CHILD_ELEMENT_JID = "jid";
    public static final String CHILD_ELEMENT_SEND = "send";

    private final String jid;
    private boolean letGroupchatSendInviteMessage = false;

    public GroupInviteRequestIQ(GroupChat groupchat, ContactJid inviteJid) {
        super(ELEMENT, NAMESPACE);
        setType(Type.set);
        if (groupchat.getFullJidIfPossible() != null)
            setTo(groupchat.getFullJidIfPossible());
        else setTo(groupchat.getContactJid().getJid());
        jid = inviteJid.getBareJid().toString();
    }

    public GroupInviteRequestIQ(GroupChat groupchat, ContactJid inviteJid, boolean letGroupchatSendInviteMessage){
        super(ELEMENT, NAMESPACE);
        setType(Type.set);
        if (groupchat.getFullJidIfPossible() != null)
            setTo(groupchat.getFullJidIfPossible());
        else setTo(groupchat.getContactJid().getJid());
        jid = inviteJid.getBareJid().toString();
        setLetGroupSendInviteMessage(letGroupchatSendInviteMessage);
    }

    public void setLetGroupSendInviteMessage(boolean letGroupchatSendInviteMessage) {
        this.letGroupchatSendInviteMessage = letGroupchatSendInviteMessage;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(CHILD_ELEMENT_JID, jid);
        xml.optElement(CHILD_ELEMENT_SEND, letGroupchatSendInviteMessage ? "true" : "false");
        return xml;
    }

}
