package com.xabber.android.data.extension.avatar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;

import com.xabber.android.data.Application;

/**
 * Helper class to create shortcuts under Android >= 3.
 * 
 * @author alexander.ivanov
 * 
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
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		try {
			return (Integer) method.invoke(activityManager, (Object[]) null);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
