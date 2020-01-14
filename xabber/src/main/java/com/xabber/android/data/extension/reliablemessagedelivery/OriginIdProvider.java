package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class OriginIdProvider extends ExtensionElementProvider<OriginIdElement>{
    @Override
    public OriginIdElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String id = parser.getAttributeValue("", "id");
        return new OriginIdElement(id);
    }
}
