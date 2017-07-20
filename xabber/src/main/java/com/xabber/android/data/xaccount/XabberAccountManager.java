package com.xabber.android.data.xaccount;

import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.XMPPUserRealm;
import com.xabber.android.data.database.realm.XabberAccountRealm;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

import io.realm.RealmResults;
import rx.Single;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountManager implements OnLoadListener {

    private static final String LOG_TAG = XabberAccountManager.class.getSimpleName();
    private static XabberAccountManager instance;

    private XabberAccount account;

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    private XabberAccountManager() {}

    @Override
    public void onLoad() {
        final XabberAccount account = loadXabberAccountFromRealm();

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(account);
            }
        });
    }

    private void onLoaded(XabberAccount account) {
        this.account = account;
    }

    @Nullable
    public XabberAccount getAccount() {
        return account;
    }

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount) {
        XabberAccount account;
        XabberAccountRealm xabberAccountRealm = new XabberAccountRealm(String.valueOf(xabberAccount.getId()));

        xabberAccountRealm.setUsername(xabberAccount.getUsername());
        xabberAccountRealm.setFirstName(xabberAccount.getFirstName());
        xabberAccountRealm.setLastName(xabberAccount.getLastName());
        xabberAccountRealm.setRegisterDate(xabberAccount.getRegistrationDate());

//        List<XMPPUserRealm> realmUsers = new ArrayList<>();
//        for (XMPPUser user : xabberAccount.getXmppUsers()) {
//            XMPPUserRealm realmUser = new XMPPUserRealm(user.getId());
//            realmUser.setUsername(user.getUsername());
//            realmUser.setHost(user.getHost());
//            realmUser.setRegistration_date(user.getRegisterDate());
//            realmUsers.add(realmUser);
//        }
//
//        xabberAccountRealm.setXmppUsers(realmUsers);

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        realm.beginTransaction();
        XabberAccountRealm accountRealm = realm.copyToRealmOrUpdate(xabberAccountRealm);
        account = xabberAccountRealmToPOJO(accountRealm);
        realm.commitTransaction();
        realm.close();

        this.account = account;
        return Single.just(account);
    }

    public static XabberAccount xabberAccountRealmToPOJO(XabberAccountRealm accountRealm) {
        XabberAccount xabberAccount = null;

        List<XMPPUser> xmppUsers = new ArrayList<>();
        for (XMPPUserRealm xmppUserRealm : accountRealm.getXmppUsers()) {
            XMPPUser xmppUser = new XMPPUser(
                    Integer.parseInt(xmppUserRealm.getId()),
                    xmppUserRealm.getUsername(),
                    xmppUserRealm.getHost(),
                    xmppUserRealm.getRegistration_date());

            xmppUsers.add(xmppUser);
        }

        xabberAccount = new XabberAccount(
                Integer.parseInt(accountRealm.getId()),
                accountRealm.getUsername(),
                accountRealm.getFirstName(),
                accountRealm.getLastName(),
                accountRealm.getRegisterDate(),
                xmppUsers
        );

        return xabberAccount;
    }

    @Nullable
    public XabberAccount loadXabberAccountFromRealm() {
        XabberAccount xabberAccount = null;

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        RealmResults<XabberAccountRealm> xabberAccounts = realm.where(XabberAccountRealm.class).findAll();

        for (XabberAccountRealm xabberAccountRealm : xabberAccounts) {
            xabberAccount = xabberAccountRealmToPOJO(xabberAccountRealm);
        }

        realm.close();
        return xabberAccount;
    }
}

