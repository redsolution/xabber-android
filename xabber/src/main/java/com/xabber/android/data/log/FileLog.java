/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.xabber.android.data.log;

import android.os.Build;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.time.FastDateFormat;
import com.xabber.android.ui.helper.BatteryHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Locale;

class FileLog {
    private final String REGULAR_LOG_FILE_NAME_PREFIX = Application.getInstance().getString(R.string.application_title_prefix).replaceAll("\\s+","") + "_ALL";
    private final String XMPP_LOG_FILE_NAME_PREFIX = Application.getInstance().getString(R.string.application_title_prefix).replaceAll("\\s+","") + "_XMPP";
    private FastDateFormat dateFormat;

    private OutputStreamWriter streamWriter = null;
    private DispatchQueue logQueue = null;
    private File currentFile = null;

    private OutputStreamWriter xmppStreamWriter = null;
    private DispatchQueue xmppLogQueue = null;
    private File xmppCurrentFile = null;

    private static final int LOG_FILE_MAX_SIZE = 8000000; // 8mb
    private static final int LOG_FILE_MAX_COUNT = 16;

    private static volatile FileLog Instance = null;

    public static FileLog getInstance() {
        FileLog localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLog.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLog();
                }
            }
        }
        return localInstance;
    }

    public FileLog() {
        dateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US);
        try {
            logQueue = new DispatchQueue("logQueue");
            currentFile = createLogFile();
            xmppLogQueue = new DispatchQueue("xmppLogQueue");
            xmppCurrentFile = createXmppLogFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createNewFilesIfNeed(){
        if (currentFile == null)
            createLogFile();

        if (xmppCurrentFile == null)
            createXmppLogFile();
    }

    private File createXmppLogFile(){
        File newLogFile = null;
        String appName = XMPP_LOG_FILE_NAME_PREFIX;
        try {
            File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
            if (sdCard == null) {
                return null;
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            newLogFile = new File(dir, appName + "_" + BuildConfig.VERSION_CODE
                    + "_" + dateFormat.format(System.currentTimeMillis()) + ".txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (xmppStreamWriter != null) {
                xmppStreamWriter.flush();
                xmppStreamWriter.close();
            }
            newLogFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(newLogFile);
            xmppStreamWriter = new OutputStreamWriter(stream);
            xmppStreamWriter.write("-----start log " + dateFormat.format(System.currentTimeMillis())
                    + " " + appName
                    + " " + BuildConfig.VERSION_NAME
                    + " Android " + Build.VERSION.RELEASE
                    + " SDK " + Build.VERSION.SDK_INT
                    + " Battery optimization: " + BatteryHelper.isOptimizingBattery()
                    + "-----\n");
            xmppStreamWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // delete log files if need
        deleteRedundantFiles();
        return newLogFile;
    }

    private File createLogFile() {
        File newLogFile = null;
        String appName = REGULAR_LOG_FILE_NAME_PREFIX;
        try {
            File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
            if (sdCard == null) {
                return null;
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            newLogFile = new File(dir, appName + "_" + BuildConfig.VERSION_CODE
                    + "_" + dateFormat.format(System.currentTimeMillis()) + ".txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (streamWriter != null) {
                streamWriter.flush();
                streamWriter.close();
            }
            if (!newLogFile.createNewFile())
                LogManager.exception(this.getClass().getSimpleName(), new Exception("Exception with new log file creating"));
            FileOutputStream stream = new FileOutputStream(newLogFile);
            streamWriter = new OutputStreamWriter(stream);
            streamWriter.write("-----start log " + dateFormat.format(System.currentTimeMillis())
                    + " " + appName
                    + " " + BuildConfig.VERSION_NAME
                    + " Android " + Build.VERSION.RELEASE
                    + " SDK " + Build.VERSION.SDK_INT
                    + " Battery optimization: " + BatteryHelper.isOptimizingBattery()
                    +  "-----\n");
            streamWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // delete log files if need
        deleteRedundantFiles();
        return newLogFile;
    }

    /*
        Controls list of log files. Each no more than 8 mb size.
     */
    private synchronized void controlFileSize() {
        if (currentFile != null)
            if (currentFile.length() >= LOG_FILE_MAX_SIZE) {
                    File newFile = createLogFile();
                    if (newFile != null)
                        currentFile = newFile;
            }

        if (xmppCurrentFile != null)
            if (xmppCurrentFile.length() >= LOG_FILE_MAX_SIZE) {
                File newFile = createXmppLogFile();
                if (newFile != null)
                    xmppCurrentFile = newFile;
            }
    }

    private void deleteRedundantFiles() {
        // delete old files if it's more than 6
        File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
        if (sdCard == null) {
            return;
        }
        File dir = new File(sdCard.getAbsolutePath() + "/logs");
        File[] files = dir.listFiles();

        if (files == null) return;

        if (files.length > LOG_FILE_MAX_COUNT) {
            Arrays.sort(files, (f1, f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
            for (int i = 0; i < files.length - LOG_FILE_MAX_COUNT; i++) {
                files[i].delete();
            }
        }
    }

    public static void e(final String tag, final String message, final Throwable exception) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + message + "\n");
                    getInstance().streamWriter.write(exception.toString());
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final String tag, final String message) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final String tag, final Throwable e) {
        if (tag.equals(SmackDebugger.LOG_TAG)){
            getInstance().controlFileSize();
            if (getInstance().xmppStreamWriter != null) {
                getInstance().xmppLogQueue.postRunnable(() -> {
                    try {
                        getInstance().xmppStreamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + e + "\n");
                        StackTraceElement[] stack = e.getStackTrace();
                        for (int a = 0; a < stack.length; a++) {
                            getInstance().xmppStreamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + stack[a] + "\n");
                        }
                        getInstance().xmppStreamWriter.flush();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });
            } else {
                e.printStackTrace();
            }
        }
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + e + "\n");
                    StackTraceElement[] stack = e.getStackTrace();
                    for (int a = 0; a < stack.length; a++) {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + stack[a] + "\n");
                    }
                    getInstance().streamWriter.flush();
                } catch (Exception e12) {
                    e12.printStackTrace();
                }
            });
        } else {
            e.printStackTrace();
        }

    }

    public static void d(final String tag, final String message) {
        if (tag.equals(SmackDebugger.LOG_TAG)){
            getInstance().controlFileSize();
            if (getInstance().xmppStreamWriter != null) {
                getInstance().xmppLogQueue.postRunnable(() -> {
                    try {
                        getInstance().xmppStreamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/" + tag + "﹕ " + message + "\n");
                        getInstance().xmppStreamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/" + tag + "﹕ " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public static void w(final String tag, final String message) {
        if (tag.equals(SmackDebugger.LOG_TAG)){
            getInstance().controlFileSize();
            if (getInstance().xmppStreamWriter != null) {
                getInstance().xmppLogQueue.postRunnable(() -> {
                    try {
                        getInstance().xmppStreamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/" + tag + ": " + message + "\n");
                        getInstance().xmppStreamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            getInstance().controlFileSize();
            if (getInstance().streamWriter != null) {
                getInstance().logQueue.postRunnable(() -> {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/" + tag + ": " + message + "\n");
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public static void cleanupLogs() {
        File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
        if (sdCard == null) {
            return;
        }
        File dir = new File(sdCard.getAbsolutePath() + "/logs");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int a = 0; a < files.length; a++) {
                File file = files[a];
                if (SettingsManager.fileLog() && getInstance().currentFile != null && file.getAbsolutePath().equals(getInstance().currentFile.getAbsolutePath())
                        || SettingsManager.fileLog() && getInstance().xmppCurrentFile != null && file.getAbsolutePath().equals(getInstance().xmppCurrentFile.getAbsolutePath())) {
                    continue;
                }
                file.delete();
            }
        }
    }
}
