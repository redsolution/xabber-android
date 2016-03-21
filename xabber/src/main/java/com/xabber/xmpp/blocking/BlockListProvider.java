package com.xabber.xmpp.blocking;

/**
 * http://xmpp.org/extensions/xep-0191.html
 */
public class BlockListProvider extends BasicBlockingProvider<BlockList> {

    @Override
    protected BlockList createInstance() {
        return new BlockList();
    }
}
