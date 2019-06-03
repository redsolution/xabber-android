package com.xabber.android.data.push;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.service.XabberService;

import java.util.ArrayList;
import java.util.List;

public class SyncManager implements OnTimerListener {

    private static final long SYNC_TIME = 60000;
    public static final String SYNC_MODE = "SYNC_MODE";
    public static final String PUSH_NODE = "PUSH_NODE";
    public static final String ACCOUNT_JID = "ACCOUNT_JID";

    private static SyncManager instance;
    private boolean syncMode;
    private boolean syncPeriod;
    private boolean syncActionDone;
    private boolean syncNotifActionDone;

    private long timestamp;
    private List<String> pushNodes = new ArrayList<>();
    private List<AccountJid> accountJids = new ArrayList<>();

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
        this.syncActionDone = true;
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
        if (!syncMode || (pushNodes.isEmpty() && accountJids.isEmpty())) return true;
        return pushNodes.contains(accountItem.getPushNode()) || accountJids.contains(accountItem.getAccount());
    }

    public boolean isSyncPeriod() {
        return syncPeriod;
    }

    public boolean isSyncMode() {
        return syncMode;
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

    public boolean isAccountAllowed(AccountJid account) {
        if (!syncMode) return true;
        return accountJids.contains(account);
    }

    public void addAllowedAccount(String node) {
        startSyncMode(node);
        XabberService.getInstance().changeForeground();
    }

    public void addAllowedAccount(AccountJid account) {
        startSyncMode(account);
        XabberService.getInstance().changeForeground();
    }

    private void startSyncMode(String pushNode) {
        this.syncMode = true;
        this.timestamp = System.currentTimeMillis();
        if (this.pushNodes != null && !this.pushNodes.contains(pushNode))
            this.pushNodes.add(pushNode);
        this.syncPeriod = true;
        this.syncActionDone = false;
        this.syncNotifActionDone = true;
    }

    private void startSyncMode(AccountJid accountJid) {
        this.syncMode = true;
        this.timestamp = System.currentTimeMillis();
        if (this.accountJids != null && !this.accountJids.contains(accountJid))
            this.accountJids.add(accountJid);
        this.syncPeriod = true;
        this.syncActionDone = true;
        this.syncNotifActionDone = false;
    }

    private void stopSyncMode() {
        this.syncMode = false;
        this.pushNodes.clear();
        this.accountJids.clear();
        this.syncPeriod = false;
    }

    private void stopSyncPeriod() {
        this.syncPeriod = false;
    }

    private boolean isTimeToStopSyncPeriod() {
        return (syncActionDone && syncNotifActionDone) || System.currentTimeMillis() > timestamp + SYNC_TIME;
    }
}
