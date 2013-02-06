package com.xabber.android.data.extension.avatar;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.xabber.android.data.Application;

/**
 * Helper class to create shortcuts under Android >= 2.3.
 * 
 * @author alexander.ivanov
 * 
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class GingerbreadShortcutHelper {

	/**
	 * Get the preferred launcher icon size. This is used when custom drawables
	 * are created (e.g., for shortcuts).
	 * 
	 * Based on {@link android.app.ActivityManager#getLauncherLargeIconSize()}
	 * for Android 3+.
	 * 
	 * @return dimensions of square icons in terms of pixels
	 */
	static int getLauncherLargeIconSize() {
		final Resources res = Application.getInstance().getResources();
		final int size = res
				.getDimensionPixelSize(android.R.dimen.app_icon_size);
		if ((res.getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) != Configuration.SCREENLAYOUT_SIZE_XLARGE) {
			return size;
		}
		final int density = res.getDisplayMetrics().densityDpi;

		switch (density) {
		case DisplayMetrics.DENSITY_LOW:
			return (size * DisplayMetrics.DENSITY_MEDIUM)
					/ DisplayMetrics.DENSITY_LOW;
		case DisplayMetrics.DENSITY_MEDIUM:
			return (size * DisplayMetrics.DENSITY_HIGH)
					/ DisplayMetrics.DENSITY_MEDIUM;
		case DisplayMetrics.DENSITY_HIGH:
			return (size * DisplayMetrics.DENSITY_XHIGH)
					/ DisplayMetrics.DENSITY_HIGH;
		case DisplayMetrics.DENSITY_XHIGH:
			return (size * DisplayMetrics.DENSITY_MEDIUM * 2)
					/ DisplayMetrics.DENSITY_XHIGH;
		default:
			return size;
		}
	}

}
