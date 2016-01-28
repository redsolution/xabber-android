package com.xabber.xmpp.blocking;

public class UnblockProvider extends BasicBlockingProvider<Unblock> {
    @Override
    protected Unblock createInstance() {
        return new Unblock();
    }
}
