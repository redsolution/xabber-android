package com.xabber.xmpp.carbon;

import java.io.IOException;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Container;
import com.xabber.xmpp.PacketExtension;

/**
 * Packet extension for XEP-0297: Stanza Forwarding. This class implements
 * the packet extension to parse
 * forwarded messages from a packet. The extension
 * <a href="http://xmpp.org/extensions/xep-0297.html">XEP-0297</a> is
 * a prerequisite for XEP-0280 (Message Carbons).
 *
 * @author Semyon Baranov
 */
public class Forwarded extends PacketExtension implements Container {
        
    public static final String NAMESPACE = "urn:xmpp:forward:0";
    public static final String ELEMENT_NAME = "forwarded";
    
    private DelayInfo delay;
    private Packet forwardedPacket;
    
    public Forwarded() {        
    }
    
    /**
     * Creates a new Forwarded packet extension.
     *
     * @param delay an optional {@link DelayInfo} timestamp of the packet.
     * @param fwdPacket the packet that is forwarded (required).
     */
    public Forwarded(DelayInfo delay, Packet fwdPacket) {
        this();
        this.delay = delay;
        this.forwardedPacket = fwdPacket;
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
    public boolean isValid() {

        return forwardedPacket != null;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
    }

    @Override
    public void serializeContent(XmlSerializer serializer) throws IOException {
    }
    
    /**
     * get the packet forwarded by this stanza.
     *
     * @return the {@link Packet} instance (typically a message) that was forwarded.
     */
    public Packet getForwardedPacket() {
        return forwardedPacket;
    }
    
    /**
     * get the timestamp of the forwarded packet.
     *
     * @return the {@link DelayInfo} representing the time when the original packet was sent. May be null.
     */
    public DelayInfo getDelayInfo() {
        return delay;
    }
}