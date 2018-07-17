package com.xabber.android.presentation.mvp.contactlist;

import android.os.Handler;

import java.util.Date;

/**
 * Created by valery.miller on 09.02.18.
 */

public class UpdateBackpressure implements Runnable {

    private static final long REFRESH_INTERVAL = 1000;

    public interface UpdatableObject {
        void update();
    }

    private UpdatableObject updatableObject;

    /**
     * Handler for deferred refresh.
     */
    private final Handler handler;

    /**
     * Lock for refresh requests.
     */
    private final Object refreshLock;

    /**
     * Whether refresh was requested.
     */
    private boolean refreshRequested;

    /**
     * Whether refresh is in progress.
     */
    private boolean refreshInProgress;

    /**
     * Minimal time when next refresh can be executed.
     */
    private Date nextRefresh;

    public UpdateBackpressure(UpdatableObject updatableObject) {
        this.updatableObject = updatableObject;
        handler = new Handler();
        refreshLock = new Object();
        refreshRequested = false;
        refreshInProgress = false;
        nextRefresh = new Date();
    }

    @Override
    public void run() {
        build();
    }

    /**
     * Requests refresh in some time in future.
     */
    public void refreshRequest() {
        synchronized (refreshLock) {
            if (refreshRequested) {
                return;
            }
            if (refreshInProgress) {
                refreshRequested = true;
            } else {
                long delay = nextRefresh.getTime() - new Date().getTime();
                handler.postDelayed(this, delay > 0 ? delay : 0);
            }
        }
    }

    /**
     * Remove refresh requests.
     */
    public void removeRefreshRequests() {
        synchronized (refreshLock) {
            refreshRequested = false;
            refreshInProgress = false;
            handler.removeCallbacks(this);
        }
    }

    protected void build() {
        synchronized (refreshLock) {
            refreshRequested = false;
            refreshInProgress = true;
            handler.removeCallbacks(this);
        }

        updatableObject.update();

        synchronized (refreshLock) {
            nextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
            refreshInProgress = false;
            handler.removeCallbacks(this); // Just to be sure.
            if (refreshRequested) {
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    }
}
