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
        LogManager.i(LOG_TAG, logMessage, replaceMessageBody(logMessage));
    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
        LogManager.exception(LOG_TAG, throwable);
    }

    /**
     * Replace body of message with ***.
     */
    private static String replaceMessageBody(String sourceMsg) {
        if (sourceMsg.contains("</message>")) {
            try {
                int s = sourceMsg.indexOf("<body>");
                int f = sourceMsg.indexOf("</body>");
                if (s != -1 && f != -1)
                    return sourceMsg.substring(0, s + 6) + "***" + sourceMsg.substring(f);
                else return sourceMsg;
            } catch (Exception e) {
                return sourceMsg;
            }
        } else return sourceMsg;
    }

}
