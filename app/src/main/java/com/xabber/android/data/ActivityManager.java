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
package com.xabber.android.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.widget.Toast;

import com.xabber.android.data.SettingsManager.InterfaceTheme;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.LoadActivity;
import com.xabber.android.ui.PreferenceEditor;
import com.xabber.androiddev.R;

/**
 * Activity stack manager.
 * 
 * @author alexander.ivanov
 * 
 */
public class ActivityManager implements OnUnloadListener {

	private static final String EXTRA_TASK_INDEX = "com.xabber.android.data.ActivityManager.EXTRA_TASK_INDEX";

	private static final boolean LOG = true;

	private final Application application;

	/**
	 * List of launched activities.
	 */
	private final ArrayList<Activity> activities;

	/**
	 * Next index of task.
	 */
	private int nextTaskIndex;

	/**
	 * Activity with index of it task.
	 */
	private final WeakHashMap<Activity, Integer> taskIndexes;

	/**
	 * Listener for errors.
	 */
	private OnErrorListener onErrorListener;

	private final static ActivityManager instance;

	static {
		instance = new ActivityManager();
		Application.getInstance().addManager(instance);
	}

	public static ActivityManager getInstance() {
		return instance;
	}

	private ActivityManager() {
		this.application = Application.getInstance();
		activities = new ArrayList<Activity>();
		nextTaskIndex = 0;
		taskIndexes = new WeakHashMap<Activity, Integer>();
	}

	/**
	 * Removes finished activities from stask.
	 */
	private void rebuildStack() {
		Iterator<Activity> iterator = activities.iterator();
		while (iterator.hasNext())
			if (iterator.next().isFinishing())
				iterator.remove();
	}

	/**
	 * Finish all activities in stack till the root contact list.
	 * 
	 * @param finishRoot
	 *            also finish root contact list.
	 */
	public void clearStack(boolean finishRoot) {
		ContactList root = null;
		rebuildStack();
		for (Activity activity : activities) {
			if (!finishRoot && root == null && activity instanceof ContactList)
				root = (ContactList) activity;
			else
				activity.finish();
		}
		rebuildStack();
	}

	/**
	 * @return Whether contact list is in the activity stack.
	 */
	public boolean hasContactList(Context context) {
		rebuildStack();
		for (Activity activity : activities)
			if (activity instanceof ContactList)
				return true;
		return false;
	}

	/**
	 * Apply theme settings.
	 * 
	 * @param activity
	 */
	private void applyTheme(Activity activity) {
		if (activity instanceof PreferenceEditor)
			return;
		TypedArray title = activity.getTheme().obtainStyledAttributes(
				new int[] { android.R.attr.windowNoTitle,
						android.R.attr.windowIsFloating });
		boolean noTitle = title.getBoolean(0, false);
		boolean isFloating = title.getBoolean(1, false);
		title.recycle();
		if (isFloating)
			return;
		InterfaceTheme theme = SettingsManager.interfaceTheme();
		if (theme == SettingsManager.InterfaceTheme.light)
			activity.setTheme(noTitle ? R.style.Theme_Light_NoTitleBar
					: R.style.Theme_Light);
		else if (theme == SettingsManager.InterfaceTheme.dark)
			activity.setTheme(noTitle ? R.style.Theme_Dark_NoTitleBar
					: R.style.Theme_Dark);
	}

	/**
	 * Push activity to stack.
	 * 
	 * Must be called from {@link Activity#onCreate(Bundle)}.
	 * 
	 * @param activity
	 */
	public void onCreate(Activity activity) {
		if (LOG)
			LogManager.i(activity, "onCreate: " + activity.getIntent());
		applyTheme(activity);
		if (application.isClosing() && !(activity instanceof LoadActivity)) {
			activity.startActivity(LoadActivity.createIntent(activity));
			activity.finish();
		}
		activities.add(activity);
		rebuildStack();
		fetchTaskIndex(activity, activity.getIntent());
	}

