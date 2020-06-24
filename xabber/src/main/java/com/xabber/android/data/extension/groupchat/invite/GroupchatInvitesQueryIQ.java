package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;

public class GroupchatInvitesQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_INVITE = "#invite";

    public GroupchatInvitesQueryIQ(ContactJid groupchatJid) {
        super(ELEMENT, NAMESPACE + HASH_INVITE);
        setType(Type.get);
        setTo(groupchatJid.getBareJid());
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.InviteList;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }
}
