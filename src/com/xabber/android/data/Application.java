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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;

import com.xabber.android.service.XabberService;
import com.xabber.androiddev.R;

/**
 * Base entry point.
 * 
 * @author alexander.ivanov
 */
public class Application extends android.app.Application {

	public static final int SDK_INT = Integer.valueOf(Build.VERSION.SDK);

	private static Application instance;

	public static Application getInstance() {
		if (instance == null)
			throw new IllegalStateException();
		return instance;
	}

	private final ArrayList<Object> registeredManagers;

	/**
	 * Unmodifiable collections of managers that implement some common
	 * interface.
	 */
	private Map<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>> managerInterfaces;

	private Map<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>> uiListeners;

	/**
	 * Thread to execute tasks in background..
	 */
	private final ExecutorService backgroundExecutor;

	/**
	 * Handler to execute runnable in UI thread.
	 */
	private final Handler handler;

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

	/**
	 * Future for loading process.
	 */
	private Future<Void> loadFuture;

	private final Runnable timerRunnable = new Runnable() {

		@Override
		public void run() {
			for (OnTimerListener listener : getManagers(OnTimerListener.class))
				listener.onTimer();
			if (!closing)
				startTimer();
		}

	};

	public Application() {
		instance = this;
		serviceStarted = false;
		initialized = false;
		notified = false;
		closing = false;
		closed = false;
		uiListeners = new HashMap<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>>();
		managerInterfaces = new HashMap<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>>();
		registeredManagers = new ArrayList<Object>();

		handler = new Handler();
		backgroundExecutor = Executors
				.newSingleThreadExecutor(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable runnable) {
						Thread thread = new Thread(runnable,
								"Background executor service");
						thread.setPriority(Thread.MIN_PRIORITY);
						thread.setDaemon(true);
						return thread;
					}
				});
	}

	/**
	 * Whether application is initialized.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	private void onLoad() {
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
		LogManager.i(this, "onClose");
		for (Object manager : registeredManagers)
			if (manager instanceof OnCloseListener)
				((OnCloseListener) manager).onClose();
		closed = true;
	}

	private void onUnload() {
		LogManager.i(this, "onUnload");
		for (Object manager : registeredManagers)
			if (manager instanceof OnUnloadListener)
				((OnUnloadListener) manager).onUnload();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/**
	 * @return <code>true</code> only once per application life. Subsequent
	 *         calls will always returns <code>false</code>.
	 */
	public boolean doNotify() {
		if (notified)
			return false;
		notified = true;
		return true;
	}

	/**
	 * Starts data loading in background if not started yet.
	 * 
	 * @return
	 */
	public void onServiceStarted() {
		if (serviceStarted)
			return;
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
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							} catch (ExecutionException e) {
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
		closing = true;
		stopService(XabberService.createIntent(this));
	}

	/**
	 * @return Whether application is to be closed.
	 */
	public boolean isClosing() {
		return closing;
	}

	/**
	 * Returns whether system contact storage is supported.
	 * 
	 * Note:
	 * 
	 * Please remove *_CONTACTS, *_ACCOUNTS, *_SETTINGS permissions,
	 * SyncAdapterService and AccountAuthenticatorService together from manifest
	 * file.
	 * 
	 * @return
	 */
	public boolean isContactsSupported() {
		return SDK_INT >= 5
				&& checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		ArrayList<String> contactManager = new ArrayList<String>();
		TypedArray contactManagerClasses = getResources().obtainTypedArray(
				R.array.contact_managers);
		for (int index = 0; index < contactManagerClasses.length(); index++)
			contactManager.add(contactManagerClasses.getString(index));
		contactManagerClasses.recycle();

		TypedArray managerClasses = getResources().obtainTypedArray(
				R.array.managers);
		for (int index = 0; index < managerClasses.length(); index++)
			if (isContactsSupported()
					|| !contactManager
							.contains(managerClasses.getString(index)))
				try {
					Class.forName(managerClasses.getString(index));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
		managerClasses.recycle();

		TypedArray tableClasses = getResources().obtainTypedArray(
				R.array.tables);
		for (int index = 0; index < tableClasses.length(); index++)
			try {
				Class.forName(tableClasses.getString(index));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		tableClasses.recycle();
	}

	@Override
	public void onLowMemory() {
		for (OnLowMemoryListener listener : getManagers(OnLowMemoryListener.class))
			listener.onLowMemory();
		super.onLowMemory();
	}

	/**
	 * Service have been destroyed.
	 */
	public void onServiceDestroy() {
		if (closed)
			return;
		onClose();
		runInBackground(new Runnable() {
			@Override
			public void run() {
				onUnload();
			}
		});
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
	 * Register new manager.
	 * 
	 * @param manager
	 */
	public void addManager(Object manager) {
		registeredManagers.add(manager);
	}

	/**
	 * @param cls
	 *            Requested class of managers.
	 * @return List of registered manager.
	 */
	@SuppressWarnings("unchecked")
	public <T extends BaseManagerInterface> Collection<T> getManagers(
			Class<T> cls) {
		if (closed)
			return Collections.emptyList();
		Collection<T> collection = (Collection<T>) managerInterfaces.get(cls);
		if (collection == null) {
			collection = new ArrayList<T>();
			for (Object manager : registeredManagers)
				if (cls.isInstance(manager))
					collection.add((T) manager);
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
		for (Object manager : registeredManagers)
			if (manager instanceof OnClearListener)
				((OnClearListener) manager).onClear();
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
	private <T extends BaseUIListener> Collection<T> getOrCreateUIListeners(
			Class<T> cls) {
		Collection<T> collection = (Collection<T>) uiListeners.get(cls);
		if (collection == null) {
			collection = new ArrayList<T>();
			uiListeners.put(cls, collection);
		}
		return collection;
	}

	/**
	 * @param cls
	 *            Requested class of listeners.
	 * @return List of registered UI listeners.
	 */
	public <T extends BaseUIListener> Collection<T> getUIListeners(Class<T> cls) {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(getOrCreateUIListeners(cls));
	}

	/**
	 * Register new listener.
	 * 
	 * Should be called from {@link Activity#onResume()}.
	 * 
	 * @param cls
	 * @param listener
	 */
	public <T extends BaseUIListener> void addUIListener(Class<T> cls,
			T listener) {
		getOrCreateUIListeners(cls).add(listener);
	}

	/**
	 * Unregister listener.
	 * 
	 * Should be called from {@link Activity#onPause()}.
	 * 
	 * @param cls
	 * @param listener
	 */
	public <T extends BaseUIListener> void removeUIListener(Class<T> cls,
			T listener) {
		getOrCreateUIListeners(cls).remove(listener);
	}

	/**
	 * Notify about error.
	 * 
	 * @param resourceId
	 */
	public void onError(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (OnErrorListener onErrorListener : getUIListeners(OnErrorListener.class))
					onErrorListener.onError(resourceId);
			}
		});
	}

	/**
	 * Notify about error.
	 * 
	 * @param networkException
	 */
	public void onError(NetworkException networkException) {
		LogManager.exception(this, networkException);
		onError(networkException.getResourceId());
	}

	/**
	 * Submits request to be executed in background.
	 * 
	 * @param runnable
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

	/**
	 * Submits request to be executed in UI thread.
	 * 
	 * @param runnable
	 */
	public void runOnUiThread(final Runnable runnable) {
		handler.post(runnable);
	}

	/**
	 * Submits request to be executed in UI thread.
	 * 
	 * @param runnable
	 * @param delayMillis
	 */
	public void runOnUiThreadDelay(final Runnable runnable, long delayMillis) {
		handler.postDelayed(runnable, delayMillis);
	}

}
