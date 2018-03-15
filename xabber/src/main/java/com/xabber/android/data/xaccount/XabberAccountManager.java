package com.xabber.android.data.xaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.EmailRealm;
import com.xabber.android.data.database.realm.SocialBindingRealm;
import com.xabber.android.data.database.realm.SyncStateRealm;
import com.xabber.android.data.database.realm.XMPPUserRealm;
import com.xabber.android.data.database.realm.XabberAccountRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.RetrofitErrorConverter;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private List<XMPPAccountSettings> xmppAccountsForSync;
    private List<XMPPAccountSettings> xmppAccountsForCreate;

    private Map<String, Boolean> accountsSyncState;

    private int lastOrderChangeTimestamp;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    private XabberAccountManager() {
        accountsSyncState = new HashMap<>();
        xmppAccountsForSync = new ArrayList<>();
        xmppAccountsForCreate = new ArrayList<>();
    }

    /**
     * Add or update synchronization values
     * @param accountsSyncState
     */
    public void setAccountSyncState(Map<String, Boolean> accountsSyncState) {
        this.accountsSyncState.putAll(accountsSyncState);
        saveSyncStatesToRealm(this.accountsSyncState);
    }

    /**
     * Set synchronization for jid, if jid exist in map
     * @param jid
     * @param sync
     */
    public void setAccountSyncState(String jid, boolean sync) {
        if (this.accountsSyncState.containsKey(jid)) {
            this.accountsSyncState.put(jid, sync);
            saveSyncStatesToRealm(this.accountsSyncState);
        }
    }

    /**
     * Add or update synchronization value to map
     * @param jid
     * @param sync
     */
    public void addAccountSyncState(String jid, boolean sync) {
        this.accountsSyncState.put(jid, sync);
        saveSyncStatesToRealm(this.accountsSyncState);
    }

    public void setAllExistingAccountSync(boolean sync) {
        for (Map.Entry<String, Boolean> entry : accountsSyncState.entrySet()) {
            entry.setValue(sync);
        }
        saveSyncStatesToRealm(this.accountsSyncState);
    }

    public boolean isAccountSynchronize(String jid) {
        boolean syncNotAllowed = false;
        AccountJid accountJid = getExistingAccount(jid);
        if (accountJid != null) {
            AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
            if (accountItem != null) syncNotAllowed = accountItem.isSyncNotAllowed();
        }

        if (accountsSyncState.containsKey(jid) && !syncNotAllowed)
            return accountsSyncState.get(jid);
        else return false;
    }

    @Nullable
    public Boolean getAccountSyncState(String jid) {
        if (accountsSyncState.containsKey(jid))
            return accountsSyncState.get(jid);
        else return null;
    }

    public List<XMPPAccountSettings> getXmppAccountsForSync() {
        return xmppAccountsForSync;
    }

    public void setXmppAccountsForSync(List<XMPPAccountSettings> items) {
        this.xmppAccountsForSync.clear();
        this.xmppAccountsForSync.addAll(items);
    }

    public void setXmppAccountsForCreate(List<XMPPAccountSettings> items) {
        this.xmppAccountsForCreate.clear();
        this.xmppAccountsForCreate.addAll(items);
    }

    @Nullable
    public List<XMPPAccountSettings> getXmppAccountsForCreate() {
        Collections.sort(xmppAccountsForCreate, Collections.reverseOrder());
        return xmppAccountsForCreate;
    }

    public void clearXmppAccountsForCreate() {
        this.xmppAccountsForCreate.clear();
    }

    public int getLastOrderChangeTimestamp() {
        return lastOrderChangeTimestamp;
    }

    public void setLastOrderChangeTimestampIsNow() {
        this.lastOrderChangeTimestamp = getCurrentTime();
        SettingsManager.setLastOrderChangeTimestamp(lastOrderChangeTimestamp);
    }

    /**
     * Update account settings
     * If catches Invalid Token error disable sync in last account before logout
     * Calls only in Add Account function
     */
    public void updateSettingsWithSaveLastAccount(final AccountJid jid) {
        Subscription updateSettingsSubscription = AuthManager.patchClientSettings(createSettingsList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());

                        // invalid token
                        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                        if (message != null && message.equals("Invalid token")) {
                            // save last account
                            setAccountSyncState(jid.getFullJid().asBareJid().toString(), false);
                            // logout from deleted account
                            onInvalidToken();
                        }
                    }
                });
        compositeSubscription.add(updateSettingsSubscription);
    }

    @Override
    public void onLoad() {
        XabberAccount account = loadXabberAccountFromRealm();
        this.account = account;

        this.lastOrderChangeTimestamp = SettingsManager.getLastOrderChangeTimestamp();

        // load sync state from realm
        this.accountsSyncState = loadSyncStatesFromRealm();

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

    public void updateAccountSettings() {
        List<XMPPAccountSettings> list = createSettingsList();
        if (list != null && list.size() > 0) {
            Subscription updateSettingsSubscription = AuthManager.patchClientSettings(list)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<XMPPAccountSettings>>() {
                        @Override
                        public void call(List<XMPPAccountSettings> s) {
                            Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());

                            // invalid token
                            String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                            if (message != null && message.equals("Invalid token")) {
                                // logout from deleted account
                                onInvalidToken();
                            }
                        }
                    });
            compositeSubscription.add(updateSettingsSubscription);
        }
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        Log.d(LOG_TAG, "Xabber account loading from net: successfully");
        this.account = xabberAccount;
        updateAccountSettings();
    }

    private void handleErrorGetAccount(Throwable throwable) {
        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());

        // invalid token
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null && message.equals("Invalid token")) {
            onInvalidToken();
        }
    }

    @Nullable
    public XabberAccount getAccount() {
        return account;
    }

    public void removeAccount() {
        this.account = null;
        this.accountsSyncState.clear();
    }

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount, String token) {
        XabberAccount account;
        XabberAccountRealm xabberAccountRealm = new XabberAccountRealm(String.valueOf(xabberAccount.getId()));

        xabberAccountRealm.setToken(token);
        xabberAccountRealm.setAccountStatus(xabberAccount.getAccountStatus());
        xabberAccountRealm.setUsername(xabberAccount.getUsername());
        xabberAccountRealm.setFirstName(xabberAccount.getFirstName());
        xabberAccountRealm.setLastName(xabberAccount.getLastName());
        xabberAccountRealm.setLanguage(xabberAccount.getLanguage());
        xabberAccountRealm.setRegisterDate(xabberAccount.getRegistrationDate());
        xabberAccountRealm.setNeedToVerifyPhone(xabberAccount.isNeedToVerifyPhone());
        xabberAccountRealm.setPhone(xabberAccount.getPhone());

        RealmList<XMPPUserRealm> realmUsers = new RealmList<>();
        for (XMPPUserDTO user : xabberAccount.getXmppUsers()) {
            XMPPUserRealm realmUser = new XMPPUserRealm(String.valueOf(user.getId()));
            realmUser.setUsername(user.getUsername());
            realmUser.setHost(user.getHost());
            realmUser.setRegistration_date(user.getRegistrationDate());
            realmUsers.add(realmUser);
        }
        xabberAccountRealm.setXmppUsers(realmUsers);

        RealmList<EmailRealm> realmEmails = new RealmList<>();
        for (EmailDTO email : xabberAccount.getEmails()) {
            EmailRealm realmEmail = new EmailRealm(String.valueOf(email.getId()));
            realmEmail.setEmail(email.getEmail());
            realmEmail.setPrimary(email.isPrimary());
            realmEmail.setVerified(email.isVerified());
            realmEmails.add(realmEmail);
        }
        xabberAccountRealm.setEmails(realmEmails);

        RealmList<SocialBindingRealm> realmSocials = new RealmList<>();
        for (SocialBindingDTO social : xabberAccount.getSocialBindings()) {
            SocialBindingRealm realmSocial = new SocialBindingRealm(String.valueOf(social.getId()));
            realmSocial.setProvider(social.getProvider());
            realmSocial.setUid(social.getUid());
            realmSocial.setFirstName(social.getFirstName());
            realmSocial.setLastName(social.getLastName());
            realmSocials.add(realmSocial);
        }
        xabberAccountRealm.setSocialBindings(realmSocials);

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

        List<EmailDTO> emails = new ArrayList<>();
        for (EmailRealm emailRealm : accountRealm.getEmails()) {
            EmailDTO email = new EmailDTO(
                    Integer.parseInt(emailRealm.getId()),
                    emailRealm.getEmail(),
                    emailRealm.isVerified(),
                    emailRealm.isPrimary());

            emails.add(email);
        }

        List<SocialBindingDTO> socials = new ArrayList<>();
        for (SocialBindingRealm socialRealm : accountRealm.getSocialBindings()) {
            SocialBindingDTO social = new SocialBindingDTO(
                    Integer.parseInt(socialRealm.getId()),
                    socialRealm.getProvider(),
                    socialRealm.getUid(),
                    socialRealm.getFirstName(),
                    socialRealm.getLastName());

            socials.add(social);
        }

        xabberAccount = new XabberAccount(
                Integer.parseInt(accountRealm.getId()),
                accountRealm.getAccountStatus(),
                accountRealm.getUsername(),
                accountRealm.getFirstName(),
                accountRealm.getLastName(),
                accountRealm.getRegisterDate(),
                accountRealm.getLanguage(),
                xmppUsers,
                emails,
                socials,
                accountRealm.getToken(),
                accountRealm.isNeedToVerifyPhone(),
                accountRealm.getPhone()
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

    @Nullable
    public Single<List<XMPPAccountSettings>> updateLocalAccounts(@Nullable List<XMPPAccountSettings> accounts) {
        if (accounts != null) {
            for (XMPPAccountSettings account : accounts) {
                updateLocalAccount(account);
            }
        }
        return Single.just(accounts);
    }

    public void updateLocalAccount(XMPPAccountSettings account) {
        // if account synced
        if (isAccountSynchronize(account.getJid()) || SettingsManager.isSyncAllAccounts()) {
            AccountJid accountJid = getExistingAccount(account.getJid());

            // create new xmpp-account
            if (accountJid == null && !account.isDeleted()) {
                try {
                    AccountJid jid = AccountManager.getInstance().addAccount(account.getJid(), "", account.getToken(), false, true, true, false, false, true);
                    AccountManager.getInstance().setColor(jid, ColorManager.getInstance().convertColorNameToIndex(account.getColor()));
                    AccountManager.getInstance().setOrder(jid, account.getOrder());
                    AccountManager.getInstance().setTimestamp(jid, account.getTimestamp());
                    AccountManager.getInstance().onAccountChanged(jid);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }

                // update existing xmpp-account
                // now we are updated only color of account
            } else if (accountJid != null && !account.isDeleted()) {
                AccountManager.getInstance().setOrder(accountJid, account.getOrder());
                AccountManager.getInstance().setTimestamp(accountJid, account.getTimestamp());
                AccountManager.getInstance().setColor(accountJid, ColorManager.getInstance().convertColorNameToIndex(account.getColor()));
                AccountManager.getInstance().onAccountChanged(accountJid);

                // delete existing account
            } else if (accountJid != null && account.isDeleted()) {
                AccountManager.getInstance().removeAccountWithoutSync(accountJid);
            }
        }
    }

    public void createLocalAccountIfNotExist() {
        Collections.sort(xmppAccountsForCreate, Collections.reverseOrder());
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (xmppAccountsForCreate != null) {
                    for (XMPPAccountSettings account : xmppAccountsForCreate) {
                        updateLocalAccount(account);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                xmppAccountsForCreate.clear();
            }
        });
    }

    public void deleteSyncedLocalAccounts() {
        for (Map.Entry<String, Boolean> entry : accountsSyncState.entrySet()) {
            AccountJid accountJid = getExistingAccount(entry.getKey());
            if (accountJid != null && entry.getValue()) AccountManager.getInstance().removeAccount(accountJid);
        }
    }

    public AccountJid getExistingAccount(String jid) {
        for (AccountJid accountJid : AccountManager.getInstance().getAllAccounts()) {
            String accountJidString = accountJid.getFullJid().asBareJid().toString();
            if (jid.equals(accountJidString)) return accountJid;
        }
        return null;
    }

    @Nullable
    public XMPPAccountSettings getAccountSettingsForSync(String jid) {
        for (XMPPAccountSettings account : xmppAccountsForSync) {
            if (account.getJid().equals(jid)) return account;
        }
        return null;
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static String getCurrentTimeString() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        Date now = new Date();
        return sdfDate.format(now);
    }

    public void onInvalidToken() {
        EventBus.getDefault().postSticky(new XabberAccountDeletedEvent());
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                deleteSyncedLocalAccounts();
                deleteSyncStatesFromRealm();
                deleteXabberAccountFromRealm();
                removeAccount();
            }
        });
    }

    public static class XabberAccountDeletedEvent {}

    public Single<List<XMPPAccountSettings>> clientSettingsDTOListToPOJO(AuthManager.ListClientSettingsDTO list) {
        List<XMPPAccountSettings> result = new ArrayList<>();

        for (AuthManager.ClientSettingsDTO dtoItem : list.getSettings()) {

            // create item
            XMPPAccountSettings accountSettings = clientSettingsDTOToPOJO(dtoItem);

            // set order
            for (AuthManager.OrderDTO orderDTO : list.getOrderData().getSettings()) {
                if (orderDTO.getJid().equals(accountSettings.getJid())) {
                    accountSettings.setOrder(orderDTO.getOrder());
                }
            }

            // add to list
            result.add(accountSettings);
        }

        // add deleted items to list
        for (AuthManager.DeletedDTO deletedDTO : list.getDeleted()) {
            XMPPAccountSettings accountSettings = new XMPPAccountSettings(deletedDTO.getJid(), true, deletedDTO.getTimestamp());
            accountSettings.setDeleted(true);
            accountSettings.setOrder(result.size() + 1);
            result.add(accountSettings);
        }

        return Single.just(result);
    }

    public XMPPAccountSettings clientSettingsDTOToPOJO(AuthManager.ClientSettingsDTO dto) {
        XMPPAccountSettings accountSettings = new XMPPAccountSettings(
                dto.getJid(),
                false,
                dto.getTimestamp()
        );
        accountSettings.setColor(dto.getSettings().getColor());
        accountSettings.setToken(dto.getSettings().getToken());
        accountSettings.setUsername(dto.getSettings().getUsername());

        return accountSettings;
    }

    public Map<String, Boolean> loadSyncStatesFromRealm() {
        Map<String, Boolean> resultMap = new HashMap<>();

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        RealmResults<SyncStateRealm> realmItems = realm.where(SyncStateRealm.class).findAll();
        for (SyncStateRealm realmItem : realmItems) {
            resultMap.put(realmItem.getJid(), realmItem.isSync());
        }
        realm.close();

        return resultMap;
    }

    public boolean deleteSyncStatesFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        final RealmResults<SyncStateRealm> results = realm.where(SyncStateRealm.class).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                success[0] = results.deleteAllFromRealm();
            }
        });
        realm.close();
        return success[0];
    }

    public void saveSyncStatesToRealm(Map<String, Boolean> syncStateMap) {

        RealmList<SyncStateRealm> realmItems = new RealmList<>();

        for (Map.Entry<String, Boolean> entry : syncStateMap.entrySet()) {
            SyncStateRealm realmItem = new SyncStateRealm();
            realmItem.setJid(entry.getKey());
            realmItem.setSync(entry.getValue());

            realmItems.add(realmItem);
        }

        // TODO: 13.03.18 ANR - WRITE
        final long startTime = System.currentTimeMillis();
        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();

        List<SyncStateRealm> oldItems = realm.where(SyncStateRealm.class).findAll();
        for (SyncStateRealm item : oldItems)
            item.deleteFromRealm();

        List<SyncStateRealm> resultRealm = realm.copyToRealmOrUpdate(realmItems);
        realm.commitTransaction();
        realm.close();
        LogManager.d("REALM", Thread.currentThread().getName()
                + " save sync state: " + (System.currentTimeMillis() - startTime));

        Log.d(LOG_TAG, resultRealm.size() + " syncState items was saved to Realm");
    }

    /**
     * @return list of settings from local account for patch to server
     */
    public List<XMPPAccountSettings> createSettingsList() {
        List<XMPPAccountSettings> resultList = new ArrayList<>();

        Collection<AccountItem> localAccounts = AccountManager.getInstance().getAllAccountItems();
        for (AccountItem localAccount : localAccounts) {
            String localJid = localAccount.getAccount().getFullJid().asBareJid().toString();
            if (SettingsManager.isSyncAllAccounts() || isAccountSynchronize(localJid)) {
                XMPPAccountSettings item = new XMPPAccountSettings(localJid, true, localAccount.getTimestamp());
                item.setOrder(localAccount.getOrder());
                item.setColor(ColorManager.getInstance().convertIndexToColorName(localAccount.getColorIndex()));
                item.setTimestamp(localAccount.getTimestamp());
                resultList.add(item);
            }
        }

        return resultList;
    }

    /**
     * @param remoteList is list of settings from cloud
     * @return list of settings for displaying in sync-dialog
     */
    public List<XMPPAccountSettings> createSyncList(List<XMPPAccountSettings> remoteList) {
        List<XMPPAccountSettings> resultList = new ArrayList<>();

        // add remote accounts to list
        for (XMPPAccountSettings remoteItem : remoteList) {
            XMPPAccountSettings.SyncStatus status = XMPPAccountSettings.SyncStatus.remote;

            // if account already exist
            AccountJid accountJid = getExistingAccount(remoteItem.getJid());
            if (accountJid != null) {
                AccountItem localItem = AccountManager.getInstance().getAccount(accountJid);
                if (localItem != null) {
                    if (remoteItem.getTimestamp() == localItem.getTimestamp())
                        status = XMPPAccountSettings.SyncStatus.localEqualsRemote;
                    else if (remoteItem.getTimestamp() > localItem.getTimestamp())
                        status = XMPPAccountSettings.SyncStatus.remoteNewer;
                    else {
                        status = XMPPAccountSettings.SyncStatus.localNewer;
                        remoteItem.setTimestamp(localItem.getTimestamp());
                    }
                    remoteItem.setSyncNotAllowed(localItem.isSyncNotAllowed());
                }

                remoteItem.setStatus(status);
                remoteItem.setSynchronization(isAccountSynchronize(remoteItem.getJid()));
                resultList.add(remoteItem);

            } else if (!remoteItem.isDeleted()){
                remoteItem.setStatus(status);
                remoteItem.setSynchronization(isAccountSynchronize(remoteItem.getJid()));
                resultList.add(remoteItem);
            }
        }

        // add local accounts to list
        Collection<AccountItem> localAccounts = AccountManager.getInstance().getAllAccountItems();
        for (AccountItem localAccount : localAccounts) {
            String localJid = localAccount.getAccount().getFullJid().asBareJid().toString();

            boolean exist = false;
            for (XMPPAccountSettings remoteItem : remoteList) {
                if (localJid.equals(remoteItem.getJid())) {
                    exist = true;
                }
            }

            if (!exist) {
                XMPPAccountSettings localItem = new XMPPAccountSettings(localJid, false, localAccount.getTimestamp());
                localItem.setOrder(remoteList.size() + localAccount.getOrder());
                localItem.setColor(ColorManager.getInstance().convertIndexToColorName(localAccount.getColorIndex()));
                localItem.setStatus(XMPPAccountSettings.SyncStatus.local);
                localItem.setSynchronization(isAccountSynchronize(localItem.getJid()));
                localItem.setSyncNotAllowed(localAccount.isSyncNotAllowed());
                resultList.add(localItem);
            }
        }


        return resultList;
    }
}

