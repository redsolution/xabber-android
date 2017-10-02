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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.frogermcs.androiddevmetrics.AndroidDevMetrics;
import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ScreenManager;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.connection.ReconnectionManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.avatar.AvatarStorage;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.mam.MamManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.ssn.SSNManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.ReceiptManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.service.XabberService;

import io.fabric.sdk.android.Fabric;
import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
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
    private static Application instance;
    private final ArrayList<Object> registeredManagers;
    /**
     * Thread to execute tasks in background..
     */
    private final ExecutorService backgroundExecutor;
    private final ExecutorService backgroundExecutorForUserActions;
    /**
     * Handler to execute runnable in UI thread.
     */
    private final Handler handler;
    /**
     * Unmodifiable collections of managers that implement some common
     * interface.
     */
    private Map<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>> managerInterfaces;
    private Map<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>> uiListeners;
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
        backgroundExecutorForUserActions = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @NonNull
    private ExecutorService createSingleThreadExecutor(final String threadName) {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                Thread thread = new Thread(runnable, threadName);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
                }
            });
    }

    public static Application getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance;
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
        loadFuture = backgroundExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    onLoad();
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Throw exceptions in UI thread if any.
                            try {
                                loadFuture.get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                            onInitialized();
                        }
                    });
                }
                return null;
            }
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
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, crashlyticsKit);
        }

        if (BuildConfig.DEBUG) {
            AndroidDevMetrics.initWith(this);
        }

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        addManagers();

        DatabaseManager.getInstance().addTables();

        LogManager.i(this, "onCreate finished...");
    }

    private void addManagers() {
        addManager(SettingsManager.getInstance());
        addManager(LogManager.getInstance());
        addManager(DatabaseManager.getInstance());
        addManager(AvatarStorage.getInstance());
        addManager(OTRManager.getInstance());
        addManager(ConnectionManager.getInstance());
        addManager(ScreenManager.getInstance());
        addManager(AccountManager.getInstance());
        addManager(XabberAccountManager.getInstance());
        addManager(MUCManager.getInstance());
        addManager(MessageManager.getInstance());
        addManager(ChatManager.getInstance());
        addManager(VCardManager.getInstance());
        addManager(AvatarManager.getInstance());
        addManager(PresenceManager.getInstance());
        addManager(RosterManager.getInstance());
        addManager(GroupManager.getInstance());
        addManager(PhraseManager.getInstance());
        addManager(NotificationManager.getInstance());
        addManager(ActivityManager.getInstance());
        addManager(CapabilitiesManager.getInstance());
        addManager(ChatStateManager.getInstance());
        addManager(NetworkManager.getInstance());
        addManager(ReconnectionManager.getInstance());
        addManager(ReceiptManager.getInstance());
        addManager(SSNManager.getInstance());
        addManager(AttentionManager.getInstance());
        addManager(CarbonManager.getInstance());
        addManager(HttpFileUploadManager.getInstance());
        addManager(BlockingManager.getInstance());
        addManager(MamManager.getInstance());
        addManager(CertificateManager.getInstance());
    }

    /**
     * Register new manager.
     */
    private void addManager(Object manager) {
        registeredManagers.add(manager);
    }

    @Override
    public void onLowMemory() {
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
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                onUnload();
            }
        });
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
        runInBackground(new Runnable() {
            @Override
            public void run() {
                clear();
            }
        });
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
        runInBackground(new Runnable() {
            @Override
            public void run() {
                clear();
                for (Object manager : registeredManagers)
                    if (manager instanceof OnWipeListener)
                        ((OnWipeListener) manager).onWipe();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseUIListener> Collection<T> getOrCreateUIListeners(Class<T> cls) {
        Collection<T> collection = (Collection<T>) uiListeners.get(cls);
        if (collection == null) {
            collection = new ArrayList<T>();
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
     * Should be called from {@link Activity#onResume()}.
     */
    public <T extends BaseUIListener> void addUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).add(listener);
    }

    /**
     * Unregister listener.
     * <p/>
     * Should be called from {@link Activity#onPause()}.
     */
    public <T extends BaseUIListener> void removeUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).remove(listener);
    }

    /**
     * Notify about error.
     */
    public void onError(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnErrorListener onErrorListener : getUIListeners(OnErrorListener.class)) {
                    onErrorListener.onError(resourceId);
                }
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

    /**
     * Submits request to be executed in background.
     */
    public void runInBackground(final Runnable runnable) {
        backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LogManager.exception(runnable, e);
                }
            }
        });
    }

    public void runInBackgroundUserRequest(final Runnable runnable) {
        backgroundExecutorForUserActions.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LogManager.exception(runnable, e);
                }
            }
        });
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

}