	/**
	 * Pop activity from stack.
	 * 
	 * Must be called from {@link Activity#onDestroy()}.
	 * 
	 * @param activity
	 */
	public void onDestroy(Activity activity) {
		if (LOG)
			LogManager.i(activity, "onDestroy");
		activities.remove(activity);
	}

	/**
	 * Pause activity.
	 * 
	 * Must be called from {@link Activity#onPause()}.
	 * 
	 * @param activity
	 */
	public void onPause(Activity activity) {
		if (LOG)
			LogManager.i(activity, "onPause");
		if (onErrorListener != null)
			application
					.removeUIListener(OnErrorListener.class, onErrorListener);
		onErrorListener = null;
	}

	/**
	 * Resume activity.
	 * 
	 * Must be called from {@link Activity#onResume()}.
	 * 
	 * @param activity
	 */
	public void onResume(final Activity activity) {
		if (LOG)
			LogManager.i(activity, "onResume");
		if (!application.isInitialized() && !(activity instanceof LoadActivity)) {
			if (LOG)
				LogManager.i(this, "Wait for loading");
			activity.startActivity(LoadActivity.createIntent(activity));
		}
		if (onErrorListener != null)
			application
					.removeUIListener(OnErrorListener.class, onErrorListener);
		onErrorListener = new OnErrorListener() {
			@Override
			public void onError(final int resourceId) {
				Toast.makeText(activity, activity.getString(resourceId),
						Toast.LENGTH_LONG).show();
			}
		};
		application.addUIListener(OnErrorListener.class, onErrorListener);
	}

	/**
	 * New intent received.
	 * 
	 * Must be called from {@link Activity#onNewIntent(Intent)}.
	 * 
	 * @param activity
	 * @param intent
	 */
	public void onNewIntent(Activity activity, Intent intent) {
		if (LOG)
			LogManager.i(activity, "onNewIntent: " + intent);
	}

	/**
	 * Result has been received.
	 * 
	 * Must be called from {@link Activity#onActivityResult(int, int, Intent)}.
	 * 
	 * @param activity
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public void onActivityResult(Activity activity, int requestCode,
			int resultCode, Intent data) {
		if (LOG)
			LogManager.i(activity, "onActivityResult: " + requestCode + ", "
					+ resultCode + ", " + data);
	}

	/**
	 * Adds task index to the intent if specified for the source activity.
	 * 
	 * Must be used when source activity starts new own activity from
	 * {@link Activity#startActivity(Intent)} and
	 * {@link Activity#startActivityForResult(Intent, int)}.
	 * 
	 * @param source
	 * @param intent
	 */
	public void updateIntent(Activity source, Intent intent) {
		Integer index = taskIndexes.get(source);
		if (index == null)
			return;
		intent.putExtra(EXTRA_TASK_INDEX, index);
	}

	/**
	 * Mark activity to be in separate activity stack.
	 * 
	 * @param activity
	 */
	public void startNewTask(Activity activity) {
		taskIndexes.put(activity, nextTaskIndex);
		LogManager.i(activity, "Start new task " + nextTaskIndex);
		nextTaskIndex += 1;
	}

	/**
	 * Either move main task to back, either close all activities in subtask.
	 * 
	 * @param activity
	 */
	public void cancelTask(Activity activity) {
		Integer index = taskIndexes.get(activity);
		LogManager.i(activity, "Cancel task " + index);
		if (index == null) {
			activity.moveTaskToBack(true);
		} else {
			for (Entry<Activity, Integer> entry : taskIndexes.entrySet())
				if (entry.getValue() == index)
					entry.getKey().finish();
		}
	}

	/**
	 * Fetch task index from the intent and mark specified activity.
	 * 
	 * @param activity
	 * @param intent
	 */
	private void fetchTaskIndex(Activity activity, Intent intent) {
		int index = intent.getIntExtra(EXTRA_TASK_INDEX, -1);
		if (index == -1)
			return;
		LogManager.i(activity, "Fetch task index " + index);
		taskIndexes.put(activity, index);
	}

	@Override
	public void onUnload() {
		clearStack(true);
	}

}
