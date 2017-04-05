package com.xabber.android.data.connection;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.log.LogManager;

public class WakeLockManager {
    private static final String LOG_TAG = WakeLockManager.class.getSimpleName();

    private static final WifiManager.WifiLock WIFI_LOCK;
    private static final PowerManager.WakeLock WAKE_LOCK;


    static {
        WIFI_LOCK = ((WifiManager) Application.getInstance().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "Xabber Wifi Lock");
        WIFI_LOCK.setReferenceCounted(false);

        WAKE_LOCK = ((PowerManager) Application.getInstance()
                .getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Xabber Wake Lock");
        WAKE_LOCK.setReferenceCounted(false);
    }

    public static void onWifiLockSettingsChanged() {
        if (SettingsManager.connectionWifiLock()) {
            LogManager.i(LOG_TAG, "Acquire wi-fi lock");
            WIFI_LOCK.acquire();
        } else {
            LogManager.i(LOG_TAG, "Release wi-fi lock");
            WIFI_LOCK.release();
        }
    }

    public static void onWakeLockSettingsChanged() {
        if (SettingsManager.connectionWakeLock()) {
            LogManager.i(LOG_TAG, "Acquire global partial wake lock");
            WAKE_LOCK.acquire();
        } else {
            LogManager.i(LOG_TAG, "Acquire global partial wake lock");
            WAKE_LOCK.release();
        }
    }
}
