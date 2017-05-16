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
import android.util.Log;

import com.xabber.android.data.Application;
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
public class LogManager {

    private static volatile boolean fileLog;
    private static boolean debuggable;

    private static LogManager instance;

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    private LogManager() {
        debuggable = (Application.getInstance().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        System.setProperty("smack.debuggerClass", "com.xabber.android.data.log.SmackDebugger");

        onSettingsChanged();
    }

    public void onSettingsChanged() {
        fileLog = SettingsManager.fileLog();

        if (debuggable || fileLog) {
            System.setProperty("smack.debugEnabled", "true");
            SmackConfiguration.DEBUG = true;
        } else {
            System.setProperty("smack.debugEnabled", "false");
            SmackConfiguration.DEBUG = false;
        }
    }

    private static void dString(String tag, String msg) {
        if (debuggable) {
            Log.d(tag, msg);
        }

        if (fileLog) {
            FileLog.d(tag, msg);
        }
    }

    private static void eString(String tag, String msg) {
        if (debuggable) {
            Log.e(tag, msg);
        }

        if (fileLog) {
            FileLog.e(tag, msg);
        }
    }

    public static void iString(String tag, String msg) {
        if (debuggable) {
            Log.i(tag, msg);
        }

        if (fileLog) {
            FileLog.d(tag, msg);
        }
    }

    /**
     * Overload of iString
     * Don't write to log confidential information
     * Write to log censored message
     */
    public static void iString(String tag, String msg, String censoredMsg) {
        if (debuggable) {
            Log.i(tag, msg);
        }

        if (fileLog) {
            FileLog.d(tag, censoredMsg);
        }
    }

    private static void wString(String tag, String msg) {
        if (debuggable) {
            Log.w(tag, msg);
        }

        if (fileLog) {
            FileLog.w(tag, msg);
        }
    }

    private static void vString(String tag, String msg) {
        if (debuggable) {
            Log.v(tag, msg);
        }

        if (fileLog) {
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

    static public void i(Object obj, String msg, String censoredMsg) {
        iString(obj.toString(), msg, censoredMsg);
    }

    static public void w(Object obj, String msg) {
        wString(obj.toString(), msg);
    }

    static public void v(Object obj, String msg) {
        vString(obj.toString(), msg);
    }

    public static void exception(Object obj, Throwable throwable) {
        wString(obj.toString(), Log.getStackTraceString(throwable));

        if (!debuggable) {
            forceException(obj, throwable);
        }

        if (fileLog) {
            FileLog.e(obj.toString(), throwable);
        }
    }

    /**
     * Print stack trace even if log is disabled.
     */
    private static void forceException(Object obj, Throwable throwable) {
        System.err.println(obj.toString());
        System.err.println(getStackTrace(throwable));
    }

    private static String getStackTrace(Throwable throwable) {
        final StringWriter result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
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

