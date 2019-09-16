package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.packet.IQ;

public class AvatarAccept extends IQ {
    protected AvatarAccept(String childElementName, String childElementNamespace) {
        super(childElementName, childElementNamespace);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        return null;
    }
}
