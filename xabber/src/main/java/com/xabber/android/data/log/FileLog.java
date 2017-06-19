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
import com.xabber.android.data.time.FastDateFormat;
import com.xabber.android.ui.helper.BatteryHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

class FileLog {
    private OutputStreamWriter streamWriter = null;
    private FastDateFormat dateFormat = null;
    private DispatchQueue logQueue = null;
    private File currentFile = null;
    private File networkFile = null;

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
        dateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss", Locale.US);
        try {
            logQueue = new DispatchQueue("logQueue");
            currentFile = createLogFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createLogFile() {
        File newLogFile = null;
        String appName = Application.getInstance().getString(R.string.application_title_full).replaceAll("\\s+","");
        try {
            File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
            if (sdCard == null) {
                return null;
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            newLogFile = new File(dir, appName + "_" + BuildConfig.VERSION_NAME
                    + "_" + dateFormat.format(System.currentTimeMillis()) + ".txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (streamWriter != null) {
                streamWriter.flush();
                streamWriter.close();
            }
            newLogFile.createNewFile();
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
        Controls list of log files. Allow only 6 log-files. Each no more than 8 mb size.
     */
    private void controlFileSize() {
        // create new file if current file is too large
        if (currentFile != null) {
            if (currentFile.length() >= LOG_FILE_MAX_SIZE) {
                File newFile = createLogFile();
                if (newFile != null) {
                    currentFile = newFile;
                }
            }
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

        if (files != null && files.length > LOG_FILE_MAX_COUNT) {
            Arrays.sort(files, new Comparator<File>(){
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });
            for (int i = 0; i < files.length - LOG_FILE_MAX_COUNT; i++) {
                files[i].delete();
            }
        }
    }

    public static String getNetworkLogPath() {
        try {
            File sdCard = Application.getInstance().getApplicationContext().getExternalFilesDir(null);
            if (sdCard == null) {
                return "";
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            getInstance().networkFile = new File(dir, getInstance().dateFormat.format(System.currentTimeMillis()) + "_net.txt");
            return getInstance().networkFile.getAbsolutePath();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void e(final String tag, final String message, final Throwable exception) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + message + "\n");
                        getInstance().streamWriter.write(exception.toString());
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void e(final String tag, final String message) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + message + "\n");
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void e(final String tag, final Throwable e) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + e + "\n");
                        StackTraceElement[] stack = e.getStackTrace();
                        for (int a = 0; a < stack.length; a++) {
                            getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/" + tag + "﹕ " + stack[a] + "\n");
                        }
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            e.printStackTrace();
        }
    }

    public static void d(final String tag, final String message) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/" + tag + "﹕ " + message + "\n");
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void w(final String tag, final String message) {
        getInstance().controlFileSize();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/" + tag + ": " + message + "\n");
                        getInstance().streamWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                if (getInstance().currentFile != null && file.getAbsolutePath().equals(getInstance().currentFile.getAbsolutePath())) {
                    continue;
                }
                if (getInstance().networkFile != null && file.getAbsolutePath().equals(getInstance().networkFile.getAbsolutePath())) {
                    continue;
                }
                file.delete();
            }
        }
    }
}
