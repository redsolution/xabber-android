package com.xabber.xmpp.blocking;

public class BlockProvider extends BasicBlockingProvider<Block> {
    @Override
    protected Block createInstance() {
        return new Block();
    }
}
