/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.log;

import android.content.pm.ApplicationInfo;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;

import org.jivesoftware.smack.SmackConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Manager to write to the log.
 *
 * @author alexander.ivanov
 */
public class LogManager implements OnLoadListener {

    private static final boolean log;
    private static final boolean debuggable;

    static {
        debuggable = (Application.getInstance().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
//        log = debuggable && SettingsManager.debugLog();
        log = SettingsManager.debugLog();
    }

    private final static LogManager instance;

    static {
        instance = new LogManager(Application.getInstance());
        Application.getInstance().addManager(instance);

        System.setProperty("smack.debuggerClass", "com.xabber.android.data.log.SmackDebugger");
        System.setProperty("smack.debugEnabled", "true");
        SmackConfiguration.DEBUG = true;

    }

    public static LogManager getInstance() {
        return instance;
    }

    private LogManager(Application application) {
    }

    @Override
    public void onLoad() {
        if (log) {
            // TODO: unknown Options
//            Options.set("verbose");
//            Options.set("verbosemsg");
//            Options.set("verbosecompression");
//            Options.set("verbosesec");
//            Options.set("verbosecache");
        }
    }

    private static void dString(String tag, String msg) {
        if (log) {
            FileLog.d(tag, msg);
        }
    }

    private static void eString(String tag, String msg) {
        if (log) {
            FileLog.e(tag, msg);
        }
    }

    public static void iString(String tag, String msg) {
        if (log) {
            FileLog.d(tag, msg);
        }
    }

    private static void wString(String tag, String msg) {
        if (log) {
            FileLog.w(tag, msg);
        }
    }

    private static void vString(String tag, String msg) {
        if (log) {
            FileLog.d(tag, msg);
        }
    }

    static public void d(Object obj, String msg) {
        dString(obj.toString(), msg);
    }

    static public void e(Object obj, String msg) {
        eString(obj.toString(), msg);
    }

    static public void i(Object obj, String msg) {
        iString(obj.toString(), msg);
    }

    static public void w(Object obj, String msg) {
        wString(obj.toString(), msg);
    }

    static public void v(Object obj, String msg) {
        vString(obj.toString(), msg);
    }

    /**
     * Print stack trace if log is enabled.
     */
    public static void exception(Object obj, Exception exception) {
        FileLog.e(obj.toString(), exception);

        if (!log) {
            return;
        }
        forceException(obj, exception);
    }

    public static void exception(Object obj, Throwable throwable) {
        FileLog.e(obj.toString(), throwable);
    }

    /**
     * Print stack trace even if log is disabled.
     */
    private static void forceException(Object obj, Exception exception) {
        System.err.println(obj.toString());
        System.err.println(getStackTrace(exception));
    }

    private static String getStackTrace(Exception exception) {
        final StringWriter result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }

    public static boolean isDebuggable() {
        return debuggable;
    }

    public static void clearLogs() {
        FileLog.cleanupLogs();
    }

    public static File[] getLogFiles() {
        File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
        if (sdCard == null) {
            return new File[0];
        }
        File dir = new File(sdCard.getAbsolutePath() + "/logs");
        return dir.listFiles();

    }
}

