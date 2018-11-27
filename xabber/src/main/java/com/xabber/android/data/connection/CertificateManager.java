package com.xabber.android.data.connection;


import android.app.Activity;
import android.support.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.entity.AccountJid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.duenndns.ssl.MemorizingTrustManager;

public class CertificateManager implements OnAccountRemovedListener {

    private Activity currentActivityForBind;
    private static CertificateManager instance;

    public static CertificateManager getInstance() {
        if (instance == null) {
            instance = new CertificateManager();
        }

        return instance;
    }

    private Map<AccountJid, MemorizingTrustManager> memorizingTrustManagerMap;
    private Map<AccountJid, MemorizingTrustManager> fileUploadMap;

    private CertificateManager() {
        this.memorizingTrustManagerMap = new ConcurrentHashMap<>();
        this.fileUploadMap = new ConcurrentHashMap<>();
    }

    @NonNull
    MemorizingTrustManager getNewMemorizingTrustManager(@NonNull final AccountJid accountJid) {
        MemorizingTrustManager mtm = new MemorizingTrustManager(Application.getInstance());
        if (currentActivityForBind != null) mtm.bindDisplayActivity(currentActivityForBind);
        memorizingTrustManagerMap.put(accountJid, mtm);
        return mtm;
    }

    @NonNull
    public MemorizingTrustManager getNewFileUploadManager(@NonNull final AccountJid accountJid) {
        MemorizingTrustManager mtm = new MemorizingTrustManager(Application.getInstance());
        if (currentActivityForBind != null) mtm.bindDisplayActivity(currentActivityForBind);
        fileUploadMap.put(accountJid, mtm);
        return mtm;
    }

    public void registerActivity(Activity activity) {
        for (MemorizingTrustManager memorizingTrustManager : memorizingTrustManagerMap.values()) {
            memorizingTrustManager.bindDisplayActivity(activity);
        }

        for (MemorizingTrustManager memorizingTrustManager : fileUploadMap.values()) {
            memorizingTrustManager.bindDisplayActivity(activity);
        }
        currentActivityForBind = activity;
    }

    public void unregisterActivity(Activity activity) {
        for (MemorizingTrustManager memorizingTrustManager : memorizingTrustManagerMap.values()) {
            memorizingTrustManager.unbindDisplayActivity(activity);
        }

        for (MemorizingTrustManager memorizingTrustManager : fileUploadMap.values()) {
            memorizingTrustManager.unbindDisplayActivity(activity);
        }
        if (currentActivityForBind == activity) currentActivityForBind = null;
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        memorizingTrustManagerMap.remove(accountItem.getAccount());
        fileUploadMap.remove(accountItem.getAccount());
    }
}
