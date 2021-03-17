package com.xabber.xmpp.groups;

import org.jivesoftware.smack.packet.IQ;

public abstract class GroupchatAbstractQueryIQ extends IQ {

    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE;
    public static final String ELEMENT = QUERY_ELEMENT;

    protected GroupchatAbstractQueryIQ(String childElementNamespace){
        super(ELEMENT, childElementNamespace);
    }

}
