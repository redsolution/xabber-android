package com.xabber.xmpp.carbon;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the packet extension to exclude a <message/> from being forwarded 
 * to other Carbons-enabled resources. 
 * The extension <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> 
 * is meant to synchronize a message flow to multiple presences of a user.
 *
 * @author Semyon Baranov
 */
public class Private extends PacketExtension {
    
    public static final String ELEMENT_NAME = "private";
    public static final String NAMESPACE = "urn:xmpp:carbons:2";
    
    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void serializeContent(XmlSerializer serializer) throws IOException {
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
