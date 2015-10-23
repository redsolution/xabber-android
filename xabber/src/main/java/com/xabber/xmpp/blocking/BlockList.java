package com.xabber.xmpp.blocking;

/**
 * http://xmpp.org/extensions/xep-0191.html
 */
public class BlockList extends BasicBlockingIq {
    public static final String ELEMENT_NAME = "blocklist";

    public BlockList() {
        super(ELEMENT_NAME);
    }
}
