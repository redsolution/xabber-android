package com.xabber.android.data.push;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.service.XabberService;

public class SyncManager implements OnTimerListener {

    private static final long SYNC_TIME = 60000;
    public static final String SYNC_MODE = "SYNC_MODE";
    public static final String PUSH_NODE = "PUSH_NODE";
    public static final String ACCOUNT_JID = "ACCOUNT_JID";

    private static SyncManager instance;
    private boolean syncMode;
    private boolean syncPeriod;
    private boolean messageSaved;

    private long timestamp;
    private String pushNode;
    private AccountJid accountJid;

    public static SyncManager getInstance() {
        if (instance == null)
            instance = new SyncManager();
        return instance;
    }

    @Override
    public void onTimer() {
        if (syncPeriod && syncMode && isTimeToStopSyncPeriod()) {
            stopSyncPeriod();
            XabberService.getInstance().changeForeground();
        }
    }

    public void onMessageSaved() {
        this.messageSaved = true;
    }

    public void onServiceStarted(Intent intent) {
        if (intent != null && intent.getBooleanExtra(SYNC_MODE, false)) {
            if (intent.hasExtra(PUSH_NODE)) startSyncMode(intent.getStringExtra(PUSH_NODE));
            else if (intent.hasExtra(ACCOUNT_JID)) startSyncMode((AccountJid) intent.getParcelableExtra(ACCOUNT_JID));
        }
    }

    public void onActivityResume() {
        stopSyncMode();
    }

    public boolean isAccountNeedConnection(AccountItem accountItem) {
        if (!syncMode || (pushNode == null && accountJid == null)) return true;
        if (pushNode != null) return pushNode.equals(accountItem.getPushNode());
        else return accountJid.equals(accountItem.getAccount());
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

    public static Intent createXabberServiceIntentWithSyncMode(Context context, AccountJid accountJid) {
        Intent intent = new Intent(context, XabberService.class);
        intent.putExtra(SYNC_MODE, true);
        intent.putExtra(ACCOUNT_JID, (Parcelable) accountJid);
        return intent;
    }

    private void startSyncMode(String pushNode) {
        this.syncMode = true;
        this.timestamp = System.currentTimeMillis();
        this.pushNode = pushNode;
        this.syncPeriod = true;
        this.messageSaved = false;
    }

    private void startSyncMode(AccountJid accountJid) {
        this.syncMode = true;
        this.timestamp = System.currentTimeMillis();
        this.accountJid = accountJid;
        this.syncPeriod = true;
        this.messageSaved = false;
    }

    private void stopSyncMode() {
        this.syncMode = false;
        this.pushNode = null;
        this.accountJid = null;
        this.syncPeriod = false;
    }

    private void stopSyncPeriod() {
        this.syncPeriod = false;
    }

    private boolean isTimeToStopSyncPeriod() {
        return messageSaved || System.currentTimeMillis() > timestamp + SYNC_TIME;
    }
}
