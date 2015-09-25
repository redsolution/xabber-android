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
package com.xabber.android.data;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jivesoftware.smack.SmackConfiguration;
import org.xbill.DNS.Options;

import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * Manager to write to the log.
 *
 * @author alexander.ivanov
 */
public class LogManager implements OnLoadListener {

    private static final boolean log;
    private static final boolean debugable;

    static {
        debugable = (Application.getInstance().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        log = debugable && SettingsManager.debugLog();
    }

    private final static LogManager instance;

    static {
        instance = new LogManager(Application.getInstance());
        Application.getInstance().addManager(instance);
    }

    public static LogManager getInstance() {
        return instance;
    }

    private LogManager(Application application) {
    }

    @Override
    public void onLoad() {
        if (log) {
            System.setProperty("smack.debuggerClass",
                    "org.jivesoftware.smack.debugger.ConsoleDebugger");
            // "com.xabber.android.data.FileLogDebugger");
            System.setProperty("smack.debugEnabled", "true");
            SmackConfiguration.DEBUG = true;
            Options.set("verbose");
            Options.set("verbosemsg");
            Options.set("verbosecompression");
            Options.set("verbosesec");
            Options.set("verbosecache");
        }
    }

    static public int dString(String tag, String msg) {
        if (log)
            return Log.d(tag, msg);
        else
            return 0;
    }

    static public int eString(String tag, String msg) {
        if (log)
            return Log.e(tag, msg);
        else
            return 0;
    }

    static public int iString(String tag, String msg) {
        if (log)
            return Log.i(tag, msg);
        else
            return 0;
    }

    static public int wString(String tag, String msg) {
        if (log)
            return Log.w(tag, msg);
        else
            return 0;
    }

    static public int vString(String tag, String msg) {
        if (log)
            return Log.v(tag, msg);
        else
            return 0;
    }

    static public int d(Object obj, String msg) {
        return dString(obj.toString(), msg);
    }

    static public int e(Object obj, String msg) {
        return eString(obj.toString(), msg);
    }

    static public int i(Object obj, String msg) {
        return iString(obj.toString(), msg);
    }

    static public int w(Object obj, String msg) {
        return wString(obj.toString(), msg);
    }

    static public int v(Object obj, String msg) {
        return vString(obj.toString(), msg);
    }

    /**
     * Print stack trace if log is enabled.
     *
     * @param obj
     * @param exception
     */
    public static void exception(Object obj, Exception exception) {
        if (!log)
            return;
        forceException(obj, exception);
    }

    /**
     * Print stack trace even if log is disabled.
     *
     * @param obj
     * @param exception
     */
    public static void forceException(Object obj, Exception exception) {
        System.err.println(obj.toString());
        System.err.println(getStackTrace(exception));
    }

    /**
     * @param exception
     * @return stack trace.
     */
    private static String getStackTrace(Exception exception) {
        final StringWriter result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }

    public static boolean isDebugable() {
        return debugable;
    }

}
