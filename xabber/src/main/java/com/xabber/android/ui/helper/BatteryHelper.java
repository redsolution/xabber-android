package com.xabber.android.ui.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;


public class BatteryHelper {
    private static final String LOG_TAG = BatteryHelper.class.getSimpleName();

    public static boolean isOptimizingBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = Application.getInstance().getApplicationContext();
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

    @SuppressLint("BatteryLife")
    public static void sendIgnoreButteryOptimizationIntent(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            Uri uri = Uri.parse("package:" + Application.getInstance().getPackageName());
            intent.setData(uri);

            try {
                activity.startActivityForResult(intent, 42);
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }
}
