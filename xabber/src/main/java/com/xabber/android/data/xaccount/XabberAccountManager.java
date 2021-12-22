package com.xabber.android.data.xaccount;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AccountRealmObject;
import com.xabber.android.data.database.realmobjects.EmailRealmObject;
import com.xabber.android.data.database.realmobjects.SocialBindingRealmObject;
import com.xabber.android.data.database.realmobjects.SyncStateRealmObject;
import com.xabber.android.data.database.realmobjects.XMPPUserRealmObject;
import com.xabber.android.data.database.realmobjects.XabberAccountRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.http.RetrofitErrorConverter;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.ExternalAPIs;

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
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountManager implements OnLoadListener {

    private static XabberAccountManager instance;

    private XabberAccount account;
    private BehaviorSubject<XabberAccount> accountSubject = BehaviorSubject.create();

    private List<XMPPAccountSettings> xmppAccountsForSync;
    private List<XMPPAccountSettings> xmppAccountsForCreate;

    private Map<String, Boolean> accountsSyncState;

    private int lastOrderChangeTimestamp;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private CompositeSubscription updateSettingsSubscriptions = new CompositeSubscription();
    private CompositeSubscription updateLocalSettingsSubscriptions = new CompositeSubscription();

    private XabberAccountManager() {
        accountsSyncState = new HashMap<>();
        xmppAccountsForSync = new ArrayList<>();
        xmppAccountsForCreate = new ArrayList<>();
    }

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    public static XabberAccount xabberAccountRealmToPOJO(XabberAccountRealmObject accountRealm) {
        if (accountRealm == null) return null;

        XabberAccount xabberAccount;

        List<XMPPUser> xmppUsers = new ArrayList<>();
        for (XMPPUserRealmObject xmppUserRealmObject : accountRealm.getXmppUsers()) {
            XMPPUser xmppUser = new XMPPUser(
                    Integer.parseInt(xmppUserRealmObject.getId()),
                    xmppUserRealmObject.getUsername(),
                    xmppUserRealmObject.getHost(),
                    xmppUserRealmObject.getRegistration_date());

            xmppUsers.add(xmppUser);
        }

        List<EmailDTO> emails = new ArrayList<>();
        for (EmailRealmObject emailRealmObject : accountRealm.getEmails()) {
            EmailDTO email = new EmailDTO(
                    Integer.parseInt(emailRealmObject.getId()),
                    emailRealmObject.getEmail(),
                    emailRealmObject.isVerified(),
                    emailRealmObject.isPrimary());

            emails.add(email);
        }

        List<SocialBindingDTO> socials = new ArrayList<>();
        for (SocialBindingRealmObject socialRealm : accountRealm.getSocialBindings()) {
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
                accountRealm.getDomain(),
                accountRealm.getFirstName(),
                accountRealm.getLastName(),
                accountRealm.getRegisterDate(),
                accountRealm.getLanguage(),
                xmppUsers,
                emails,
                socials,
                accountRealm.getToken(),
                accountRealm.isNeedToVerifyPhone(),
                accountRealm.getPhone(),
                accountRealm.hasPassword()
        );

        return xabberAccount;
    }

    public static String getCurrentTimeString() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        Date now = new Date();
        return sdfDate.format(now);
    }

    /**
     * Add or update synchronization values
     *
     * @param accountsSyncState
     */
    public void setAccountSyncState(Map<String, Boolean> accountsSyncState) {
        this.accountsSyncState.putAll(accountsSyncState);
        saveSyncStatesToRealm(this.accountsSyncState);
    }

    /**
     * Set synchronization for jid, if jid exist in map
     *
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
     *
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
            AccountItem accountItem = AccountManager.INSTANCE.getAccount(accountJid);
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

    @Nullable
    public List<XMPPAccountSettings> getXmppAccountsForCreate() {
        Collections.sort(xmppAccountsForCreate, Collections.reverseOrder());
        return xmppAccountsForCreate;
    }

    public void setXmppAccountsForCreate(List<XMPPAccountSettings> items) {
        this.xmppAccountsForCreate.clear();
        this.xmppAccountsForCreate.addAll(items);
    }

    public void clearXmppAccountsForCreate() {
        this.xmppAccountsForCreate.clear();
    }

    public int getLastOrderChangeTimestamp() {
        return lastOrderChangeTimestamp;
    }

    public void setLastOrderChangeTimestampIsNow() {
        this.lastOrderChangeTimestamp = (int) (System.currentTimeMillis() / 1000L);
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
                .subscribe(s -> LogManager.d(this, "XMPP accounts loading from net: successfully"),
                        throwable -> {
                            LogManager.d(this, "XMPP accounts loading from net: error: "
                                    + throwable.toString());

                            // invalid token
                            String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                            if (message != null && message.equals("Invalid token")) {
                                // save last account
                                setAccountSyncState(jid.getFullJid().asBareJid().toString(), false);
                                // logout from deleted account
                                onInvalidToken();
                            }
                        });
        compositeSubscription.add(updateSettingsSubscription);
    }

    @Override
    public void onLoad() {
        XabberAccount account = loadXabberAccountFromRealm();
        setAccount(account);

        this.lastOrderChangeTimestamp = SettingsManager.getLastOrderChangeTimestamp();

        // load sync state from realmobjects
        this.accountsSyncState = loadSyncStatesFromRealm();

        if (account != null) {
            getAccountFromNet(account.getToken(), true);
        }
    }

    private void getAccountFromNet(String token, final boolean needUpdateSettings) {
        Subscription getAccountSubscription = AuthManager.getAccount(token)
                .subscribe(xabberAccount -> handleSuccessGetAccount(xabberAccount,
                        needUpdateSettings),
                        throwable -> handleErrorGetAccount(throwable));
        compositeSubscription.add(getAccountSubscription);
    }

    /**
     * Send local Xabber account settings updates to the server.
     */
    public void updateRemoteAccountSettings() {
        List<XMPPAccountSettings> list = createSettingsList();
        if (list != null && list.size() > 0) {
            // prevents simultaneous calls
            if (!updateSettingsSubscriptions.hasSubscriptions()) {
                Subscription updateSettingsSubscription = AuthManager.patchClientSettings(list)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {
                            LogManager.d(this, "XMPP accounts loading from net: successfully");
                            updateSettingsSubscriptions.clear();
                        }, throwable -> {
                            LogManager.d(this, "XMPP accounts loading from net: error: "
                                    + throwable.toString());

                            // invalid token
                            String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                            if (message != null && message.equals("Invalid token")) {
                                // logout from deleted account
                                onInvalidToken();
                            }
                            updateSettingsSubscriptions.clear();
                        });
                updateSettingsSubscriptions.add(updateSettingsSubscription);
            }
        }
    }

    /**
     * Get Xabber account settings from the server and save them locally
     */
    public void updateLocalAccountSettings() {
        if (!updateLocalSettingsSubscriptions.hasSubscriptions()) {
            Subscription getSettingsSubscription = AuthManager.getClientSettings()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(list -> {
                        LogManager.d(this, "Account settings loaded successfully");
                        updateLocalSettingsSubscriptions.clear();
                    }, throwable -> {

                        LogManager.d(this, "XMPP accounts loading from net: error: "
                                + throwable.toString());

                        // invalid token
                        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                        if (message != null && message.equals("Invalid token")) {
                            // logout from deleted account
                            onInvalidToken();
                        }
                        updateLocalSettingsSubscriptions.clear();
                    });
            updateLocalSettingsSubscriptions.add(getSettingsSubscription);
        }
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount,
                                         boolean needUpdateSettings) {
        LogManager.d(this, "Xabber account loading from net: successfully");
        setAccount(xabberAccount);
        //if (needUpdateSettings) updateRemoteAccountSettings();
        if (needUpdateSettings) {
            if (AccountManager.INSTANCE.isLoaded()) {
                updateLocalAccountSettings();
            } else {
                AccountManager.INSTANCE.setCallAccountUpdate(true);
            }
        }
    }

    private void handleErrorGetAccount(Throwable throwable) {
        LogManager.d(this, "Xabber account loading from net: error: " + throwable.toString());

        // invalid token
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null && message.equals("Invalid token")) {
            onInvalidToken();
        }
    }

    public void deleteAccountSettings(String jid) {
        if (getAccountSyncState(jid) != null) {

            Subscription deleteSubscription = AuthManager.deleteClientSettings(jid)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleSuccessDelete, this::handleErrorDelete);
            compositeSubscription.add(deleteSubscription);
        }
    }

    private void handleSuccessDelete(List<XMPPAccountSettings> settings) {
        LogManager.d(this, "Settings deleted successfuly");
    }

    private void handleErrorDelete(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                onInvalidToken();
            } else {
                LogManager.d(this, "Error while deleting settings: " + message);
            }
        } else {
            LogManager.d(this, "Error while deleting settings: " + throwable.toString());
        }
    }

    @Nullable
    public XabberAccount getAccount() {
        return account;
    }

    private void setAccount(XabberAccount account) {
        this.account = account;
        accountSubject.onNext(this.account);
    }

    public BehaviorSubject<XabberAccount> subscribeForAccount() {
        return accountSubject;
    }


    /**
     * Remove xabber account from database and other mentions of xabber account, but
     * DOESN'T AFFECT TO XMPP ACCOUNTS
     * You may delete xmpp accounts with AccountManager.INSTANCE.removeAccount()
     */
    public void removeXabberAccount() {
        deleteXabberAccountFromRealm();
        setAccount(null);

        this.accountsSyncState.clear();
    }

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount,
                                                                  String token) {
        XabberAccount account;
        XabberAccountRealmObject xabberAccountRealmObject =
                new XabberAccountRealmObject(String.valueOf(xabberAccount.getId()));

        xabberAccountRealmObject.setToken(token);
        xabberAccountRealmObject.setAccountStatus(xabberAccount.getAccountStatus());
        xabberAccountRealmObject.setUsername(xabberAccount.getUsername());
        xabberAccountRealmObject.setDomain(xabberAccount.getDomain());
        xabberAccountRealmObject.setFirstName(xabberAccount.getFirstName());
        xabberAccountRealmObject.setLastName(xabberAccount.getLastName());
        xabberAccountRealmObject.setLanguage(xabberAccount.getLanguage());
        xabberAccountRealmObject.setRegisterDate(xabberAccount.getRegistrationDate());
        xabberAccountRealmObject.setNeedToVerifyPhone(xabberAccount.isNeedToVerifyPhone());
        xabberAccountRealmObject.setPhone(xabberAccount.getPhone());
        xabberAccountRealmObject.setHasPassword(xabberAccount.hasPassword());

        RealmList<XMPPUserRealmObject> realmUsers = new RealmList<>();
        for (XMPPUserDTO user : xabberAccount.getXmppUsers()) {
            XMPPUserRealmObject realmUser = new XMPPUserRealmObject(String.valueOf(user.getId()));
            realmUser.setUsername(user.getUsername());
            realmUser.setHost(user.getHost());
            realmUser.setRegistration_date(user.getRegistrationDate());
            realmUsers.add(realmUser);
        }
        xabberAccountRealmObject.setXmppUsers(realmUsers);

        RealmList<EmailRealmObject> realmEmails = new RealmList<>();
        for (EmailDTO email : xabberAccount.getEmails()) {
            EmailRealmObject realmEmail = new EmailRealmObject(String.valueOf(email.getId()));
            realmEmail.setEmail(email.getEmail());
            realmEmail.setPrimary(email.isPrimary());
            realmEmail.setVerified(email.isVerified());
            realmEmails.add(realmEmail);
        }
        xabberAccountRealmObject.setEmails(realmEmails);

        RealmList<SocialBindingRealmObject> realmSocials = new RealmList<>();
        for (SocialBindingDTO social : xabberAccount.getSocialBindings()) {
            SocialBindingRealmObject realmSocial =
                    new SocialBindingRealmObject(String.valueOf(social.getId()));
            realmSocial.setProvider(social.getProvider());
            realmSocial.setUid(social.getUid());
            realmSocial.setFirstName(social.getFirstName());
            realmSocial.setLastName(social.getLastName());
            realmSocials.add(realmSocial);
        }
        xabberAccountRealmObject.setSocialBindings(realmSocials);

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.beginTransaction();
        XabberAccountRealmObject accountRealm = realm.copyToRealmOrUpdate(xabberAccountRealmObject);
        account = xabberAccountRealmToPOJO(accountRealm);
        realm.commitTransaction();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        setAccount(account);
        return Single.just(account);
    }

    @Nullable
    public XabberAccount loadXabberAccountFromRealm() {
        XabberAccount xabberAccount = null;
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        RealmResults<XabberAccountRealmObject> xabberAccounts = realm
                .where(XabberAccountRealmObject.class)
                .findAll();

        for (XabberAccountRealmObject xabberAccountRealmObject : xabberAccounts) {
            xabberAccount = xabberAccountRealmToPOJO(xabberAccountRealmObject);
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            realm.close();
        }
        return xabberAccount;
    }

    boolean deleteXabberAccountFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 ->
                    success[0] = realm1.where(XabberAccountRealmObject.class)
                            .findAll()
                            .deleteAllFromRealm()
            );
        } catch (Exception e) {
            LogManager.exception(this, e);
        } finally {
            if (realm != null) realm.close();
        }
        return success[0];
    }

    @Nullable
    public Single<List<XMPPAccountSettings>> updateLocalAccounts(
            @Nullable List<XMPPAccountSettings> accounts
    ) {
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
                    AccountJid jid = AccountManager.INSTANCE.addAccount(account.getJid(),
                            "", account.getToken(), false, true,
                            true, false, false, true, false);
                    AccountManager.INSTANCE.setColor(
                            jid,
                            ColorManager.getInstance().convertColorNameToIndex(account.getColor())
                    );
                    AccountManager.INSTANCE.setOrder(jid, account.getOrder());
                    AccountManager.INSTANCE.setTimestamp(jid, account.getTimestamp());
                    AccountManager.INSTANCE.onAccountChanged(jid);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }

                // update existing xmpp-account
                // now we are updated only color of account
            } else if (accountJid != null && !account.isDeleted()) {
                AccountManager.INSTANCE.setOrder(accountJid, account.getOrder());
                AccountManager.INSTANCE.setTimestamp(accountJid, account.getTimestamp());
                if (account.getColor() != null)
                    AccountManager.INSTANCE.setColor(accountJid,
                            ColorManager.getInstance().convertColorNameToIndex(account.getColor()));
                AccountManager.INSTANCE.onAccountChanged(accountJid);

                // delete existing account
            } else if (accountJid != null && account.isDeleted()) {
                AccountManager.INSTANCE.removeAccountWithoutSync(accountJid);
            }
        }
    }

    public AccountJid getExistingAccount(String jid) {
        int slash = jid.indexOf('/');
        if (slash != -1) {
            LogManager.d(this,
                    "Got FullJid instead of BareJid to check if account already exists, " +
                            "possibly from the server. Jid = " + jid);
            LogManager.d(this, Log.getStackTraceString(new Throwable()));
            jid = jid.substring(0, slash); // make sure we compare barejids
        }
        for (AccountJid accountJid : AccountManager.INSTANCE.getAllAccounts()) {
            String accountJidString = accountJid.getFullJid().asBareJid().toString();
            if (jid.equals(accountJidString)) return accountJid;
        }
        return null;
    }

    public void onInvalidToken() {
        EventBus.getDefault().postSticky(new XabberAccountDeletedEvent());
        Application.getInstance().runInBackground(() -> {
            deleteSyncStatesFromRealm();
            deleteXabberAccountFromRealm();
            removeXabberAccount();

            for (Map.Entry<String, Boolean> entry : accountsSyncState.entrySet()) {
                if (entry.getValue())
                    try {
                        AccountManager.INSTANCE.removeAccount(AccountJid.from(entry.getKey()));
                    } catch (Exception e) {
                        LogManager.exception(this, e);
                    }
            }

        });
        unregisterEndpoint();
    }

    public Single<List<XMPPAccountSettings>> clientSettingsDTOListToPOJO(
            AuthManager.ListClientSettingsDTO list) {
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
            XMPPAccountSettings accountSettings =
                    new XMPPAccountSettings(deletedDTO.getJid(), true, deletedDTO.getTimestamp());
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

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<SyncStateRealmObject> realmItems = realm.where(SyncStateRealmObject.class)
                .findAll();

        for (SyncStateRealmObject realmItem : realmItems)
            resultMap.put(realmItem.getJid(), realmItem.isSync());

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return resultMap;
    }

    public boolean deleteSyncStatesFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 -> {
                success[0] = realm1.where(SyncStateRealmObject.class)
                        .findAll()
                        .deleteAllFromRealm();
            });
        } catch (Exception e) {
            LogManager.exception(this, e);
        } finally {
            if (realm != null) realm.close();
        }
        return success[0];
    }

    public void saveSyncStatesToRealm(Map<String, Boolean> syncStateMap) {

        RealmList<SyncStateRealmObject> realmItems = new RealmList<>();

        for (Map.Entry<String, Boolean> entry : syncStateMap.entrySet()) {
            SyncStateRealmObject realmItem = new SyncStateRealmObject();
            realmItem.setJid(entry.getKey());
            realmItem.setSync(entry.getValue());

            realmItems.add(realmItem);
        }

        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> {
                    List<AccountRealmObject> accounts = realm1
                            .where(AccountRealmObject.class)
                            .findAll();

                    String jid;
                    if (accounts.isEmpty()) {
                        realmItems.deleteAllFromRealm();
                    } else {
                        for (AccountRealmObject account : accounts) {
                            jid = account.getUserName() + "@" + account.getServerName();
                            SyncStateRealmObject syncStateRealmObjects =
                                    realm1.where(SyncStateRealmObject.class)
                                            .equalTo(SyncStateRealmObject.Fields.JID, jid)
                                            .findFirst();
                            if (syncStateRealmObjects != null)
                                realm1.copyToRealmOrUpdate(syncStateRealmObjects);
                        }
                    }
                });
            } catch (Exception e) {
                LogManager.exception(this, e);
            }
        });
    }

    /**
     * @return list of settings from local account for patch to server
     */
    public List<XMPPAccountSettings> createSettingsList() {
        List<XMPPAccountSettings> resultList = new ArrayList<>();

        Collection<AccountItem> localAccounts = AccountManager.INSTANCE.getAllAccountItems();
        for (AccountItem localAccount : localAccounts) {
            String localJid = localAccount.getAccount().getFullJid().asBareJid().toString();
            if (SettingsManager.isSyncAllAccounts() || isAccountSynchronize(localJid)) {
                XMPPAccountSettings item =
                        new XMPPAccountSettings(localJid, true, localAccount.getTimestamp());
                item.setOrder(localAccount.getOrder());
                item.setColor(ColorManager.getInstance()
                        .convertIndexToColorName(localAccount.getColorIndex()));
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
                AccountItem localItem = AccountManager.INSTANCE.getAccount(accountJid);
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

            } else if (!remoteItem.isDeleted()) {
                remoteItem.setStatus(status);
                remoteItem.setSynchronization(isAccountSynchronize(remoteItem.getJid()));
                resultList.add(remoteItem);
            }
        }

        // add local accounts to list
        Collection<AccountItem> localAccounts = AccountManager.INSTANCE.getAllAccountItems();
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
                localItem.setColor(ColorManager.getInstance()
                        .convertIndexToColorName(localAccount.getColorIndex()));
                localItem.setStatus(XMPPAccountSettings.SyncStatus.local);
                localItem.setSynchronization(isAccountSynchronize(localItem.getJid()));
                localItem.setSyncNotAllowed(localAccount.isSyncNotAllowed());
                resultList.add(localItem);
            }
        }


        return resultList;
    }

    public void registerEndpoint() {
        String token = ExternalAPIs.getPushEndpointToken();
        if (token == null) return;

        compositeSubscription.add(
                AuthManager.registerFCMEndpoint(token)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                responseBody -> LogManager.d(this, "Endpoint successfully registered"),
                                throwable -> LogManager.d(this, "Endpoint register failed: "
                                        + throwable.toString())));
    }

    public void unregisterEndpoint() {
        String token = ExternalAPIs.getPushEndpointToken();
        if (token == null) return;

        compositeSubscription.add(
                AuthManager.unregisterFCMEndpoint(token)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(responseBody ->
                                        LogManager.d(this, "Endpoint successfully unregistered"),
                                throwable -> LogManager.d(this, "Endpoint unregister failed: "
                                        + throwable.toString())));
    }

    public static class XabberAccountDeletedEvent {
    }

}

