package com.xabber.android.data.extension.delivery;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class TimeProvider extends ExtensionElementProvider<TimeElement> {

    @Override
    public TimeElement parse(XmlPullParser parser, int initialDepth) {
        String name = parser.getAttributeValue("", "by");
        String timestamp = parser.getAttributeValue("", "stamp");
        return new TimeElement(name, timestamp);
    }

}
