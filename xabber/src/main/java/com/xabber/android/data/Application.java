/*
  Copyright (c) 2013, Redsolution LTD. All rights reserved.
  <p>
  This file is part of Xabber project; you can redistribute it and/or
  modify it under the terms of the GNU General Public License, Version 3.
  <p>
  Xabber is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.
  <p>
  You should have received a copy of the GNU General Public License,
  along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import com.frogermcs.androiddevmetrics.AndroidDevMetrics;
import com.github.moduth.blockcanary.BlockCanary;
import com.squareup.leakcanary.LeakCanary;
import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.connection.ReconnectionManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.avatar.AvatarStorage;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.chat_markers.ChatMarkerManager;
import com.xabber.android.data.extension.chat_state.ChatStateManager;
import com.xabber.android.data.extension.delivery.DeliveryManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.groups.GroupInviteManager;
import com.xabber.android.data.extension.groups.GroupMemberManager;
import com.xabber.android.data.extension.groups.GroupsManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.retract.RetractManager;
import com.xabber.android.data.extension.ssn.SSNManager;
import com.xabber.android.data.extension.sync.SyncManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.http.PatreonManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.ReceiptManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.notification.DelayedNotificationActionManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.push.PushManager;
import com.xabber.android.data.roster.CircleManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XMPPAuthManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.BaseUIListener;
import com.xabber.android.ui.OnErrorListener;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.AppBlockCanaryContext;

import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Base entry point.
 *
 * @author alexander.ivanov
 */
public class Application extends android.app.Application {

    private static final String LOG_TAG = Application.class.getSimpleName();
    private static final ThreadFactory backgroundThreadFactory = r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        return thread;
    };
    private static Application instance;
    private final ArrayList<Object> registeredManagers;
    /**
     * Thread to execute tasks in background..
     */
    private final ExecutorService backgroundExecutor;
    //private static ThreadPoolExecutor fallbackNetworkExecutor;
    private final ExecutorService backgroundNetworkExecutor;
    private final ExecutorService backgroundExecutorForUserActions;
    private final ExecutorService backgroundNetworkExecutorForUserActions;
    /**
     * Handler to execute runnable in UI thread.
     */
    private final Handler handler;
    /**
     * Unmodifiable collections of managers that implement some common
     * interface.
     */
    private final Map<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>> managerInterfaces;
    private final Map<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>> uiListeners;
    /**
     * Where data load was requested.
     */
    private boolean serviceStarted;
    /**
     * Whether application was initialized.
     */
    private boolean initialized;
    /**
     * Whether user was notified about some action in contact list activity
     * after application initialization.
     */
    private boolean notified;
    /**
     * Whether application is to be closed.
     */
    private boolean closing;
    /**
     * Whether {@link #onServiceDestroy()} has been called.
     */
    private boolean closed;

    private final Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            for (OnTimerListener listener : getManagers(OnTimerListener.class)) {
                listener.onTimer();
            }
            if (!closing) {
                startTimer();
            }
        }

    };

    /**
     * Future for loading process.
     */
    private Future<Void> loadFuture;

    public Application() {
        instance = this;
        serviceStarted = false;
        initialized = false;
        notified = false;
        closing = false;
        closed = false;
        uiListeners = new HashMap<>();
        managerInterfaces = new HashMap<>();
        registeredManagers = new ArrayList<>();

        handler = new Handler();
        backgroundExecutor = createSingleThreadExecutor("Background executor service");
        backgroundNetworkExecutor = createSingleThreadExecutor("Background network executor service");
        backgroundExecutorForUserActions = createMultiThreadFixedPoolExecutor();
        backgroundNetworkExecutorForUserActions = createMultiThreadFixedPoolExecutor();
    }

    public static Application getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @NonNull
    private ExecutorService createSingleThreadExecutor(final String threadName) {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });
    }

    /*private ExecutorService createMultiThreadCacheExecutor() {
        return new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                backgroundThreadFactory);
    }

    private ExecutorService createFlexibleCacheExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1, 20,
                30L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                backgroundThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(onExecutionReject);
        return threadPoolExecutor;
    }

    private ThreadPoolExecutor createFallbackExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5, 5,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                backgroundThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    private ExecutorService createFixedThreadPoolWithTimeout() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) createMultiThreadFixedPoolExecutor();
        executor.setKeepAliveTime(60L, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    } */

    private ExecutorService createMultiThreadFixedPoolExecutor() {
        return Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                backgroundThreadFactory);
    }

    /**
     * Whether application is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    private void onLoad() {
        ProviderManager.addLoader(new ProviderFileLoader(getResources().openRawResource(R.raw.smack)));

        for (OnLoadListener listener : getManagers(OnLoadListener.class)) {
            LogManager.i(listener, "onLoad");
            listener.onLoad();
        }
    }

    private void onInitialized() {
        for (OnInitializedListener listener : getManagers(OnInitializedListener.class)) {
            LogManager.i(listener, "onInitialized");
            listener.onInitialized();
        }
        initialized = true;
        XabberService.getInstance().changeForeground();
        startTimer();
    }

    private void onClose() {
        LogManager.i(LOG_TAG, "onClose1");
        for (Object manager : registeredManagers) {
            if (manager instanceof OnCloseListener) {
                ((OnCloseListener) manager).onClose();
            }
        }
        closed = true;
        LogManager.i(LOG_TAG, "onClose2");
    }

    void onUnload() {
        LogManager.i(LOG_TAG, "onUnload1");
        for (Object manager : registeredManagers) {
            if (manager instanceof OnUnloadListener) {
                ((OnUnloadListener) manager).onUnload();
            }
        }
        LogManager.i(LOG_TAG, "onUnload2");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * @return <code>true</code> only once per application life. Subsequent
     * calls will always returns <code>false</code>.
     */
    public boolean doNotify() {
        if (notified) {
            return false;
        }
        notified = true;
        return true;
    }

    /**
     * Starts data loading in background if not started yet.
     */
    public void onServiceStarted() {
        if (serviceStarted) {
            return;
        }
        serviceStarted = true;
        LogManager.i(this, "onStart");
        loadFuture = backgroundExecutor.submit(() -> {
            try {
                onLoad();
            } finally {
                runOnUiThread(() -> {
                    // Throw exceptions in UI thread if any.
                    try {
                        loadFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    onInitialized();
                });
            }
            return null;
        });
    }

    /**
     * Requests to close application in some time in future.
     */
    public void requestToClose() {
        LogManager.i(LOG_TAG, "requestToClose1");
        closing = true;
        stopService(XabberService.createIntent(this));
        LogManager.i(LOG_TAG, "requestToClose2");
    }

    /**
     * @return Whether application is to be closed.
     */
    public boolean isClosing() {
        return closing;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            /* Leak Canary */
            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return;
            }
            LeakCanary.install(this);

            /* Block Canary */
            BlockCanary.install(this, new AppBlockCanaryContext()).start();

            /* Android Dev Metrics */
            AndroidDevMetrics.initWith(this);

            /* Strict Mode */
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }

//        new ANRWatchDog()
//                .setANRListener(error -> LogManager.exception("ANR Detected!", error))
//                .start();

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        addManagers();
        LogManager.i(this, "onCreate finished...");
    }

    private void addManagers() {
        addManager(SettingsManager.getInstance());
        addManager(LogManager.getInstance());
        addManager(DatabaseManager.getInstance());
        addManager(ConnectionManager.getInstance());
        addManager(AccountManager.getInstance());
        addManager(XabberAccountManager.getInstance());
        addManager(MessageManager.getInstance());
        addManager(ChatManager.getInstance());
        addManager(VCardManager.getInstance());
        addManager(ColorManager.getInstance());
        addManager(AvatarStorage.getInstance());
        addManager(AvatarManager.getInstance());
        addManager(PresenceManager.INSTANCE);
        addManager(RosterManager.getInstance());
        addManager(OTRManager.getInstance());
        addManager(CircleManager.getInstance());
        addManager(PhraseManager.getInstance());
        addManager(NotificationManager.getInstance());
        addManager(CustomNotifyPrefsManager.getInstance());
        addManager(ActivityManager.getInstance());
        addManager(CapabilitiesManager.getInstance());
        addManager(ChatStateManager.getInstance());
        addManager(NetworkManager.getInstance());
        addManager(ReconnectionManager.getInstance());
        addManager(ReceiptManager.getInstance());
        addManager(ChatMarkerManager.INSTANCE);
        addManager(SSNManager.getInstance());
        addManager(AttentionManager.getInstance());
        addManager(CarbonManager.INSTANCE);
        addManager(HttpFileUploadManager.getInstance());
        addManager(BlockingManager.getInstance());
        addManager(MessageArchiveManager.INSTANCE);
        addManager(CertificateManager.getInstance());
        addManager(XMPPAuthManager.getInstance());
        addManager(PushManager.getInstance());
        addManager(DelayedNotificationActionManager.getInstance());
        addManager(LastActivityInteractor.getInstance());
        addManager(XTokenManager.getInstance());
        addManager(GroupsManager.INSTANCE);
        addManager(GroupMemberManager.INSTANCE);
        addManager(RetractManager.getInstance());
        addManager(DeliveryManager.getInstance());
        addManager(PatreonManager.getInstance());
        addManager(GroupInviteManager.INSTANCE);
        addManager(SyncManager.INSTANCE);
    }

    /**
     * Register new manager.
     */
    private void addManager(Object manager) {
        registeredManagers.add(manager);
    }

    @Override
    public void onLowMemory() {
        LogManager.e(LOG_TAG, "Warning! Low memory!");
        for (OnLowMemoryListener listener : getManagers(OnLowMemoryListener.class)) {
            listener.onLowMemory();
        }
        super.onLowMemory();
    }

    /**
     * Service have been destroyed.
     */
    public void onServiceDestroy() {
        LogManager.i(LOG_TAG, "onServiceDestroy");

        if (closed) {
            LogManager.i(LOG_TAG, "onServiceDestroy closed");
            return;
        }
        onClose();

        // use new thread instead of run in background to exit immediately
        // without waiting for possible other threads in executor
        Thread thread = new Thread(this::onUnload);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void onTerminate() {
        requestToClose();
        super.onTerminate();
    }

    /**
     * Start periodically callbacks.
     */
    private void startTimer() {
        runOnUiThreadDelay(timerRunnable, OnTimerListener.DELAY);
    }

    /**
     * @param cls Requested class of managers.
     * @return List of registered manager.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseManagerInterface> Collection<T> getManagers(Class<T> cls) {
        if (closed) {
            return Collections.emptyList();
        }
        Collection<T> collection = (Collection<T>) managerInterfaces.get(cls);
        if (collection == null) {
            collection = new ArrayList<>();
            for (Object manager : registeredManagers) {
                if (cls.isInstance(manager)) {
                    collection.add((T) manager);
                }
            }
            collection = Collections.unmodifiableCollection(collection);
            managerInterfaces.put(cls, collection);
        }
        return collection;
    }

    /**
     * Request to clear application data.
     */
    public void requestToClear() {
        runInBackground(this::clear);
    }

    private void clear() {
        for (Object manager : registeredManagers) {
            if (manager instanceof OnClearListener) {
                ((OnClearListener) manager).onClear();
            }
        }
    }

    /**
     * Request to wipe all sensitive application data.
     */
    public void requestToWipe() {
        runInBackground(() -> {
            clear();
            for (Object manager : registeredManagers)
                if (manager instanceof OnWipeListener)
                    ((OnWipeListener) manager).onWipe();
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseUIListener> Collection<T> getOrCreateUIListeners(Class<T> cls) {
        Collection<T> collection = (Collection<T>) uiListeners.get(cls);
        if (collection == null) {
            collection = new ArrayList<>();
            uiListeners.put(cls, collection);
        }
        return collection;
    }

    /**
     * @param cls Requested class of listeners.
     * @return List of registered UI listeners.
     */
    public <T extends BaseUIListener> Collection<T> getUIListeners(Class<T> cls) {
        if (closed) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(getOrCreateUIListeners(cls));
    }

    /**
     * Register new listener.
     * <p/>
     * Should be called from {@link Activity@onResume()}.
     */
    public <T extends BaseUIListener> void addUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).add(listener);
    }

    /**
     * Unregister listener.
     * <p/>
     * Should be called from {@link Activity@onPause()}.
     */
    public <T extends BaseUIListener> void removeUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).remove(listener);
    }

    /**
     * Notify about error.
     */
    public void onError(final int resourceId) {
        runOnUiThread(() -> {
            for (OnErrorListener onErrorListener : getUIListeners(OnErrorListener.class)) {
                onErrorListener.onError(resourceId);
            }
        });
    }

    /**
     * Notify about error.
     */
    public void onError(NetworkException networkException) {
        LogManager.exception(this, networkException);
        onError(networkException.getResourceId());
    }

    public void runInBackground(final Runnable runnable) {
        // long start = System.currentTimeMillis();
        // Throwable testThrowable = new Throwable();
        backgroundExecutor.submit(() -> {
            try {
                // long qTime = System.currentTimeMillis() - start;
                // LogManager.d("BackgroundTest/bg", "time in q = " + qTime + " ms");
                runnable.run();
                // long taskTime = System.currentTimeMillis() - start - qTime;
                // LogManager.d("BackgroundTest/bg", "time on run() = " + taskTime + " ms");
                // if (taskTime > backgroundTaskTimeout) {
                //     LogManager.d("BackgroundTest/bg_TASK-TOO-LONG", taskTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // } else if (qTime > backgroundTaskTimeout){
                //     LogManager.d("BackgroundTest/bg_QUEUE-TOO-LONG", qTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // }
            } catch (Exception e) {
                LogManager.exception(runnable, e);
            }
        });
    }

    public void runInBackgroundNetwork(final Runnable runnable) {
        // long start = System.currentTimeMillis();
        // Throwable testThrowable = new Throwable();
        backgroundNetworkExecutor.submit(() -> {
            try {
                // long qTime = System.currentTimeMillis() - start;
                // LogManager.d("BackgroundTest/Net", "time in q = " + qTime + " ms");
                runnable.run();
                // long taskTime = System.currentTimeMillis() - start - qTime;
                // LogManager.d("BackgroundTest/Net", "time on run() = " + taskTime + " ms");
                // if (taskTime > backgroundTaskTimeout) {
                //     LogManager.d("BackgroundTest/Net_TASK-TOO-LONG", taskTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // } else if (qTime > backgroundTaskTimeout){
                //     LogManager.d("BackgroundTest/Net_QUEUE-TOO-LONG", qTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // }
                // LogManager.d("BackgroundTest/Net_ExecutorInfo_completedTask", backgroundNetworkExecutor.toString());
            } catch (Exception e) {
                LogManager.exception(runnable, e);
            }
        });
        // LogManager.d("BackgroundTest/Net_ExecutorInfo_queuedTask", backgroundNetworkExecutor.toString());
    }

    public void runInBackgroundUserRequest(final Runnable runnable) {
        // long start = System.currentTimeMillis();
        // Throwable testThrowable = new Throwable();
        backgroundExecutorForUserActions.submit(() -> {
            try {
                // long qTime = System.currentTimeMillis() - start;
                // LogManager.d("BackgroundTest/UserAction", "time in q = " + qTime + " ms");
                runnable.run();
                // long taskTime = System.currentTimeMillis() - start - qTime;
                // LogManager.d("BackgroundTest/UserAction", "time on run() = " + taskTime + " ms");
                // if (taskTime > backgroundTaskTimeout) {
                //     LogManager.d("BackgroundTest/UserAction_TASK-TOO-LONG", taskTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // } else if (qTime > backgroundTaskTimeout){
                //     LogManager.d("BackgroundTest/UserAction_QUEUE-TOO-LONG", qTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // }
                // LogManager.d("BackgroundTest/UserAction_ExecutorInfo_completedTask", backgroundExecutorForUserActions.toString());
            } catch (Exception e) {
                LogManager.exception(runnable, e);
            }
        });
        // LogManager.d("BackgroundTest/UserAction_ExecutorInfo_queuedTask", backgroundExecutorForUserActions.toString());
    }

    public void runInBackgroundNetworkUserRequest(final Runnable runnable) {
        // long start = System.currentTimeMillis();
        // Throwable testThrowable = new Throwable();
        backgroundNetworkExecutorForUserActions.submit(() -> {
            try {
                // long qTime = System.currentTimeMillis() - start;
                // LogManager.d("BackgroundTest/Net-UserAction", "time in q = " + qTime + " ms");
                runnable.run();
                // long taskTime = System.currentTimeMillis() - start - qTime;
                // LogManager.d("BackgroundTest/Net-UserAction", "time on run() = " + taskTime + " ms");
                // if (taskTime > backgroundTaskTimeout) {
                //     LogManager.d("BackgroundTest/Net-UserAction_TASK-TOO-LONG", taskTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // } else if (qTime > backgroundTaskTimeout){
                //     LogManager.d("BackgroundTest/Net-UserAction_QUEUE-TOO-LONG", qTime + " ms\n" + Log.getStackTraceString(testThrowable));
                // }
                // LogManager.d("BackgroundTest/Net-UserAction_ExecutorInfo_completedTask", backgroundNetworkExecutorForUserActions.toString());
            } catch (Exception e) {
                LogManager.exception(runnable, e);
            }
        });
        // LogManager.d("BackgroundTest/Net-UserAction_ExecutorInfo_queuedTask", backgroundNetworkExecutorForUserActions.toString());
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThreadDelay(final Runnable runnable, long delayMillis) {
        handler.postDelayed(runnable, delayMillis);
    }

    public boolean isServiceNotStarted() {
        return !serviceStarted;
    }

    public String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return getString(R.string.application_title_full) + " " + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.exception(this, e);
        }
        return "";
    }

    public void resetApplication() {
        try {
            requestToWipe();
            //Deleting all user files
            File cacheDirectory = getCacheDir();
            if (cacheDirectory.getParent() != null) {
                File applicationDirectory = new File(cacheDirectory.getParent());
                if (applicationDirectory.exists() && applicationDirectory.list() != null) {
                    String[] fileNames = applicationDirectory.list();
                    for (String fileName : fileNames) {
                        if (!fileName.equals("lib")) {
                            FileManager.getInstance()
                                    .deleteFile(new File(applicationDirectory, fileName));
                        }
                    }
                }
            }

            //Deleting all custom prefs
            SettingsManager.resetCustomPrefs();

            //Restart app
            Context c = getApplicationContext();
            if (c != null) {
                PackageManager pm = c.getPackageManager();
                if (pm != null) {
                    Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                                mPendingIntent);
                        System.exit(0);
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.e(LOG_TAG, "Was not able to reset application");
        }
    }

}
