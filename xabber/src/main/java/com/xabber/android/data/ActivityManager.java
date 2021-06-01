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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.push.SyncManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.activity.AboutActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.activity.LoadActivity;
import com.xabber.android.ui.activity.TutorialActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Activity stack manager.
 *
 * @author alexander.ivanov
 */
public class ActivityManager implements OnUnloadListener {

    private static final String EXTRA_TASK_INDEX = "com.xabber.android.data.ActivityManager.EXTRA_TASK_INDEX";
    private static final long START_SERVICE_DELAY = 1000;

    private static final boolean LOG = true;
    private static ActivityManager instance;

    private final Application application;
    /**
     * List of launched activities.
     */
    private final ArrayList<Activity> activities;
    /**
     * Activity with index of it task.
     */
    private final WeakHashMap<Activity, Integer> taskIndexes;
    /**
     * Next index of task.
     */
    private int nextTaskIndex;
    /**
     * Listener for errors.
     */
    private OnErrorListener onErrorListener;

    public static ActivityManager getInstance() {
        if (instance == null) {
            instance = new ActivityManager();
        }

        return instance;
    }

    private ActivityManager() {
        this.application = Application.getInstance();
        activities = new ArrayList<>();
        nextTaskIndex = 0;
        taskIndexes = new WeakHashMap<>();
    }

    /**
     * Removes finished activities from stask.
     */
    private void rebuildStack() {
        Iterator<Activity> iterator = activities.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isFinishing()) {
                iterator.remove();
            }
        }
    }

    /**
     * Finish all activities in stack till the root contact list.
     *
     * @param finishRoot also finish root contact list.
     */
    public void clearStack(boolean finishRoot) {
        MainActivity root = null;
        rebuildStack();
        for (Activity activity : activities) {
            if (!finishRoot && root == null && activity instanceof MainActivity) {
                root = (MainActivity) activity;
            } else {
                activity.finish();
            }
        }
        rebuildStack();
    }

    /**
     * @return Whether contact list is in the activity stack.
     */
    public boolean hasContactList(Context context) {
        rebuildStack();
        for (Activity activity : activities)
            if (activity instanceof MainActivity)
                return true;
        return false;
    }

    /**
     * Apply theme settings.
     *
     * @param activity
     */
    private void applyTheme(Activity activity) {
        activity.setTheme(R.style.Theme);
        SettingsManager.InterfaceTheme theme = SettingsManager.interfaceTheme();
        if (theme.equals(SettingsManager.InterfaceTheme.dark)) {
            activity.setTheme(R.style.ThemeDark);
        } else {
            activity.setTheme(R.style.Theme);
        }
    }

    /**
     * Push activity to stack.
     * <p/>
     * Must be called from {@link Activity#onCreate(Bundle)}.
     *
     * @param activity
     */
    public void onCreate(Activity activity) {
        if (LOG) {
            LogManager.i(activity, "onCreate: " + activity.getIntent());
        }
        if (!(activity instanceof AboutActivity) && !(activity instanceof TutorialActivity)) {
            applyTheme(activity);
        }
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
     * <p/>
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
     * <p/>
     * Must be called from {@link Activity#onPause()}.
     *
     * @param activity
     */
    public void onPause(Activity activity) {
        if (LOG)
            LogManager.i(activity, "onPause");

        CertificateManager.getInstance().unregisterActivity(activity);

        if (onErrorListener != null)
            application
                    .removeUIListener(OnErrorListener.class, onErrorListener);
        onErrorListener = null;
    }

    /**
     * Resume activity.
     * <p/>
     * Must be called from {@link Activity#onResume()}.
     *
     * @param activity
     */
    public void onResume(final Activity activity) {
        if (LOG) {
            LogManager.i(activity, "onResume");
        }
        if((!application.isInitialized() || SyncManager.getInstance().isSyncMode())
                && !Application.getInstance().isClosing()) {

            if (LOG) {
                LogManager.i(this, "Wait for loading");
            }
            AccountManager.getInstance().onPreInitialize();
            RosterManager.getInstance().onPreInitialize();
            // TODO
            //  check if we need to do the convoluted
            //  backgroundThread -> sleep -> uiThread before starting service
            //  or if simple uiThreadDelay is sufficient
            Application.getInstance().runOnUiThreadDelay(() ->
                    activity.startService(XabberService.createIntent(activity)),
                    START_SERVICE_DELAY);
            // Application.getInstance().runInBackground(new Runnable() {
            //     @Override
            //     public void run() {
            //         try {
            //             Thread.sleep(START_SERVICE_DELAY);
            //         } catch (InterruptedException e) {
            //             e.printStackTrace();
            //         }
            //         Application.getInstance().runOnUiThread(new Runnable() {
            //             @Override
            //             public void run() {
            //                 activity.startService(XabberService.createIntent(activity));
            //             }
            //         });
            //     }
            // });
        }
        if (onErrorListener != null) {
            application.removeUIListener(OnErrorListener.class, onErrorListener);
        }
        onErrorListener = new OnErrorListener() {
            @Override
            public void onError(final int resourceId) {
                Toast.makeText(activity, activity.getString(resourceId),
                        Toast.LENGTH_LONG).show();
            }
        };
        application.addUIListener(OnErrorListener.class, onErrorListener);

        CertificateManager.getInstance().registerActivity(activity);
        AccountManager.getInstance().stopGracePeriod();
        SyncManager.getInstance().onActivityResume();
    }

    /**
     * New intent received.
     * <p/>
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
     * <p/>
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
     * <p/>
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
            for (Entry<Activity, Integer> entry : taskIndexes.entrySet()) {
                if (entry.getValue().equals(index)) {
                    entry.getKey().finish();
                }
            }
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
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clearStack(true);
            }
        });
    }

}
