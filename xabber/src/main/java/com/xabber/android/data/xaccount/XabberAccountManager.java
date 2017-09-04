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
import com.xabber.android.data.database.realm.XMPPAccountSettignsRealm;
import com.xabber.android.data.database.realm.XMPPUserRealm;
import com.xabber.android.data.database.realm.XabberAccountRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.stringprep.XmppStringprepException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    private int lastOrderChangeTimestamp;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static XabberAccountManager getInstance() {
        if (instance == null)
            instance = new XabberAccountManager();
        return instance;
    }

    private XabberAccountManager() {
        xmppAccounts = new ArrayList<>();
    }

    public int getLastOrderChangeTimestamp() {
        return lastOrderChangeTimestamp;
    }

    @Override
    public void onLoad() {
        XabberAccount account = loadXabberAccountFromRealm();
        this.account = account;

        this.lastOrderChangeTimestamp = SettingsManager.getLastOrderChangeTimestamp();
        this.xmppAccounts = loadXMPPAccountSettingsFromRealm();

        // add xmpp settings for local account if not exist
        addSettingsFromLocalAccounts();

        if (account != null) {
            getAccountFromNet(account.getToken());
        }
    }

    public void addSettingsFromLocalAccounts() {
        Collection<AccountItem> items = AccountManager.getInstance().getAllAccountItems();
        if (items == null) return;

        List<XMPPAccountSettings> settingsList = new ArrayList<>();

        for (AccountItem accountItem : items) {
            String jid = accountItem.getAccount().getFullJid().asBareJid().toString();
            if (getAccountSettings(jid) == null) {
                XMPPAccountSettings accountSettings = new XMPPAccountSettings(
                        jid,
                        true,
                        getCurrentTime());
                accountSettings.setColor(ColorManager.getInstance().convertIndexToColorName(accountItem.getColorIndex()));
                settingsList.add(accountSettings);
            }
        }

        if (settingsList.size() > 0) {
            saveOrUpdateXMPPAccountSettingsToRealm(settingsList);
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

    private void updateAccountSettings() {
        Subscription updateSettingsSubscription = AuthManager.updateClientSettings(this.xmppAccounts)
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
                    }
                });
        compositeSubscription.add(updateSettingsSubscription);
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        Log.d(LOG_TAG, "Xabber account loading from net: successfully");
        this.account = xabberAccount;
        updateAccountSettings();
    }

    private void handleErrorGetAccount(Throwable throwable) {
        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());
        // TODO: 24.07.17 need login
    }

