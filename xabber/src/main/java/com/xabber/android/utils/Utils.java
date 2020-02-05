package com.xabber.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Build;
import android.util.TypedValue;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.push.SyncManager;
import com.xabber.android.service.XabberService;

import java.util.Calendar;
import java.util.Date;

public class Utils {

    public static int dipToPx(float dip, Context context) {
        return (int) dipToPxFloat(dip, context);
    }

    public static float dipToPxFloat(float dip, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dip, context.getResources().getDisplayMetrics());
    }

    public static int spToPx(float sp, Context context) {
        return (int) spToPxFloat(sp, context);
    }

    public static float spToPxFloat(float sp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                sp, context.getResources().getDisplayMetrics());
    }

    public static int longToInt(long number) {
        if (number > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (number < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else return (int) number;
    }

    public static boolean isSameDay(Long date1, Long date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(new Date(date1));
        cal2.setTime(new Date(date2));
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    public static void startXabberServiceCompat(Context context) {
        startXabberServiceCompat(context, XabberService.createIntent(context));
    }

    public static void startXabberServiceCompatWithSyncMode(Context context, String pushNode) {
        startXabberServiceCompat(context,
                SyncManager.createXabberServiceIntentWithSyncMode(context, pushNode));
    }

    public static void startXabberServiceCompatWithSyncMode(Context context, AccountJid accountJid) {
        startXabberServiceCompat(context,
                SyncManager.createXabberServiceIntentWithSyncMode(context, accountJid));
    }

    private static void startXabberServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static String xmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;"); //$NON-NLS-1$
                    break;
                case '>':
                    sb.append("&gt;"); //$NON-NLS-1$
                    break;
                case '&':
                    sb.append("&amp;"); //$NON-NLS-1$
                    break;
                case '\'':
                    // In this implementation we use &apos; instead of &#39; because we encode XML, not HTML.
                    sb.append("&apos;"); //$NON-NLS-1$
                    break;
                case '"':
                    sb.append("&quot;"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void lockScreenRotation(Activity activity, boolean lockOrientation) {
        int lock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (lockOrientation) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();

            Point size = new Point();
            display.getSize(size);

            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                if (size.x > size.y) {
                    //rotation is 0 or 180 deg, and the size of x is greater than y,
                    //so we have a tablet
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    //rotation is 0 or 180 deg, and the size of y is greater than x,
                    //so we have a phone
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    }
                }
            } else {
                if (size.x > size.y) {
                    //rotation is 90 or 270, and the size of x is greater than y,
                    //so we have a phone
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    //rotation is 90 or 270, and the size of y is greater than x,
                    //so we have a tablet
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    }
                }
            }
        }
        activity.setRequestedOrientation(lock);
    }

    /**
     * Haptic feedback helper methods.
     *
     * */
    public static void performHapticFeedback(View view) {
        performHapticFeedback(view, HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public static void performHapticFeedback(View view, int feedbackType) {
        performHapticFeedback(view, feedbackType, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    public static void performHapticFeedback(View view, int feedbackType, int flag) {
        if (view != null)
            view.performHapticFeedback(feedbackType, flag);
    }
}
