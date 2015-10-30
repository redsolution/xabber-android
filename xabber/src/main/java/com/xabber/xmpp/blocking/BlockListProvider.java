package com.xabber.xmpp.blocking;

import org.xmlpull.v1.XmlPullParser;

/**
 * http://xmpp.org/extensions/xep-0191.html
 */
public class BlockListProvider extends BasicBlockingProvider<BlockList> {

    @Override
    protected BlockList createInstance(XmlPullParser parser) {
        return new BlockList();
    }
}
