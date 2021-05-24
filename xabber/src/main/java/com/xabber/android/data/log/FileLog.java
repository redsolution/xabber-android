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
    private FastDateFormat dateFormat;

    private OutputStreamWriter streamWriter = null;
    private DispatchQueue logQueue = null;
    private File currentFile = null;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createNewFilesIfNeed(){
        if (currentFile == null)
            createLogFile();
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
                if (SettingsManager.fileLog() && getInstance().currentFile != null
                        && file.getAbsolutePath().equals(getInstance().currentFile.getAbsolutePath())) {
                    continue;
                }
                file.delete();
            }
        }
    }
}
