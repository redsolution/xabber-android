/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.account;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnUnloadListener;
import com.xabber.android.data.connection.ConnectionSettings;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.AccountRealm;
import com.xabber.android.data.extension.mam.LoadHistorySettings;
import com.xabber.android.data.extension.mam.MamManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnWipeListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountAddedListener;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.account.listeners.OnAccountDisabledListener;
import com.xabber.android.data.account.listeners.OnAccountEnabledListener;
import com.xabber.android.data.account.listeners.OnAccountOfflineListener;
import com.xabber.android.data.account.listeners.OnAccountOnlineListener;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.account.listeners.OnAccountSyncableChangedListener;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.ReconnectionManager;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.database.sqlite.AccountTable;
import com.xabber.android.data.database.sqlite.StatusTable;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.notification.BaseAccountNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XabberAccountManager;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * This class manage all operations with accounts.
 * <p/>
 * <p/>
 * Each account has unique full jid (userName@serverName/resource). This jid is
 * persistent and independent from real jid assigned by server. Real full jid
 * (assigned by server) of account can be taken by
 * {@link AccountItem#getRealJid()}.
 *
 * @author alexander.ivanov
 */
public class AccountManager implements OnLoadListener, OnUnloadListener, OnWipeListener {

    private static final String LOG_TAG = AccountManager.class.getSimpleName();

    private static AccountManager instance;

    /**
     * List of saved statuses.
     */
    private final Collection<SavedStatus> savedStatuses;
    /**
     * Number of different account colors.
     */
    private final int colors;
    /**
     * List of accounts.
     */
    private final Map<AccountJid, AccountItem> accountItems;
    private final BaseAccountNotificationProvider<AccountError> accountErrorProvider;

    private final Application application;
    /**
     * Whether away status mode is enabled.
     */
    private boolean away;
    /**
     * Whether extended away mode is enabled.
     */
    private boolean xa;

    public static AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }

        return instance;
    }

    private AccountManager() {
        this.application = Application.getInstance();
        accountItems = new HashMap<>();
        savedStatuses = new ArrayList<>();
        accountErrorProvider = new BaseAccountNotificationProvider<>(R.drawable.ic_stat_error);

        colors = application.getResources().getIntArray(R.array.account_color_names).length;

        away = false;
        xa = false;
    }

    @Override
    public void onLoad() {
        final Collection<SavedStatus> savedStatuses = loadSavedStatuses();


        final Collection<AccountItem> accountItems = new ArrayList<>();
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();

        RealmResults<AccountRealm> accountRealms = realm.where(AccountRealm.class).findAll();

        LogManager.i(LOG_TAG, "onLoad got realm accounts: " + accountRealms.size());

        for (AccountRealm accountRealm : accountRealms) {
            DomainBareJid serverName = null;
            try {
                serverName = JidCreate.domainBareFrom(accountRealm.getServerName());
            } catch (XmppStringprepException e) {
                LogManager.exception(this, e);
            }

            Localpart userName = null;
            try {
                userName = Localpart.from(accountRealm.getUserName());
            } catch (XmppStringprepException e) {
                LogManager.exception(this, e);
            }

            Resourcepart resource = null;
            try {
                resource = Resourcepart.from(accountRealm.getResource());
            } catch (XmppStringprepException e) {
                LogManager.exception(this, e);
            }

            if (serverName == null || userName == null || resource == null) {
                LogManager.e(LOG_TAG, "could not create account. username " + userName
                        + ", server name " + serverName
                        + ", resource " + resource);
                continue;
            }

            // fix for db migration
            int order = accountRealm.getOrder();
            if (order == 0) {
                for (AccountItem item : accountItems) {
                    if (item.getOrder() > order) order = item.getOrder();
                }
                order++;
            }

            AccountItem accountItem = new AccountItem(
                    accountRealm.isCustom(),
                    accountRealm.getHost(),
                    accountRealm.getPort(),
                    serverName,
                    userName,
                    resource,
                    accountRealm.isStorePassword(),
                    accountRealm.getPassword(),
                    accountRealm.getToken(),
                    accountRealm.getColorIndex(),
                    order,
                    accountRealm.isSyncNotAllowed(),
                    accountRealm.getTimestamp(),
                    accountRealm.getPriority(),
                    accountRealm.getStatusMode(),
                    accountRealm.getStatusText(),
                    accountRealm.isEnabled(),
                    accountRealm.isSaslEnabled(),
                    accountRealm.getTlsMode(),
                    accountRealm.isCompression(),
                    accountRealm.getProxyType(),
                    accountRealm.getProxyHost(),
                    accountRealm.getProxyPort(),
                    accountRealm.getProxyUser(),
                    accountRealm.getProxyPassword(),
                    accountRealm.isSyncable(),
                    accountRealm.getKeyPair(),
                    accountRealm.getLastSync(),
                    accountRealm.getArchiveMode());
            accountItem.setId(accountRealm.getId());
            accountItem.setClearHistoryOnExit(accountRealm.isClearHistoryOnExit());
            if (accountRealm.getMamDefaultBehavior() != null) {
                accountItem.setMamDefaultBehaviour(accountRealm.getMamDefaultBehavior());
            }
            if (accountRealm.getLoadHistorySettings() != null) {
                accountItem.setLoadHistorySettings(accountRealm.getLoadHistorySettings());
            }
            accountItem.setSuccessfulConnectionHappened(accountRealm.isSuccessfulConnectionHappened());

            accountItems.add(accountItem);

        }

        realm.close();

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(savedStatuses, accountItems);
            }
        });
    }

    @NonNull
    private Collection<SavedStatus> loadSavedStatuses() {
        final Collection<SavedStatus> savedStatuses = new ArrayList<>();
        Cursor cursor = StatusTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    savedStatuses.add(new SavedStatus(StatusTable.getStatusMode(cursor),
                            StatusTable.getStatusText(cursor)));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return savedStatuses;
    }

    private void onLoaded(Collection<SavedStatus> savedStatuses, Collection<AccountItem> accountItems) {
        this.savedStatuses.addAll(savedStatuses);
        for (AccountItem accountItem : accountItems) {
            addAccount(accountItem);
        }
        NotificationManager.getInstance().registerNotificationProvider(accountErrorProvider);
    }

    private void addAccount(AccountItem accountItem) {
        accountItems.put(accountItem.getAccount(), accountItem);
        for (OnAccountAddedListener listener : application.getManagers(OnAccountAddedListener.class)) {
            listener.onAccountAdded(accountItem);
        }
        if (accountItem.isEnabled()) {
            onAccountEnabled(accountItem);
            if (accountItem.getRawStatusMode().isOnline()) {
                onAccountOnline(accountItem);
            }
        }
        onAccountChanged(accountItem.getAccount());
    }

    /**
     * @return Next color index for the next account.
     */
    int getNextColorIndex() {
        int[] count = new int[colors];
        for (AccountItem accountItem : accountItems.values()) {
            count[accountItem.getColorIndex() % colors] += 1;
        }
        int result = 0;
        int value = count[0];
        for (int index = 0; index < count.length; index++) {
            if (count[index] < value) {
                result = index;
            }
        }
        return result;
    }

    int getNextOrder() {
        int max = 0;
        for (AccountItem item : accountItems.values()) {
            if (item.getOrder() > max) max = item.getOrder();
        }
        return max + 1;
    }

    /**
     * @param account full jid.
     * @return Specified account or <code>null</code> if account doesn't exists.
     */
    @Nullable
    public AccountItem getAccount(AccountJid account) {
        return accountItems.get(account);
    }

    /**
     * Save account item to database.
     */
    void requestToWriteAccount(final AccountItem accountItem) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                AccountTable.getInstance().write(accountItem.getId(), accountItem);
            }
        });
    }

    /**
     * Creates new account and starts connection.
     */
    private AccountItem addAccount(boolean custom, String host, int port,
                                   DomainBareJid serverName, Localpart userName, boolean storePassword,
                                   String password, String token, Resourcepart resource, int color, int order, boolean syncNotAllowed, int timestamp, int priority,
                                   StatusMode statusMode, String statusText, boolean enabled,
                                   boolean saslEnabled, TLSMode tlsMode, boolean compression,
                                   ProxyType proxyType, String proxyHost, int proxyPort,
                                   String proxyUser, String proxyPassword, boolean syncable,
                                   KeyPair keyPair, Date lastSync, ArchiveMode archiveMode,
                                   boolean registerNewAccount) {

        AccountItem accountItem = new AccountItem(custom, host, port, serverName, userName,
                resource, storePassword, password, token, color, order, syncNotAllowed, timestamp, priority, statusMode, statusText, enabled,
                saslEnabled, tlsMode, compression, proxyType, proxyHost, proxyPort, proxyUser,
                proxyPassword, syncable, keyPair, lastSync, archiveMode);

        requestToWriteAccount(accountItem);
        addAccount(accountItem);
        ReconnectionManager.getInstance().requestReconnect(accountItem.getAccount());
        return accountItem;
    }

    /**
     * Creates new account.
     *
     * @param user full or bare jid.
     * @return assigned account name.
     * @throws NetworkException if user or server part are invalid.
     */
    public AccountJid addAccount(String user, String password, String token, boolean syncable,
                                 boolean storePassword, boolean xabberSync, boolean useOrbot, boolean registerNewAccount, boolean enabled)
            throws NetworkException {
        if (user == null) {
            throw new NetworkException(R.string.EMPTY_USER_NAME);
        }

        DomainBareJid serverName;
        try {
            serverName = JidCreate.domainBareFrom(user);
        } catch (XmppStringprepException e) {
            throw new NetworkException(R.string.INCORRECT_USER_NAME);
        }
        Localpart userName;
        try {
            userName = Localpart.from(XmppStringUtils.parseLocalpart(user));
        } catch (XmppStringprepException e) {
            throw new NetworkException(R.string.INCORRECT_USER_NAME);
        }

        Resourcepart resource = null;
        String resourceString = XmppStringUtils.parseResource(user).trim();
        if (!TextUtils.isEmpty(resourceString)) {
            try {
                resource = Resourcepart.from(resourceString);
            } catch (XmppStringprepException e) {
                LogManager.exception(this, e);
            }
        }
        String host = serverName.getDomain().toString();
        int port = 5222;
        boolean tlsRequired = false;
        if (useOrbot) {
            tlsRequired = true;
        }

        if (resource == null) {
            resource = generateResource();
        }

        AccountItem accountItem;
        while(true) {

            if (getAccount(AccountJid.from(userName, serverName, resource)) == null) {
                break;
            }
            resource = generateResource();
        }


        boolean useCustomHost = application.getResources().getBoolean(R.bool.account_use_custom_host_default);

        boolean useCompression = application.getResources().getBoolean(R.bool.account_use_compression_default);

        ArchiveMode archiveMode = ArchiveMode.valueOf(application.getString(R.string.account_archive_mode_default_value));

        accountItem = addAccount(useCustomHost, host, port, serverName, userName,
                storePassword, password, token, resource, getNextColorIndex(), getNextOrder(), false,
                XabberAccountManager.getInstance().getCurrentTime(), 0, StatusMode.available,
                SettingsManager.statusText(), enabled, true, tlsRequired ? TLSMode.required : TLSMode.enabled,
                useCompression, useOrbot ? ProxyType.orbot : ProxyType.none, "localhost", 8080,
                "", "", syncable, null, null, archiveMode, registerNewAccount);
        if (accountItem == null) {
            throw new NetworkException(R.string.ACCOUNT_REGISTER_FAILED);
        }

        onAccountChanged(accountItem.getAccount());
        if (accountItems.size() > 1 && SettingsManager.contactsEnableShowAccounts()) {
            SettingsManager.enableContactsShowAccount();
        }

        // add xmpp account settings
        if (xabberSync) XabberAccountManager.getInstance()
                .addAccountSyncState(accountItem.getAccount().getFullJid().asBareJid().toString(), true);
        else SettingsManager.setSyncAllAccounts(false);

        return accountItem.getAccount();
    }

    @NonNull
    private Resourcepart generateResource() {
        try {
            return Resourcepart.from(application.getString(R.string.account_resource_default) + "_" + StringUtils.randomString(8));
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            return Resourcepart.EMPTY;
        }
    }

    /**
     * Remove user`s account. Don't call any callbacks.
     */
    private void removeAccountWithoutCallback(final AccountJid account) {
        final AccountItem accountItem = getAccount(account);
        if (accountItem == null) {
            return;
        }

        boolean wasEnabled = accountItem.isEnabled();
        accountItem.setEnabled(false);
        accountItem.disconnect();
        if (wasEnabled) {
            if (accountItem.getRawStatusMode().isOnline()) {
                onAccountOffline(accountItem);
            }
            onAccountDisabled(accountItem);
        }

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                AccountTable.getInstance().remove(account, accountItem.getId());
            }
        });

        accountItems.remove(account);
        for (OnAccountRemovedListener listener : application.getManagers(OnAccountRemovedListener.class)) {
            listener.onAccountRemoved(accountItem);
        }
        removeAccountError(account);
    }

    /**
     * Remove user`s account.
     */
    public void removeAccount(AccountJid account) {
        // disable synchronization for this account in xabber account
        SettingsManager.setSyncAllAccounts(false);
        XabberAccountManager.getInstance().setAccountSyncState(account.getFullJid().asBareJid().toString(), false);

        // removing local account
        removeAccountWithoutCallback(account);
        onAccountChanged(account);
    }

    /**
     * Remove user`s account.
     * without set sync for account
     */
    public void removeAccountWithoutSync(AccountJid account) {
        removeAccountWithoutCallback(account);
        onAccountChanged(account);
    }

    public void updateAccountPassword(AccountJid account, String pass) {

        AccountItem result = getAccount(account);

        if (result == null) {
            return;
        }

        result.setPassword(pass);
        result.recreateConnectionWithEnable(result.getAccount());
        requestToWriteAccount(result);
    }

    /**
     * Update user`s account.
     * <p/>
     * It will reconnect to the server if changes was made.
     * <p/>
     * It will remove old account and create new one if full jid was changed.
     *
     * @param account       full source jid
     */
    public void updateAccount(AccountJid account, boolean custom, String host, int port, DomainBareJid serverName,
                              Localpart userName, boolean storePassword, String password, String token, Resourcepart resource,
                              int priority, boolean enabled, boolean saslEnabled, TLSMode tlsMode,
                              boolean compression, ProxyType proxyType, String proxyHost, int proxyPort,
                              String proxyUser, String proxyPassword, boolean syncable,
                              ArchiveMode archiveMode, int colorIndex) {
        AccountItem result;
        AccountItem accountItem = getAccount(account);

        if (accountItem == null) {
            return;
        }

        if (accountItem.getConnectionSettings().getServerName().equals(serverName)
                && accountItem.getConnectionSettings().getUserName().equals(userName)
                && accountItem.getConnectionSettings().getResource().equals(resource)) {
            result = accountItem;

            result.setColorIndex(colorIndex);

            boolean reconnect = false;
            if (accountItem.getConnectionSettings().isCustomHostAndPort() != custom
                    || !accountItem.getConnectionSettings().getHost().equals(host)
                    || accountItem.getConnectionSettings().getPort() != port
                    || !accountItem.getConnectionSettings().getPassword().equals(password)
                    || accountItem.getConnectionSettings().getTlsMode() != tlsMode
                    || accountItem.getConnectionSettings().isSaslEnabled() != saslEnabled
                    || accountItem.getConnectionSettings().useCompression() != compression
                    || accountItem.getConnectionSettings().getProxyType() != proxyType
                    || !accountItem.getConnectionSettings().getProxyHost().equals(proxyHost)
                    || accountItem.getConnectionSettings().getProxyPort() != proxyPort
                    || !accountItem.getConnectionSettings().getProxyUser().equals(proxyUser)
                    || !accountItem.getConnectionSettings().getProxyPassword().equals(proxyPassword)) {
                result.updateConnectionSettings(custom, host, port, password, saslEnabled, tlsMode,
                        compression, proxyType, proxyHost, proxyPort, proxyUser, proxyPassword);
                reconnect = true;
            }
            if (result.isSyncable() != syncable) {
                result.setSyncable(syncable);
                for (OnAccountSyncableChangedListener listener :
                        application.getManagers(OnAccountSyncableChangedListener.class)) {
                    listener.onAccountSyncableChanged(result);
                }
            }
            result.setStorePassword(storePassword);
            boolean changed = result.isEnabled() != enabled;
            result.setEnabled(enabled);
            if (result.getPriority() != priority) {
                result.setPriority(priority);
                try {
                    PresenceManager.getInstance().resendPresence(account);
                } catch (NetworkException e) {
                    LogManager.exception(this, e);
                }
            }
            if (result.getArchiveMode() != archiveMode) {
                result.setArchiveMode(archiveMode);
            }
            if (changed && enabled) {
                onAccountEnabled(result);
                if (result.getRawStatusMode().isOnline()) {
                    onAccountOnline(result);
                }
            }
            if (changed || reconnect) {
                result.setSuccessfulConnectionHappened(false);
                result.recreateConnection();
            }
            if (changed && !enabled) {
                if (result.getRawStatusMode().isOnline()) {
                    onAccountOffline(result);
                }
                onAccountDisabled(result);
            }
            requestToWriteAccount(result);
        } else {
            StatusMode statusMode = accountItem.getRawStatusMode();
            String statusText = accountItem.getStatusText();
            KeyPair keyPair = accountItem.getKeyPair();
            Date lastSync = accountItem.getLastSync();
            removeAccountWithoutCallback(account);
            result = addAccount(custom, host, port, serverName, userName, storePassword,
                    password, token, resource, colorIndex, accountItem.getOrder(), accountItem.isSyncNotAllowed(),
                    accountItem.getTimestamp(), priority, statusMode, statusText, enabled,
                    saslEnabled, tlsMode, compression, proxyType, proxyHost, proxyPort, proxyUser,
                    proxyPassword, syncable, keyPair, lastSync, archiveMode, false);
        }
        onAccountChanged(result.getAccount());

        // disable sync for account if it use not default settings
        ConnectionSettings connectionSettings = result.getConnectionSettings();
        if (connectionSettings.isCustomHostAndPort()
                || connectionSettings.getProxyType() != ProxyType.none
                || connectionSettings.getTlsMode() == TLSMode.legacy) {

            result.setSyncNotAllowed(true);
        } else result.setSyncNotAllowed(false);
    }

    public boolean haveNotAllowedSyncAccounts() {
        for (AccountItem account : accountItems.values()) {
            if (account.isSyncNotAllowed()) return true;
        }
        return false;
    }

    public void setKeyPair(AccountJid account, KeyPair keyPair) {
        AccountItem accountItem = getAccount(account);
        if (accountItem != null) {
            accountItem.setKeyPair(keyPair);
            requestToWriteAccount(accountItem);
        }
    }

    public void setEnabled(AccountJid account, boolean enabled) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            return;
        }

        accountItem.setEnabled(enabled);
        requestToWriteAccount(accountItem);
    }

    /**
     * @return List of enabled accounts.
     */
    public Collection<AccountJid> getEnabledAccounts() {
        List<AccountJid> enabledAccounts = new ArrayList<>();
        for (AccountItem accountItem : accountItems.values()) {
            if (accountItem.isEnabled()) {
                AccountJid accountJid = accountItem.getAccount();
                accountJid.setOrder(accountItem.getOrder());
                enabledAccounts.add(accountJid);
            }
        }

        return Collections.unmodifiableCollection(enabledAccounts);
    }

    public boolean hasAccounts() {
        return !accountItems.isEmpty();
    }

    /**
     * @return List of all accounts including disabled.
     */
    public Collection<AccountJid> getAllAccounts() {
        return Collections.unmodifiableCollection(accountItems.keySet());
    }

    public Collection<AccountItem> getAllAccountItems() {
        return Collections.unmodifiableCollection(accountItems.values());
    }

    public CommonState getCommonState() {
        boolean disabled = false;
        boolean offline = false;
        boolean waiting = false;
        boolean connecting = false;
        boolean roster = false;
        boolean online = false;

        for (AccountItem accountItem : accountItems.values()) {
            ConnectionState state = accountItem.getState();
            if (state == ConnectionState.connected) {
                online = true;
            }
            if (RosterManager.getInstance().isRosterReceived(accountItem.getAccount())) {
                roster = true;
            }
            if (state == ConnectionState.connecting || state == ConnectionState.authentication) {
                connecting = true;
            }
            if (state == ConnectionState.waiting) {
                waiting = true;
            }
            if (accountItem.isEnabled()) {
                offline = true;
            }
            disabled = true;
        }

        if (online) {
            return CommonState.online;
        } else if (roster) {
            return CommonState.roster;
        } else if (connecting) {
            return CommonState.connecting;
        }

        if (waiting) {
            return CommonState.waiting;
        } else if (offline) {
            return CommonState.offline;
        } else if (disabled) {
            return CommonState.disabled;
        } else {
            return CommonState.empty;
        }
    }

    /**
     * @return Color drawable level or default colors if account was not found.
     */
    public int getColorLevel(AccountJid account) {
        AccountItem accountItem = getAccount(account);
        int colorIndex;

        if (accountItem == null) {
            return 0;
        } else {
            colorIndex = accountItem.getColorIndex() % colors;
        }

        if (colorIndex < 0) {
            colorIndex += colors;
        }
        return colorIndex;
    }

    private boolean hasSameBareAddress(AccountJid account) {
        BareJid bareJid = account.getFullJid().asBareJid();
        for (AccountItem check : accountItems.values()) {
            if (!check.getAccount().equals(account)
                    && check.getAccount().getFullJid().asBareJid().equals(bareJid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Verbose account name.
     */
    public String getVerboseName(AccountJid account) {
        AccountItem accountItem = getAccount(account);
        if (accountItem == null) {
            return account.toString();
        }

        if (hasSameBareAddress(account)) {
            return account.toString();
        } else {
            return account.getFullJid().asBareJid().toString();
        }
    }

    /**
     * @return Account vCard based nick name or verbose name if nick is not
     * specified.
     */
    public String getNickName(AccountJid account) {
        String result = VCardManager.getInstance().getName(account.getFullJid().asBareJid());
        if ("".equals(result)) {
            return getVerboseName(account);
        } else {
            return result;
        }
    }

    /**
     * Sets status for account.
     */
    public void setStatus(AccountJid account, StatusMode statusMode, String statusText) {
        if (statusText != null && !statusText.trim().isEmpty()) {
            addSavedStatus(statusMode, statusText);
        }

        AccountItem accountItem = getAccount(account);
        setStatus(accountItem, statusMode, statusText);
        try {
            PresenceManager.getInstance().resendPresence(account);
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
        boolean found = false;
        for (AccountItem check : accountItems.values()) {
            if (check.isEnabled() && SettingsManager.statusMode() == check.getRawStatusMode()) {
                found = true;
                break;
            }
        }
        if (!found) {
            SettingsManager.setStatusMode(statusMode);
        }
        found = false;
        for (AccountItem check : accountItems.values()) {
            if (check.isEnabled() && SettingsManager.statusText().equals(check.getStatusText())) {
                found = true;
                break;
            }
        }
        if (!found) {
            SettingsManager.setStatusText(statusText);
        }
        onAccountChanged(account);
    }

    boolean isAway() {
        return away;
    }

    boolean isXa() {
        return xa;
    }

    /**
     * Set status mode to away.
     * <p/>
     * If we are already away or xa, do nothing.
     */
    public void goAway() {
        if (away || xa) {
            return;
        }
        away = true;
        resendPresence();
    }

    /**
     * Set status mode to xa.
     * <p/>
     * If we are already xa, do nothing.
     */
    public void goXa() {
        if (xa) {
            return;
        }
        xa = true;
        resendPresence();
    }

    /**
     * Restore status mode to the state that was before we go away.
     * <p/>
     * If we are already waked up, do nothing.
     */
    public void wakeUp() {
        if (!away && !xa) {
            return;
        }
        away = false;
        xa = false;
        resendPresence();
    }

    /**
     * Sends new presence information for all accounts.
     */
    public void resendPresence() {
        for (AccountItem accountItem : accountItems.values()) {
            if (accountItem.isEnabled()) {
                try {
                    PresenceManager.getInstance().resendPresence(accountItem.getAccount());
                } catch (NetworkException e) {
                    LogManager.exception(this, e);
                }
            }
        }
    }

    public void setColor(AccountJid accountJid, int colorIndex) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem != null) {
            accountItem.setColorIndex(colorIndex);
            requestToWriteAccount(accountItem);
        }
    }

    public void setOrder(AccountJid accountJid, int order) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem != null) {
            accountItem.setOrder(order);
            requestToWriteAccount(accountItem);
        }
    }

    public void setTimestamp(AccountJid accountJid, int timestamp) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem != null) {
            accountItem.setTimestamp(timestamp);
            requestToWriteAccount(accountItem);
        }
    }

    public void setClearHistoryOnExit(AccountJid accountJid, boolean clearHistoryOnExit) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem != null) {
            accountItem.setClearHistoryOnExit(clearHistoryOnExit);
            requestToWriteAccount(accountItem);
        }
    }

    public void setMamDefaultBehaviour(AccountJid accountJid, MamPrefsIQ.DefaultBehavior mamDefaultBehavior) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem == null) {
            return;
        }

        if (!accountItem.getMamDefaultBehaviour().equals(mamDefaultBehavior)) {
            accountItem.setMamDefaultBehaviour(mamDefaultBehavior);
            requestToWriteAccount(accountItem);
            MamManager.getInstance().requestUpdatePreferences(accountJid);
        }
    }

    public void setLoadHistorySettings(AccountJid accountJid, LoadHistorySettings loadHistorySettings) {
        AccountItem accountItem = getAccount(accountJid);
        if (accountItem == null) {
            return;
        }

        if (!accountItem.getLoadHistorySettings().equals(loadHistorySettings)) {
            accountItem.setLoadHistorySettings(loadHistorySettings);
            requestToWriteAccount(accountItem);
            // TODO request history if needed
        }
    }


    public void setSuccessfulConnectionHappened(AccountJid account, boolean successfulConnectionHappened) {
        AccountItem accountItem = getAccount(account);
        if (accountItem == null) {
            return;
        }

        accountItem.setSuccessfulConnectionHappened(successfulConnectionHappened);
        requestToWriteAccount(accountItem);
    }

    /**
     * Sets status for account.
     */
    private void setStatus(AccountItem accountItem, StatusMode statusMode, String statusText) {
        accountItem.setStatus(statusMode, statusText);
        requestToWriteAccount(accountItem);
    }

    /**
     * Sets status for all accounts.
     *
     * @param statusMode
     * @param statusText can be <code>null</code> if value was not changed.
     */
    public void setStatus(StatusMode statusMode, String statusText) {
        SettingsManager.setStatusMode(statusMode);

        if (statusText != null) {
            addSavedStatus(statusMode, statusText);
            SettingsManager.setStatusText(statusText);
        }

        for (AccountItem accountItem : accountItems.values()) {
            setStatus(accountItem, statusMode,
                    statusText == null ? accountItem.getStatusText() : statusText);
        }

        resendPresence();
        onAccountsChanged(new ArrayList<>(AccountManager.getInstance().getAllAccounts()));
    }

    /**
     * Save status in presets.
     */
    private void addSavedStatus(final StatusMode statusMode, final String statusText) {
        SavedStatus savedStatus = new SavedStatus(statusMode, statusText);
        if (savedStatuses.contains(savedStatus)) {
            return;
        }
        savedStatuses.add(savedStatus);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                StatusTable.getInstance().write(statusMode, statusText);
            }
        });
    }

    /**
     * Remove status from presets.
     */
    public void removeSavedStatus(final SavedStatus savedStatus) {
        if (!savedStatuses.remove(savedStatus)) {
            return;
        }
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                StatusTable.getInstance().remove(savedStatus.getStatusMode(),
                        savedStatus.getStatusText());
            }
        });
    }

    /**
     * Clear list of status presets.
     */
    public void clearSavedStatuses() {
        savedStatuses.clear();
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                StatusTable.getInstance().clear();
            }
        });
    }

    /**
     * @return List of preset statuses.
     */
    public Collection<SavedStatus> getSavedStatuses() {
        return Collections.unmodifiableCollection(savedStatuses);
    }

    /**
     * @return Selected account to show contacts. <code>null</code> if
     * <ul>
     * <li>there is no selected account,</li>
     * <li>selected account does not exists or disabled,</li>
     * <li>Group by account is enabled.</li>
     * </ul>
     */
    public AccountJid getSelectedAccount() {
        if (SettingsManager.contactsShowAccounts()) {
            return null;
        }

        if (TextUtils.isEmpty(SettingsManager.contactsSelectedAccount())) {
            return null;
        }

        AccountJid selected;
        try {
            selected = AccountJid.from(SettingsManager.contactsSelectedAccount());
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            return null;
        }

        AccountItem selectedAccountItem = accountItems.get(selected);
        if (selectedAccountItem != null && selectedAccountItem.isEnabled()) {
            return selected;
        }

        return null;
    }

    public void removeAccountError(AccountJid account) {
        accountErrorProvider.remove(account);
    }

    public void addAccountError(AccountErrorEvent accountErrorEvent) {
        accountErrorProvider.add(new AccountError(accountErrorEvent), true);
    }

    void removePasswordRequest(AccountJid account) {
        accountErrorProvider.remove(account);
    }

    public void onAccountChanged(AccountJid account) {
        Collection<AccountJid> accounts = new ArrayList<>(1);
        accounts.add(account);
        onAccountsChanged(accounts);
    }

    public void onAccountsChanged(final Collection<AccountJid> accounts) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnAccountChangedListener accountListener
                        : Application.getInstance().getUIListeners(OnAccountChangedListener.class)) {
                    accountListener.onAccountsChanged(accounts);
                }
            }
        });
    }

    public void onAccountEnabled(AccountItem accountItem) {
        for (OnAccountEnabledListener listener : application.getManagers(OnAccountEnabledListener.class)) {
            listener.onAccountEnabled(accountItem);
        }
    }

    public void onAccountOnline(AccountItem accountItem) {
        for (OnAccountOnlineListener listener : application.getManagers(OnAccountOnlineListener.class)) {
            listener.onAccountOnline(accountItem);
        }
    }

    public void onAccountOffline(AccountItem accountItem) {
        accountItem.clearPassword();
        for (OnAccountOfflineListener listener : application.getManagers(OnAccountOfflineListener.class)) {
            listener.onAccountOffline(accountItem);
        }
    }

    public void onAccountDisabled(AccountItem accountItem) {
        for (OnAccountDisabledListener listener : application.getManagers(OnAccountDisabledListener.class)) {
            listener.onAccountDisabled(accountItem);
        }
    }

    @Override
    public void onWipe() {
        AccountTable.getInstance().wipe();
    }

    @Override
    public void onUnload() {
        removeHistoryOnExit();
    }

    private void removeHistoryOnExit() {
        Collection<AccountItem> allAccountItems = AccountManager.getInstance().getAllAccountItems();
        for (AccountItem accountItem : allAccountItems) {
            if (accountItem.isClearHistoryOnExit()) {
                LogManager.i(LOG_TAG, "Removing all history for account " + accountItem.getAccount());
                MessageDatabaseManager.getInstance().removeAccountMessages(accountItem.getAccount());
            }
        }
    }
}
