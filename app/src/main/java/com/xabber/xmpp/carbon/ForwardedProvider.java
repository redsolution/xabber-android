package com.xabber.xmpp.carbon;

import com.xabber.xmpp.AbstractExtensionProvider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.delay.provider.DelayInformationProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Packet extension for XEP-0297: Stanza Forwarding. This class implements
 * a {@link AbstractExtensionProvider} to parse forwarded messages from a packet.
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

    DelayInformationProvider dip = new DelayInformationProvider();
    
    @Override
    protected Forwarded createInstance(XmlPullParser parser) {

        return new Forwarded(null, null);
    }

    @Override
    public Forwarded parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        DelayInformationProvider delayInformationProvider = new DelayInformationProvider();
        DelayInformation delayInformation = null;
        Stanza packet = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("delay"))
                    delayInformation = delayInformationProvider.parse(parser);
                else if (parser.getName().equals("message"))
                    packet = PacketParserUtils.parseMessage(parser);
                else throw new SmackException("Unsupported forwarded packet type: " + parser.getName());
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(Forwarded.ELEMENT_NAME))
                done = true;
        }
        if (packet == null)
            throw new SmackException("forwarded extension must contain a packet");
        return new Forwarded(delayInformation, packet);
    }
}
