package com.xabber.xmpp.blocking;


public class Unblock extends BasicBlockingIq {
    public static final String ELEMENT_NAME = "unblock";

    public Unblock() {
        super(ELEMENT_NAME);
    }

}
