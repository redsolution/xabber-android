package com.xabber.android.data.extension.groupchat.invite.outgoing;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;

public class GroupchatInviteListQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_INVITE = "#invite";

    public GroupchatInviteListQueryIQ(ContactJid groupchatJid) {
        super(NAMESPACE + HASH_INVITE);
        setType(Type.get);
        setTo(groupchatJid.getBareJid());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }
}
