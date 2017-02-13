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

    private static CertificateManager instance;

    public static CertificateManager getInstance() {
        if (instance == null) {
            instance = new CertificateManager();
        }

        return instance;
    }

    private Map<AccountJid, MemorizingTrustManager> memorizingTrustManagerMap;

    private CertificateManager() {
        this.memorizingTrustManagerMap = new ConcurrentHashMap<>();
    }

    @NonNull
    MemorizingTrustManager getNewMemorizingTrustManager(@NonNull final AccountJid accountJid) {
        MemorizingTrustManager mtm = new MemorizingTrustManager(Application.getInstance());
        memorizingTrustManagerMap.put(accountJid, mtm);
        return mtm;
    }

    public void registerActivity(Activity activity) {
        for (MemorizingTrustManager memorizingTrustManager : memorizingTrustManagerMap.values()) {
            memorizingTrustManager.bindDisplayActivity(activity);
        }
    }

    public void unregisterActivity(Activity activity) {
        for (MemorizingTrustManager memorizingTrustManager : memorizingTrustManagerMap.values()) {
            memorizingTrustManager.unbindDisplayActivity(activity);
        }
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        memorizingTrustManagerMap.remove(accountItem.getAccount());
    }
}
