package com.xabber.xmpp.blocking;

import org.xmlpull.v1.XmlPullParser;

public class BlockProvider extends BasicBlockingProvider<Block> {
    @Override
    protected Block createInstance(XmlPullParser parser) {
        return new Block();
    }
}
