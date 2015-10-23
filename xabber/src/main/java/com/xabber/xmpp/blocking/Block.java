package com.xabber.xmpp.blocking;

public class Block extends BasicBlockingIq {
    public static final String ELEMENT_NAME = "block";

    public Block() {
        super(ELEMENT_NAME);
    }

}
