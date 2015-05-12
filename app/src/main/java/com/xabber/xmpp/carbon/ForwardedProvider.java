package com.xabber.xmpp.carbon;

import java.io.IOException;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.xabber.android.data.LogManager;
import com.xabber.xmpp.AbstractExtensionProvider;

/**
 * Packet extension for XEP-0297: Stanza Forwarding. This class implements
 * a {@link PacketExtensionProvider} to parse forwarded messages from a packet. 
 * The extension <a href="http://xmpp.org/extensions/xep-0297.html">XEP-0297</a> 
 * is a prerequisite for XEP-0280 (Message Carbons).
 *
 * <p>The {@link ForwardedProvider} must be registered in the
 * <b>smack.providers</b> file for the element <b>forwarded</b> with
 * namespace <b>urn:xmpp:forwarded:0</b></p> to be used.
 *
 * @author Semyon Baranov
 */
public class ForwardedProvider extends AbstractExtensionProvider<Forwarded> {

    DelayInfoProvider dip = new DelayInfoProvider();
    
    @Override
    protected Forwarded createInstance(XmlPullParser parser) {

        return new Forwarded(null, null);
    }
        
    @Override
    public Forwarded parseExtension(XmlPullParser parser) throws Exception {
        DelayInfoProvider dip = new DelayInfoProvider();
        DelayInfo di = null;
        Packet packet = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("delay"))
                    di = (DelayInfo)dip.parseExtension(parser);
                else if (parser.getName().equals("message"))
                    packet = PacketParserUtils.parseMessage(parser);
                else throw new Exception("Unsupported forwarded packet type: " + parser.getName());
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(Forwarded.ELEMENT_NAME))
                done = true;
        }
        if (packet == null)
            throw new Exception("forwarded extension must contain a packet");
        return new Forwarded(di, packet);
    }
}
