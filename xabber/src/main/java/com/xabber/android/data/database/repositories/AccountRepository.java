package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionSettings;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AccountRealm;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;

public class AccountRepository {

    public static void saveAccountToRealm(AccountItem accountItem){

        Application.getInstance().runInBackground(() -> {
            AccountRealm accountRealm = new AccountRealm(accountItem.getId());

            ConnectionSettings connectionSettings = accountItem.getConnectionSettings();

            accountRealm.setCustom(connectionSettings.isCustomHostAndPort());
            accountRealm.setHost(connectionSettings.getHost());
            accountRealm.setPort(connectionSettings.getPort());
            accountRealm.setServerName(connectionSettings.getServerName().getDomain().toString());
            accountRealm.setUserName(connectionSettings.getUserName().toString());

            String password = connectionSettings.getPassword();
            if (!accountItem.isStorePassword()) {
                password = AccountItem.UNDEFINED_PASSWORD;
            }
            accountRealm.setPassword(password);

            if (connectionSettings.getXToken() != null)
                accountRealm.setXToken(XTokenManager.tokenToXTokenRealm(connectionSettings.getXToken()));
            accountRealm.setToken(connectionSettings.getToken());
            accountRealm.setOrder(accountItem.getOrder());
            accountRealm.setSyncNotAllowed(accountItem.isSyncNotAllowed());
            accountRealm.setXabberAutoLoginEnabled(accountItem.isXabberAutoLoginEnabled());
            accountRealm.setTimestamp(accountItem.getTimestamp());
            accountRealm.setResource(connectionSettings.getResource().toString());
            accountRealm.setColorIndex(accountItem.getColorIndex());
            accountRealm.setPriority(accountItem.getPriority());
            accountRealm.setStatusMode(accountItem.getRawStatusMode());
            accountRealm.setStatusText(accountItem.getStatusText());
            accountRealm.setEnabled(accountItem.isEnabled());
            accountRealm.setSaslEnabled(connectionSettings.isSaslEnabled());
            accountRealm.setTlsMode(connectionSettings.getTlsMode());
            accountRealm.setCompression(connectionSettings.useCompression());
            accountRealm.setProxyType(connectionSettings.getProxyType());
            accountRealm.setProxyHost(connectionSettings.getProxyHost());
            accountRealm.setProxyPort(connectionSettings.getProxyPort());
            accountRealm.setProxyUser(connectionSettings.getProxyUser());
            accountRealm.setProxyPassword(connectionSettings.getProxyPassword());
            accountRealm.setSyncable(accountItem.isSyncable());
            accountRealm.setStorePassword(accountItem.isStorePassword());
            accountRealm.setKeyPair(accountItem.getKeyPair());
            accountRealm.setLastSync(accountItem.getLastSync());
            accountRealm.setArchiveMode(accountItem.getArchiveMode());
            accountRealm.setClearHistoryOnExit(accountItem.isClearHistoryOnExit());
            accountRealm.setMamDefaultBehavior(accountItem.getMamDefaultBehaviour());
            accountRealm.setLoadHistorySettings(accountItem.getLoadHistorySettings());
            accountRealm.setSuccessfulConnectionHappened(accountItem.isSuccessfulConnectionHappened());
            accountRealm.setPushNode(accountItem.getPushNode());
            accountRealm.setPushServiceJid(accountItem.getPushServiceJid());
            accountRealm.setPushEnabled(accountItem.isPushEnabled());
            accountRealm.setPushWasEnabled(accountItem.isPushWasEnabled());

            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(accountRealm);
                });
            } catch (Exception e) {
                LogManager.exception("AccountTable", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static void clearAllAccountsFromRealm(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(AccountRealm.class)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("AccountRepository", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static void deleteAccountFromRealm(final String account, final String id){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    AccountRealm accountRealm = realm1
                            .where(AccountRealm.class)
                            //.equalTo(AccountRealm.Fields.USERNAME, account) //TODO WARN this is possible reason of non deleting accounts
                            .equalTo(AccountRealm.Fields.ID, id)
                            .findFirst();

                    if (accountRealm != null) {
                        accountRealm.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception("AccountTable", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }
}
