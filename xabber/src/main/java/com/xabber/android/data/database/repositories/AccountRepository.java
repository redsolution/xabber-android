package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionSettings;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AccountRealmObject;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class AccountRepository {

    private static final String LOG_TAG = AccountRepository.class.getSimpleName();

    public static ArrayList<AccountRealmObject> getEnabledAccountsFromRealm(){
        ArrayList<AccountRealmObject> accounts = new ArrayList<>();
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<AccountRealmObject> results = realm
                .where(AccountRealmObject.class)
                .findAll();
        for (AccountRealmObject accountRealmObject : results)
            if (accountRealmObject.isEnabled())
                accounts.add(accountRealmObject);
        if (Looper.getMainLooper() != Looper.myLooper())
            realm.close();
        return accounts;
    }

    public static void saveAccountToRealm(AccountItem accountItem) {

        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    AccountRealmObject managedAccountRealmObject = realm1.where(AccountRealmObject.class)
                            .equalTo(AccountRealmObject.Fields.ID, accountItem.getId())
                            .findFirst();

                    AccountRealmObject accountRealmObject;
                    if (managedAccountRealmObject == null) {
                        accountRealmObject = new AccountRealmObject(accountItem.getId());
                    } else {
                        // To safely assign an unmanaged token realm object
                        // to a managed account realm object, we can:
                        // 1) make the managed object into an
                        //      unmanaged one and copyToRealm.
                        // 2) make the token object into the
                        //      managed one with copyToRealm before assigning.
                        // This is 1)
                        accountRealmObject = realm1.copyFromRealm(managedAccountRealmObject);
                    }

                    ConnectionSettings connectionSettings = accountItem.getConnectionSettings();

                    accountRealmObject.setCustom(connectionSettings.isCustomHostAndPort());
                    accountRealmObject.setHost(connectionSettings.getHost());
                    accountRealmObject.setPort(connectionSettings.getPort());
                    accountRealmObject.setServerName(connectionSettings.getServerName().getDomain().toString());
                    accountRealmObject.setUserName(connectionSettings.getUserName().toString());

                    String password = connectionSettings.getPassword();
                    if (!accountItem.isStorePassword()) {
                        password = AccountItem.UNDEFINED_PASSWORD;
                    }
                    accountRealmObject.setPassword(password);

                    if (connectionSettings.getXToken() != null)
                        accountRealmObject.setXToken(XTokenManager.tokenToXTokenRealm(connectionSettings.getXToken()));
                    accountRealmObject.setToken(connectionSettings.getToken());
                    accountRealmObject.setOrder(accountItem.getOrder());
                    accountRealmObject.setSyncNotAllowed(accountItem.isSyncNotAllowed());
                    accountRealmObject.setXabberAutoLoginEnabled(accountItem.isXabberAutoLoginEnabled());
                    accountRealmObject.setTimestamp(accountItem.getTimestamp());
                    accountRealmObject.setResource(connectionSettings.getResource().toString());
                    accountRealmObject.setColorIndex(accountItem.getColorIndex());
                    accountRealmObject.setPriority(accountItem.getPriority());
                    accountRealmObject.setStatusMode(accountItem.getRawStatusMode());
                    accountRealmObject.setStatusText(accountItem.getStatusText());
                    accountRealmObject.setEnabled(accountItem.isEnabled());
                    accountRealmObject.setSaslEnabled(connectionSettings.isSaslEnabled());
                    accountRealmObject.setTlsMode(connectionSettings.getTlsMode());
                    accountRealmObject.setCompression(connectionSettings.useCompression());
                    accountRealmObject.setProxyType(connectionSettings.getProxyType());
                    accountRealmObject.setProxyHost(connectionSettings.getProxyHost());
                    accountRealmObject.setProxyPort(connectionSettings.getProxyPort());
                    accountRealmObject.setProxyUser(connectionSettings.getProxyUser());
                    accountRealmObject.setProxyPassword(connectionSettings.getProxyPassword());
                    accountRealmObject.setSyncable(accountItem.isSyncable());
                    accountRealmObject.setStorePassword(accountItem.isStorePassword());
                    accountRealmObject.setKeyPair(accountItem.getKeyPair());
                    accountRealmObject.setLastSync(accountItem.getLastSync());
                    accountRealmObject.setArchiveMode(accountItem.getArchiveMode());
                    accountRealmObject.setClearHistoryOnExit(accountItem.isClearHistoryOnExit());
                    accountRealmObject.setMamDefaultBehavior(accountItem.getMamDefaultBehaviour());
                    accountRealmObject.setLoadHistorySettings(accountItem.getLoadHistorySettings());
                    accountRealmObject.setSuccessfulConnectionHappened(accountItem.isSuccessfulConnectionHappened());
                    accountRealmObject.setPushNode(accountItem.getPushNode());
                    accountRealmObject.setPushServiceJid(accountItem.getPushServiceJid());
                    accountRealmObject.setPushEnabled(accountItem.isPushEnabled());
                    accountRealmObject.setPushWasEnabled(accountItem.isPushWasEnabled());

                    realm1.copyToRealmOrUpdate(accountRealmObject);
                    LogManager.d(LOG_TAG, "Account " + accountItem.getAccount().getBareJid() + " has been successfully " +
                            "saved with new settings");
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void clearAllAccountsFromRealm(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(AccountRealmObject.class)
                        .findAll()
                        .deleteAllFromRealm());
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static void deleteAccountFromRealm(final String account, final String id){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    AccountRealmObject accountRealmObject = realm1
                            .where(AccountRealmObject.class)
                            //.equalTo(AccountRealm.Fields.USERNAME, account) //TODO WARN this is possible reason of non deleting accounts
                            .equalTo(AccountRealmObject.Fields.ID, id)
                            .findFirst();

                    if (accountRealmObject != null) {
                        accountRealmObject.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
