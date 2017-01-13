package com.xabber.android.data.log;

import android.util.Log;
import java.util.logging.*;

/**
 * Make JUL work on Android.
 *
 * http://stackoverflow.com/questions/4561345/how-to-configure-java-util-logging-on-android/9047282#9047282
 */
public class AndroidLoggingHandler extends Handler {

    public static void reset(Handler rootHandler) {
        Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }
        java.util.logging.LogManager.getLogManager().getLogger("").addHandler(rootHandler);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
        if (!super.isLoggable(record))
            return;

        String tag = record.getLoggerName();

        try {
            int level = getAndroidLevel(record.getLevel());

            switch (level) {
                case Log.ERROR:
                    LogManager.e(tag, record.getMessage());
                    break;
                case Log.WARN:
                    LogManager.w(tag, record.getMessage());
                    break;
                case Log.INFO:
                    LogManager.i(tag, record.getMessage());
                    break;
                case Log.DEBUG:
                    LogManager.d(tag, record.getMessage());
                    break;
            }

            if (record.getThrown() != null) {
                LogManager.exception(tag, record.getThrown());
            }
        } catch (RuntimeException e) {
            Log.e("AndroidLoggingHandler", "Error logging message.", e);
        }
    }

    private static int getAndroidLevel(Level level) {
        int value = level.intValue();
        if (value >= 1000) {
            return Log.ERROR;
        } else if (value >= 900) {
            return Log.WARN;
        } else if (value >= 800) {
            return Log.INFO;
        } else {
            return Log.DEBUG;
        }
    }
}
