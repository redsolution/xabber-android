package com.xabber.android.data.log;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.AbstractDebugger;

import java.io.Reader;
import java.io.Writer;

public class SmackDebugger extends AbstractDebugger {
    private static final String LOG_TAG = "Smack";

    public SmackDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        super(connection, writer, reader);
    }

    @Override
    protected void log(String logMessage) {
        LogManager.i(LOG_TAG, logMessage);

    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
        LogManager.exception(LOG_TAG, throwable);
    }

}
