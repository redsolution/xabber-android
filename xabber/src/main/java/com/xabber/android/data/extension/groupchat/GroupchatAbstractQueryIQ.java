package com.xabber.android.data.extension.groupchat;

import org.jivesoftware.smack.packet.IQ;

public abstract class GroupchatAbstractQueryIQ extends IQ {

    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE;
    public static final String ELEMENT = QUERY_ELEMENT;

    protected GroupchatAbstractQueryIQ(String childElementName, String childElementNamespace) {
        super(childElementName, childElementNamespace);
    }

}
