package com.xabber.xmpp.carbon;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the packet extension to parse a received message from a packet. 
 * The extension <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> 
 * is meant to synchronize a message flow to multiple presences of a user.
 *
 * @author Semyon Baranov
 */
public class Received extends PacketExtension {
    public static final String ELEMENT_NAME = "received";
    public static final String NAMESPACE = "urn:xmpp:carbons:2";
    private Forwarded fwd;
    
    public Received() {
    }

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
        return fwd != null;
    }
    
    /**
     * Gets the forwarded packet or null if the forwarded has not been set.
     *
     * @return the {@link Forwarded} message contained in this Carbon.
     */
    public Forwarded getForwarded() {
        
        return fwd;
    }
    
    /**
     * Sets the forwarded packet.
     * @param fwd - the {@link Forwarded} message contained in this Carbon.
     */
    public void setForwarded(Forwarded fwd) {
        
        this.fwd = fwd;
    }
}
