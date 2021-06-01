package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class StanzaIdProvider extends ExtensionElementProvider<StanzaIdElement> {

    @Override
    public StanzaIdElement parse(XmlPullParser parser, int initialDepth) {
        String name = parser.getAttributeValue("", "by");
        String id = parser.getAttributeValue("", "id");
        return new StanzaIdElement(name, id);
    }
}
