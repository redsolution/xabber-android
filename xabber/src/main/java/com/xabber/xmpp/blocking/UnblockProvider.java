package com.xabber.xmpp.blocking;

import org.xmlpull.v1.XmlPullParser;

public class UnblockProvider extends BasicBlockingProvider<Unblock> {
    @Override
    protected Unblock createInstance(XmlPullParser parser) {
        return new Unblock();
    }
}
