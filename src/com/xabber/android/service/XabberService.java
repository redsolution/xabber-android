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
package com.xabber.android.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.notification.NotificationManager;

/**
 * Basic service to work in background.
 * 
 * @author alexander.ivanov
 * 
 */
public class XabberService extends Service {

	private Method startForeground;
	private Method stopForeground;

	private static XabberService instance;

	public static XabberService getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		LogManager.i(this, "onCreate");

		// Try to get methods supported in API Level 5+
		try {
			startForeground = getClass().getMethod("startForeground",
					new Class[] { int.class, Notification.class });
			stopForeground = getClass().getMethod("stopForeground",
					new Class[] { boolean.class });
		} catch (NoSuchMethodException e) {
			startForeground = stopForeground = null;
		}

		changeForeground();
	}

	public void changeForeground() {
		if (SettingsManager.eventsPersistent()
				&& Application.getInstance().isInitialized())
			startForegroundWrapper(NotificationManager.getInstance()
					.getPersistentNotification());
		else
			stopForegroundWrapper();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Application.getInstance().onServiceStarted();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LogManager.i(this, "onDestroy");
		stopForegroundWrapper();
		Application.getInstance().onServiceDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundWrapper(Notification notification) {
		if (startForeground != null) {
			Object[] startForegroundArgs = new Object[] {
					Integer.valueOf(NotificationManager.PERSISTENT_NOTIFICATION_ID),
					notification };
			try {
				startForeground.invoke(this, startForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				LogManager.w(this, "Unable to invoke startForeground" + e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				LogManager.w(this, "Unable to invoke startForeground" + e);
			}
		} else {
			setForeground(true);
			try {
				((android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
						.notify(NotificationManager.PERSISTENT_NOTIFICATION_ID,
								notification);
			} catch (SecurityException e) {
			}
		}
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundWrapper() {
		if (stopForeground != null) {
			try {
				stopForeground.invoke(this, new Object[] { Boolean.TRUE });
				// We don't want to clear notification bar.
			} catch (InvocationTargetException e) {
				// Should not happen.
				LogManager.w(this, "Unable to invoke stopForeground" + e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				LogManager.w(this, "Unable to invoke stopForeground" + e);
			}
		} else {
			setForeground(false);
		}
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, XabberService.class);
	}

}
