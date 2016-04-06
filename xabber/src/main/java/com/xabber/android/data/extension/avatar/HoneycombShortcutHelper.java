package com.xabber.android.data.extension.avatar;

import android.content.Context;

import com.xabber.android.data.Application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper class to create shortcuts under Android >= 3.
 *
 * @author alexander.ivanov
 */
public class HoneycombShortcutHelper {

    /**
     * Get the preferred launcher icon size. This is used when custom drawables
     * are created (e.g., for shortcuts).
     *
     * @return dimensions of square icons in terms of pixels
     */
    static int getLauncherLargeIconSize() {
        android.app.ActivityManager activityManager = (android.app.ActivityManager) Application
                .getInstance().getSystemService(Context.ACTIVITY_SERVICE);
        Method method;
        try {
            method = activityManager.getClass().getMethod(
                    "getLauncherLargeIconSize", (Class[]) null);
        } catch (SecurityException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try {
            return (Integer) method.invoke(activityManager, (Object[]) null);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
