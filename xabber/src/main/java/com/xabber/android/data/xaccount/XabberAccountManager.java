package com.xabber.android.data.xaccount;

import rx.Single;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XabberAccountManager {

    private static final String LOG_TAG = XabberAccountManager.class.getSimpleName();
    private static XabberAccountManager instance;

    private XabberAccount account;

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    private XabberAccountManager() {}

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount) {
        return Single.just(null);
    }

}
