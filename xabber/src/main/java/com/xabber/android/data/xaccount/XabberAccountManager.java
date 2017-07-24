package com.xabber.android.data.xaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.XMPPAccountSettignsRealm;
import com.xabber.android.data.database.realm.XMPPUserRealm;
import com.xabber.android.data.database.realm.XabberAccountRealm;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountManager implements OnLoadListener {

    private static final String LOG_TAG = XabberAccountManager.class.getSimpleName();
    private static XabberAccountManager instance;

    private XabberAccount account;
    private List<XMPPAccountSettings> xmppAccounts;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    private XabberAccountManager() {
        xmppAccounts = new ArrayList<>();
    }

    @Override
    public void onLoad() {
        XabberAccount account = loadXabberAccountFromRealm();
        this.account = account;

        if (account != null) {
            getAccountFromNet(account.getToken());
        }
    }

    private void getAccountFromNet(String token) {
        Subscription getAccountSubscription = AuthManager.getAccount(token)
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount xabberAccount) {
                        handleSuccessGetAccount(xabberAccount);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetAccount(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        Log.d(LOG_TAG, "Xabber account loading from net: successfully");
        this.account = xabberAccount;
        getSettingsFromNet();
    }

    private void handleErrorGetAccount(Throwable throwable) {
        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());
        // TODO: 24.07.17 need login
    }

    private void getSettingsFromNet() {
        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        updateXmppAccounts(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void updateXmppAccounts(List<XMPPAccountSettings> items) {
        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
        this.xmppAccounts.clear();
        this.xmppAccounts.addAll(items);
    }

    public List<XMPPAccountSettings> getXmppAccounts() {
        return xmppAccounts;
    }

    @Nullable
    public XabberAccount getAccount() {
        return account;
    }

    public void removeAccount() {
        this.account = null;
        this.xmppAccounts.clear();
    }

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount, String token) {
        XabberAccount account;
        XabberAccountRealm xabberAccountRealm = new XabberAccountRealm(String.valueOf(xabberAccount.getId()));

        xabberAccountRealm.setToken(token);
        xabberAccountRealm.setUsername(xabberAccount.getUsername());
        xabberAccountRealm.setFirstName(xabberAccount.getFirstName());
        xabberAccountRealm.setLastName(xabberAccount.getLastName());
        xabberAccountRealm.setRegisterDate(xabberAccount.getRegistrationDate());

        RealmList<XMPPUserRealm> realmUsers = new RealmList<>();
        for (XMPPUserDTO user : xabberAccount.getXmppUsers()) {
            XMPPUserRealm realmUser = new XMPPUserRealm(String.valueOf(user.getId()));
            realmUser.setUsername(user.getUsername());
            realmUser.setHost(user.getHost());
            realmUser.setRegistration_date(user.getRegistrationDate());
            realmUsers.add(realmUser);
        }
        xabberAccountRealm.setXmppUsers(realmUsers);

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
        if (accountRealm == null) return null;

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
                xmppUsers,
                accountRealm.getToken()
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

    public boolean deleteXabberAccountFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        final RealmResults<XabberAccountRealm> results = realm.where(XabberAccountRealm.class).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                success[0] = results.deleteAllFromRealm();
            }
        });
        realm.close();
        return success[0];
    }

    public boolean deleteXMPPAccountsFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        final RealmResults<XMPPAccountSettignsRealm> results = realm.where(XMPPAccountSettignsRealm.class).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                success[0] = results.deleteAllFromRealm();
            }
        });
        realm.close();
        return success[0];
    }

    public Single<List<XMPPAccountSettings>> loadXMPPAccountSettingsFromRealm() {
        List<XMPPAccountSettings> result = null;

        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<XMPPAccountSettignsRealm> realmItems = realm.where(XMPPAccountSettignsRealm.class).findAll();
        result = xmppAccountSettingsRealmListToPOJO(realmItems);

        realm.close();
        return Single.just(result);
    }

    @Nullable
    public Single<List<XMPPAccountSettings>> saveOrUpdateXMPPAccountSettingsToRealm(AuthManager.ListClientSettingsDTO listXMPPAccountSettings) {
        List<XMPPAccountSettings> result;
        RealmList<XMPPAccountSettignsRealm> realmItems = new RealmList<>();

        for (AuthManager.ClientSettingsDTO dtoItem : listXMPPAccountSettings.getSettings()) {
            XMPPAccountSettignsRealm realmItem = new XMPPAccountSettignsRealm(dtoItem.getJid());

            AuthManager.SettingsValuesDTO valuesDTO = dtoItem.getSettings();
            if (valuesDTO != null) {
                realmItem.setToken(valuesDTO.getToken());
                realmItem.setColor(valuesDTO.getColor());
                realmItem.setOrder(valuesDTO.getOrder());
                // TODO: 21.07.17 add sync, timestamp, username
            }
            realmItems.add(realmItem);
        }

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        realm.beginTransaction();
        List<XMPPAccountSettignsRealm> resultRealm = realm.copyToRealmOrUpdate(realmItems);
        result = xmppAccountSettingsRealmListToPOJO(resultRealm);
        realm.commitTransaction();
        realm.close();

        updateXmppAccounts(result);
        return Single.just(result);
    }

    @Nullable
    public List<XMPPAccountSettings> xmppAccountSettingsRealmListToPOJO(List<XMPPAccountSettignsRealm> realmitems) {
        if (realmitems == null) return null;

        List<XMPPAccountSettings> items = new ArrayList<>();
        for (XMPPAccountSettignsRealm realmItem : realmitems) {
            items.add(xmppAccountSettingsRealmToPOJO(realmItem));
        }
        return items;
    }

    @Nullable
    public XMPPAccountSettings xmppAccountSettingsRealmToPOJO(XMPPAccountSettignsRealm realmItem) {
        if (realmItem == null) return null;

        XMPPAccountSettings item = new XMPPAccountSettings(
                realmItem.getJid(),
                realmItem.isSynchronization(),
                realmItem.getTimestamp());

        item.setOrder(realmItem.getOrder());
        item.setColor(realmItem.getColor());
        item.setToken(realmItem.getToken());
        item.setUsername(realmItem.getUsername());

        return item;
    }
}