//    private void getSettingsFromNet() {
//        Subscription getSettingsSubscription = AuthManager.getClientSettings()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<List<XMPPAccountSettings>>() {
//                    @Override
//                    public void call(List<XMPPAccountSettings> s) {
//                        //updateXmppAccounts(s);
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
//                    }
//                });
//        compositeSubscription.add(getSettingsSubscription);
//    }

    private void updateXmppAccounts(List<XMPPAccountSettings> items) {
        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
        this.xmppAccounts.clear();
        this.xmppAccounts.addAll(items);
    }

    public List<XMPPAccountSettings> getXmppAccounts() {
        return xmppAccounts;
    }

    public void addXmppAccountSettings(final AccountItem accountItem, final boolean sync) {
        String jid = accountItem.getAccount().getFullJid().asBareJid().toString();
        if (getAccountSettings(jid) == null) {

            XMPPAccountSettings accountSettings = new XMPPAccountSettings(
                    jid,
                    sync,
                    getCurrentTime());
            accountSettings.setColor(ColorManager.getInstance().convertIndexToColorName(accountItem.getColorIndex()));

            Subscription addAccountSettingsSubscription = saveOrUpdateXMPPAccountSettingsToRealm(accountSettings)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<XMPPAccountSettings>() {
                        @Override
                        public void call(XMPPAccountSettings s) {
                            if (sync) updateAccountSettings();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.d(LOG_TAG, "add xmpp account settings: error: " + throwable.toString());
                        }
                    });
            compositeSubscription.add(addAccountSettingsSubscription);
        }
    }

    public void saveSettingsToRealm() {
        Subscription saveSettingsSubscription = saveOrUpdateXMPPAccountSettingsToRealm(this.xmppAccounts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        updateAccountSettings();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "add xmpp account settings: error: " + throwable.toString());
                    }
                });
        compositeSubscription.add(saveSettingsSubscription);
    }

    @Nullable
    public XabberAccount getAccount() {
        return account;
    }

    public void removeAccount() {
        this.account = null;
    }

    public Single<XabberAccount> saveOrUpdateXabberAccountToRealm(XabberAccountDTO xabberAccount, String token) {
        XabberAccount account;
        XabberAccountRealm xabberAccountRealm = new XabberAccountRealm(String.valueOf(xabberAccount.getId()));

        xabberAccountRealm.setToken(token);
        xabberAccountRealm.setAccountStatus(xabberAccount.getAccountStatus());
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
                xmppUsers,
                emails,
                socials,
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

    public boolean deleteDeadXMPPAccountsFromRealm() {
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        final RealmResults<XMPPAccountSettignsRealm> results = realm.where(XMPPAccountSettignsRealm.class).findAll();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (XMPPAccountSettignsRealm set : results) {
                    AccountJid accountJid = getExistingAccount(set.getJid());
                    if (accountJid == null) {
                        set.deleteFromRealm();
                    }
                }
            }
        });
        realm.close();
        return true;
    }

    public boolean deleteSyncedXMPPAccountsFromRealm() {
        final boolean[] success = new boolean[1];
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        final RealmResults<XMPPAccountSettignsRealm> results = realm.where(XMPPAccountSettignsRealm.class).equalTo("synchronization", true).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                success[0] = results.deleteAllFromRealm();
            }
        });
        realm.close();
        return success[0];
    }

    public List<XMPPAccountSettings> loadXMPPAccountSettingsFromRealm() {
        List<XMPPAccountSettings> result = null;

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        RealmResults<XMPPAccountSettignsRealm> realmItems = realm.where(XMPPAccountSettignsRealm.class).findAll();
        result = xmppAccountSettingsRealmListToPOJO(realmItems);

        realm.close();
        return result;
    }

    public List<XMPPAccountSettings> loadXMPPAccountSettingsFromRealmUI() {
        List<XMPPAccountSettings> result = null;

        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<XMPPAccountSettignsRealm> realmItems = realm.where(XMPPAccountSettignsRealm.class).findAll();
        result = xmppAccountSettingsRealmListToPOJO(realmItems);

        realm.close();
        return result;
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
                //realmItem.setOrder(valuesDTO.getOrder());
                realmItem.setUsername(valuesDTO.getUsername());
            }

            for (AuthManager.OrderDTO orderDTO : listXMPPAccountSettings.getOrderData().getSettings()) {
                if (orderDTO.getJid().equals(realmItem.getJid())) {
                    realmItem.setOrder(orderDTO.getOrder());
                }
            }

            realmItem.setTimestamp(dtoItem.getTimestamp());

            // add to sync only accounts required sync
            XMPPAccountSettings accountSettings = getAccountSettings(dtoItem.getJid());
            if (accountSettings != null) {
                if (accountSettings.isSynchronization() || SettingsManager.isSyncAllAccounts()) {
                    realmItem.setSynchronization(accountSettings.isSynchronization());
                    realmItems.add(realmItem);
                }
            } else {
                realmItem.setSynchronization(true);
                realmItems.add(realmItem);
            }
        }

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        realm.beginTransaction();
        List<XMPPAccountSettignsRealm> resultRealm = realm.copyToRealmOrUpdate(realmItems);
        result = xmppAccountSettingsRealmListToPOJO(resultRealm);
        realm.commitTransaction();
        realm.close();

        SettingsManager.setLastSyncDate(getCurrentTimeString());
        updateXmppAccounts(loadXMPPAccountSettingsFromRealm());
        return Single.just(result);
    }

    @Nullable
    public Single<List<XMPPAccountSettings>> saveOrUpdateXMPPAccountSettingsToRealm(List<XMPPAccountSettings> items) {
        List<XMPPAccountSettings> result;
        RealmList<XMPPAccountSettignsRealm> realmItems = new RealmList<>();

        for (XMPPAccountSettings item : items) {
            XMPPAccountSettignsRealm realmItem = new XMPPAccountSettignsRealm(item.getJid());

            realmItem.setToken(item.getToken());
            realmItem.setColor(item.getColor());
            realmItem.setOrder(item.getOrder());
            realmItem.setUsername(item.getUsername());
            realmItem.setTimestamp(item.getTimestamp());
            realmItem.setSynchronization(item.isSynchronization());

            realmItems.add(realmItem);
        }

        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        List<XMPPAccountSettignsRealm> resultRealm = realm.copyToRealmOrUpdate(realmItems);
        result = xmppAccountSettingsRealmListToPOJO(resultRealm);
        realm.commitTransaction();
        realm.close();

        updateXmppAccounts(result);
        return Single.just(result);
    }

    public Single<XMPPAccountSettings> saveOrUpdateXMPPAccountSettingsToRealm(XMPPAccountSettings item) {

        XMPPAccountSettignsRealm realmItem = new XMPPAccountSettignsRealm(item.getJid());

        realmItem.setToken(item.getToken());
        realmItem.setColor(item.getColor());
        realmItem.setOrder(item.getOrder());
        realmItem.setUsername(item.getUsername());
        realmItem.setTimestamp(item.getTimestamp());
        realmItem.setSynchronization(item.isSynchronization());

        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        XMPPAccountSettignsRealm resultRealm = realm.copyToRealmOrUpdate(realmItem);
        XMPPAccountSettings result = xmppAccountSettingsRealmToPOJO(resultRealm);
        realm.commitTransaction();
        realm.close();

        updateXmppAccounts(loadXMPPAccountSettingsFromRealmUI());
        if (result != null) return Single.just(result);
        else return Single.error(new Throwable("Create realm object error"));
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
        item.setSynchronization(realmItem.isSynchronization());

        return item;
    }

    @Nullable
    public Single<List<XMPPAccountSettings>> updateLocalAccounts(@Nullable List<XMPPAccountSettings> accounts) {
        if (accounts != null) {
            for (XMPPAccountSettings account : accounts) {
                AccountJid accountJid = getExistingAccount(account.getJid());
                if (accountJid == null) {
                    // create new xmpp-account
//                    try {
//                        AccountManager.getInstance().addAccount(account.getJid(), "", false, true, true, false, false);
//                    } catch (NetworkException e) {
//                        Application.getInstance().onError(e);
//                    }
                } else {
                    // update existing xmpp-account
                    // now we are updated only color of account
                    AccountManager.getInstance().setColor(accountJid, ColorManager.getInstance().convertColorNameToIndex(account.getColor()));
                    AccountManager.getInstance().onAccountChanged(accountJid);
                }
            }
        }
        return Single.just(accounts);
    }

    public void createLocalAccountIfNotExist() {
        if (this.xmppAccounts != null) {
            for (XMPPAccountSettings account : this.xmppAccounts) {
                if (account.isSynchronization() || SettingsManager.isSyncAllAccounts()) {
                    AccountJid localAccountJid = getExistingAccount(account.getJid());
                    if (localAccountJid == null) {
                        // create new xmpp-account
                        try {
                            AccountJid accountJid = AccountManager.getInstance().addAccount(account.getJid(), "", account.getToken(), false, true, true, false, false);
                            AccountManager.getInstance().setColor(accountJid, ColorManager.getInstance().convertColorNameToIndex(account.getColor()));
                            AccountManager.getInstance().onAccountChanged(accountJid);
                        } catch (NetworkException e) {
                            Application.getInstance().onError(e);
                        }
                    }
                }
            }
        }
    }

    public AccountJid getExistingAccount(String jid) {
        for (AccountJid accountJid : AccountManager.getInstance().getAllAccounts()) {
            String accountJidString = accountJid.getFullJid().asBareJid().toString();
            if (jid.equals(accountJidString)) return accountJid;
        }
        return null;
    }

    public void setColor(AccountJid accountJid, int colorIndex) {
        if (accountJid != null) {
            for (XMPPAccountSettings account : xmppAccounts) {
                if (account.getJid().equals(accountJid.getFullJid().asBareJid().toString())) {
                    account.setColor(ColorManager.getInstance().convertIndexToColorName(colorIndex));
                    account.setTimestamp(getCurrentTime());
                }
            }
            saveSettingsToRealm();
        }
    }

    public void setXMPPAccountOrder(HashMap<String, Integer> items) {
        for (XMPPAccountSettings account : xmppAccounts) {
            if (items.containsKey(account.getJid())) {
                int orderValue = items.get(account.getJid());
                if (orderValue > 0) account.setOrder(orderValue);
                //account.setTimestamp(getCurrentTime());
            }
        }
        lastOrderChangeTimestamp = getCurrentTime();
        SettingsManager.setLastOrderChangeTimestamp(lastOrderChangeTimestamp);
        saveSettingsToRealm();
    }

    @Nullable
    public XMPPAccountSettings getAccountSettings(String jid) {
        for (XMPPAccountSettings account : xmppAccounts) {
            if (account.getJid().equals(jid)) return account;
        }
        return null;
    }

    public void setSyncForAccount(AccountJid account, boolean sync) {
        BareJid bareJid = account.getFullJid().asBareJid();
        if (bareJid != null) {
            for (XMPPAccountSettings accountSettings : xmppAccounts) {
                if (accountSettings.getJid().equals(bareJid.toString())) {
                    accountSettings.setSynchronization(sync);
                }
            }
            saveSettingsToRealm();
        }
    }

    public void setSyncAllAccounts(List<XMPPAccountSettings> items) {
        for (XMPPAccountSettings account : xmppAccounts) {
            for (XMPPAccountSettings accountNew : items) {
                if (account.getJid().equals(accountNew.getJid()))
                    account.setSynchronization(accountNew.isSynchronization());
            }
        }
        saveOrUpdateXMPPAccountSettingsToRealm(items);
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static String getCurrentTimeString() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        Date now = new Date();
        return sdfDate.format(now);
    }

    public void deleteAllSyncedAccounts() {
        for (Iterator<XMPPAccountSettings> it = this.xmppAccounts.iterator(); it.hasNext(); ) {
            XMPPAccountSettings accountSettings = it.next();
            if (accountSettings.isSynchronization()) {
                // delete local accounts
                AccountJid localAccountJid = getExistingAccount(accountSettings.getJid());
                if (localAccountJid != null) {
                    AccountManager.getInstance().removeAccountWithoutSync(localAccountJid);
                }

                // delete local settings
                it.remove();
            }
        }
    }
}

