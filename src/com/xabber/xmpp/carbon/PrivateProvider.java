package com.xabber.xmpp.carbon;

import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractExtensionProvider;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * a {@link PacketExtensionProvider} to exclude a <message/> from being 
 * forwarded to other Carbons-enabled resources. 
 * The extension <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> 
 * is meant to synchronize a message flow to multiple presences of a user.
 *
 * @author Semyon Baranov
 */
public class PrivateProvider  extends AbstractExtensionProvider<Private> {

    @Override
    protected Private createInstance(XmlPullParser parser) {
        return new Private();
    }
}
