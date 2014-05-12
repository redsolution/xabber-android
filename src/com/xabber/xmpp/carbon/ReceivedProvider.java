package com.xabber.xmpp.carbon;

import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.android.data.LogManager;
import com.xabber.xmpp.AbstractExtensionProvider;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * a {@link PacketExtensionProvider} to parse a received message from a packet. 
 * The extension <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> 
 * is meant to synchronize a message flow to multiple presences of a user.
 *
 * @author Semyon Baranov
 */
public class ReceivedProvider extends AbstractExtensionProvider<Received> {
    @Override
    protected Received createInstance(XmlPullParser parser) {
        
        return new Received();
    }
    
    @Override
    protected boolean parseInner(XmlPullParser parser, Received instance)
            throws Exception {
        
        Forwarded forwarded = null;
        if (parser.getName().equals(Forwarded.ELEMENT_NAME)) {
            try {
                forwarded = (Forwarded) PacketParserUtils.parsePacketExtension(Forwarded.ELEMENT_NAME, Forwarded.NAMESPACE, parser);
            } catch (Exception e) {
                LogManager.exception(this, e);
            }
        } 
        
        if (forwarded == null) {
            LogManager.exception(this, new Exception("received extension must contain a forwarded extension"));
            return false;
        }
        instance.setForwarded(forwarded);
        return true;
    }
}
