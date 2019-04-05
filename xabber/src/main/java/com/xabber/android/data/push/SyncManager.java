package com.xabber.android.data.push;

import android.content.Context;
import android.content.Intent;

import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.service.XabberService;

public class SyncManager implements OnTimerListener {

    private static final long SYNC_TIME = 60000;
    public static final String SYNC_MODE = "SYNC_MODE";
    public static final String PUSH_NODE = "PUSH_NODE";

    private static SyncManager instance;
    private boolean syncMode;
    private boolean syncPeriod;

    private long timestamp;
    private String pushNode;

    public static SyncManager getInstance() {
        if (instance == null)
            instance = new SyncManager();
        return instance;
    }

    @Override
    public void onTimer() {
        if (syncPeriod && syncMode && isTimeToShutdown()) {
            stopSyncPeriod();
            XabberService.getInstance().changeForeground();
        }
    }

    public void onMessageSaved() {
        this.timestamp = System.currentTimeMillis();
    }

    public void onServiceStarted(Intent intent) {
        if (intent != null && intent.getBooleanExtra(SYNC_MODE, false))
            startSyncMode(intent.getStringExtra(PUSH_NODE));
    }

    public void onActivityResume() {
        stopSyncMode();
    }

    public boolean isAccountNeedConnection(AccountItem accountItem) {
        if (!syncMode || pushNode == null) return true;
        return pushNode.equals(accountItem.getPushNode());
    }

    public boolean isSyncPeriod() {
        return syncPeriod;
    }

    public static Intent createXabberServiceIntentWithSyncMode(Context context, String pushNode) {
        Intent intent = new Intent(context, XabberService.class);
        intent.putExtra(SYNC_MODE, true);
        intent.putExtra(PUSH_NODE, pushNode);
        return intent;
    }

    private void startSyncMode(String pushNode) {
        this.syncMode = true;
        this.timestamp = System.currentTimeMillis();
        this.pushNode = pushNode;
        this.syncPeriod = true;
    }

    private void stopSyncMode() {
        this.syncMode = false;
        this.pushNode = null;
        this.syncPeriod = false;
    }

    private void stopSyncPeriod() {
        this.syncPeriod = false;
    }

    private boolean isTimeToShutdown() {
        return System.currentTimeMillis() > timestamp + SYNC_TIME;
    }
}
