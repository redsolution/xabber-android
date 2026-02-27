package com.xabber.xmpp.groups.invite.outgoing;

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ;
import com.xabber.android.data.message.chat.GroupChat;

public class GroupchatInviteListQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_INVITE = "#invite";

    public GroupchatInviteListQueryIQ(GroupChat groupChat) {
        super(NAMESPACE + HASH_INVITE);
        setType(Type.get);
        if (groupChat.getFullJidIfPossible() != null)
            setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }
}
