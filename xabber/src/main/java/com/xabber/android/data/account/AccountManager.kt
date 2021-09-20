/*
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
package com.xabber.android.data.account

import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.xabber.android.R
import com.xabber.android.data.*
import com.xabber.android.data.connection.*
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AccountRealmObject
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.database.repositories.ContactRepository
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.database.repositories.StatusRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.archive.LoadHistorySettings
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.extension.xtoken.XToken
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.notification.BaseAccountNotificationProvider
import com.xabber.android.data.notification.NotificationManager
import com.xabber.android.data.roster.PresenceManager.sendAccountPresence
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.data.xaccount.XabberAccountManager
import com.xabber.android.ui.OnAccountChangedListener
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.forEachOnUi
import org.jivesoftware.smack.util.StringUtils
import org.jivesoftware.smackx.mam.element.MamPrefsIQ
import org.jxmpp.jid.DomainBareJid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Localpart
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.stringprep.XmppStringprepException
import org.jxmpp.util.XmppStringUtils
import java.security.KeyPair
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class manage all operations with accounts.
 *
 * Each account has unique full jid (userName@serverName/resource). This jid is
 * persistent and independent from real jid assigned by server. Real full jid
 * (assigned by server) of account can be taken by
 * [AccountItem.getRealJid].
 *
 * @author alexander.ivanov
 */
object AccountManager : OnLoadListener, OnUnloadListener, OnWipeListener, OnAuthenticatedListener {

    /**
     * List of saved statuses.
     */
    private val savedStatuses: MutableCollection<SavedStatus> = mutableListOf()

    /**
     * Number of different account colors.
     */
    private val differentAccountColorsCount: Int =
        Application.getInstance().resources.getIntArray(R.array.account_color_names).size

    /**
     * List of accounts.
     */
    private val accountItems: MutableMap<AccountJid, AccountItem> = ConcurrentHashMap()

    private val accountErrorProvider: BaseAccountNotificationProvider<AccountError> =
        BaseAccountNotificationProvider(R.drawable.ic_stat_error)

    var isLoaded = false
        private set

    private var callAccountUpdate = false

    fun setCallAccountUpdate(call: Boolean) {
        callAccountUpdate = call
    }

    override fun onAuthenticated(connectionItem: ConnectionItem) {
        Application.getInstance().runOnUiThread { removeAccountError(connectionItem.account) }
    }

    override fun onLoad() {
        val savedStatuses = StatusRepository.getAllSavedStatusesFromRealm()
        val accountItems: MutableCollection<AccountItem> = ArrayList()
        val realm = DatabaseManager.getInstance().defaultRealmInstance
        val accountRealmObjects = realm.where(AccountRealmObject::class.java).findAll()
        LogManager.i(this, "onLoad got realmobjects accounts: " + accountRealmObjects.size)
        for (accountRealmObject in accountRealmObjects) {
            var serverName: DomainBareJid? = null
            try {
                serverName = JidCreate.domainBareFrom(accountRealmObject.serverName)
            } catch (e: XmppStringprepException) {
                LogManager.exception(this, e)
            }
            var userName: Localpart? = null
            try {
                userName = Localpart.from(accountRealmObject.userName)
            } catch (e: XmppStringprepException) {
                LogManager.exception(this, e)
            }
            var resource: Resourcepart? = null
            try {
                resource = Resourcepart.from(accountRealmObject.resource)
            } catch (e: XmppStringprepException) {
                LogManager.exception(this, e)
            }
            if (serverName == null || userName == null || resource == null) {
                LogManager.e(
                    this,
                    "could not create account. username " + userName
                            + ", server name " + serverName
                            + ", resource " + resource
                )
                continue
            }

            // fix for db migration
            var order = accountRealmObject.order
            if (order == 0) {
                for (item in accountItems) {
                    if (item.order > order) order = item.order
                }
                order++
            }
            val accountItem = AccountItem(
                accountRealmObject.isCustom,
                accountRealmObject.host,
                accountRealmObject.port,
                serverName,
                userName,
                resource,
                accountRealmObject.isStorePassword,
                accountRealmObject.password,
                accountRealmObject.token,
                if (accountRealmObject.xToken != null) accountRealmObject.xToken.toXToken() else null,
                accountRealmObject.colorIndex,
                order,
                accountRealmObject.isSyncNotAllowed,
                accountRealmObject.timestamp,
                accountRealmObject.priority,
                accountRealmObject.statusMode,
                accountRealmObject.statusText,
                accountRealmObject.isEnabled,
                accountRealmObject.isSaslEnabled,
                accountRealmObject.tlsMode,
                accountRealmObject.isCompression,
                accountRealmObject.proxyType,
                accountRealmObject.proxyHost,
                accountRealmObject.proxyPort,
                accountRealmObject.proxyUser,
                accountRealmObject.proxyPassword,
                accountRealmObject.isSyncable,
                accountRealmObject.keyPair,
                accountRealmObject.archiveMode,
                accountRealmObject.isXabberAutoLoginEnabled,
                accountRealmObject.retractVersion
            )
            accountItem.id = accountRealmObject.id
            accountItem.isClearHistoryOnExit = accountRealmObject.isClearHistoryOnExit
            if (accountRealmObject.startHistoryTimestamp != 0L) {
                accountItem.startHistoryTimestamp = Date(accountRealmObject.startHistoryTimestamp)
            }
            if (accountRealmObject.mamDefaultBehavior != null) {
                accountItem.mamDefaultBehaviour = accountRealmObject.mamDefaultBehavior!!
            }
            if (accountRealmObject.loadHistorySettings != null) {
                accountItem.loadHistorySettings = accountRealmObject.loadHistorySettings
            }
            accountItem.isSuccessfulConnectionHappened =
                accountRealmObject.isSuccessfulConnectionHappened
            accountItems.add(accountItem)
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
        this.savedStatuses.addAll(savedStatuses)
        for (accountItem in accountItems) {
            addAccount(accountItem)
        }
        NotificationManager.getInstance().registerNotificationProvider(accountErrorProvider)
        isLoaded = true
        if (callAccountUpdate) {
            XabberAccountManager.getInstance().updateLocalAccountSettings()
        }
    }

    private fun addAccount(accountItem: AccountItem) {
        accountItems[accountItem.account] = accountItem
        for (listener in Application.getInstance().getManagers(
            OnAccountAddedListener::class.java
        )) {
            listener.onAccountAdded(accountItem)
        }
        if (accountItem.isEnabled) {
            Application.getInstance().getManagers(OnAccountEnabledListener::class.java).forEach {
                it.onAccountEnabled(accountItem)
            }
            if (accountItem.rawStatusMode.isOnline) {
                Application.getInstance().getManagers(OnAccountOnlineListener::class.java).forEach {
                    it.onAccountOnline(accountItem)
                }
            }
        }
        onAccountChanged(accountItem.account)
    }

    /**
     * @return Next color index for the next account.
     */
    private val nextColorIndex: Int
        get() {
            val count = IntArray(differentAccountColorsCount)
            for (accountItem in accountItems.values) {
                count[accountItem.colorIndex % differentAccountColorsCount] += 1
            }
            var result = ColorManager.defaultAccountColorIndex
            val value = count[ColorManager.defaultAccountColorIndex]
            for (index in count.indices) {
                if (count[index] < value) {
                    result = index
                }
            }
            return result
        }

    private val nextOrder: Int
        get() {
            var max = 0
            for (item in accountItems.values) {
                if (item.order > max) max = item.order
            }
            return max + 1
        }

    /**
     * @param account full jid.
     * @return Specified account or `null` if account doesn't exists.
     */
    fun getAccount(account: AccountJid?): AccountItem? {
        if (account == null) {
            LogManager.d(this, "accountJid is Null")
            throw NullPointerException("accountJid is Null")
        }
        return accountItems[account]
    }

    fun isAccountExist(user: String): Boolean {
        var user = user
        val slash = user.indexOf('/')
        if (slash != -1) {
            LogManager.d(
                this,
                "Trying to find account with FullJid instead of BareJid with jid =  $user"
            )
            LogManager.d(this, Log.getStackTraceString(Throwable()))
            user = user.substring(0, slash) // make sure we compare barejids
        }
        val accounts = allAccounts
        for (account in accounts) {
            if (account.fullJid.asBareJid().equals(user)) return true
        }
        return false
    }

    /**
     * Creates new account and starts connection.
     */
    private fun addAccount(
        custom: Boolean, host: String, port: Int, serverName: DomainBareJid, userName: Localpart,
        storePassword: Boolean, password: String, token: String, resource: Resourcepart,
        color: Int, order: Int, syncNotAllowed: Boolean, timestamp: Int, priority: Int,
        statusMode: StatusMode, statusText: String, enabled: Boolean, saslEnabled: Boolean,
        tlsMode: TLSMode, compression: Boolean, proxyType: ProxyType, proxyHost: String,
        proxyPort: Int, proxyUser: String, proxyPassword: String, syncable: Boolean,
        keyPair: KeyPair?, archiveMode: ArchiveMode, registerNewAccount: Boolean
    ): AccountItem {
        val accountItem = AccountItem(
            custom,
            host,
            port,
            serverName,
            userName,
            resource,
            storePassword,
            password,
            token,
            null,
            color,
            order,
            syncNotAllowed,
            timestamp,
            priority,
            statusMode,
            statusText,
            enabled,
            saslEnabled,
            tlsMode,
            compression,
            proxyType,
            proxyHost,
            proxyPort,
            proxyUser,
            proxyPassword,
            syncable,
            keyPair,
            archiveMode,
            true,
            null
        )
        AccountRepository.saveAccountToRealm(accountItem)
        addAccount(accountItem)
        ReconnectionManager.getInstance().requestReconnect(accountItem.account)
        return accountItem
    }

    /**
     * Creates new account.
     *
     * @param user bare jid.
     * @return assigned account name.
     * @throws NetworkException if user or server part are invalid.
     */
    @Throws(NetworkException::class)
    fun addAccount(
        user: String?, password: String, token: String, syncable: Boolean,
        storePassword: Boolean, xabberSync: Boolean, useOrbot: Boolean,
        registerNewAccount: Boolean, enabled: Boolean, tlsRequired: Boolean
    ): AccountJid {
        if (user == null) {
            throw NetworkException(R.string.settings_account__alert_xmpp_id_not_specified)
        }
        if (user.contains(" ")) {
            throw NetworkException(R.string.account_add__alert_incorrect_xmpp_id)
        }
        val serverName: DomainBareJid = try {
            JidCreate.domainBareFrom(user)
        } catch (e: XmppStringprepException) {
            throw NetworkException(R.string.account_add__alert_incorrect_xmpp_id)
        }
        val userName: Localpart = try {
            Localpart.from(XmppStringUtils.parseLocalpart(user))
        } catch (e: XmppStringprepException) {
            throw NetworkException(R.string.account_add__alert_incorrect_xmpp_id)
        }
        if (isAccountExist(user)) throw NetworkException(R.string.settings_account__alert_account_exists)
        var resource: Resourcepart? = null
        val resourceString = XmppStringUtils.parseResource(user).trim { it <= ' ' }
        if (!TextUtils.isEmpty(resourceString)) {
            try {
                resource = Resourcepart.from(resourceString)
            } catch (e: XmppStringprepException) {
                LogManager.exception(this, e)
            }
        }
        val host = serverName.domain.toString()
        val port = 5222
        if (resource == null) {
            resource = generateResource()
        }
        val accountItem: AccountItem
        val useCustomHost =
            Application.getInstance().resources.getBoolean(R.bool.account_use_custom_host_default)
        val useCompression =
            Application.getInstance().resources.getBoolean(R.bool.account_use_compression_default)
        val archiveMode =
            ArchiveMode.valueOf(
                Application.getInstance().getString(R.string.account_archive_mode_default_value)
            )
        accountItem = addAccount(
            useCustomHost,
            host,
            port,
            serverName,
            userName,
            storePassword,
            password,
            token,
            resource,
            nextColorIndex,
            nextOrder,
            false,
            XabberAccountManager.getInstance().currentTime,
            67,
            StatusMode.available,
            SettingsManager.statusText(),
            enabled,
            true,
            if (tlsRequired) TLSMode.required else TLSMode.enabled,
            useCompression,
            if (useOrbot) ProxyType.orbot else ProxyType.none,
            "localhost",
            8080,
            "",
            "",
            syncable,
            null,
            archiveMode,
            registerNewAccount
        )
        if (accountItem == null) {
            throw NetworkException(R.string.account_add__alert_registration_failed)
        }
        onAccountChanged(accountItem.account)
        if (accountItems.size > 1 && SettingsManager.contactsEnableShowAccounts()) {
            SettingsManager.enableContactsShowAccount()
        }

        // add xmpp account settings
        if (xabberSync) XabberAccountManager.getInstance()
            .addAccountSyncState(
                accountItem.account.fullJid.asBareJid().toString(),
                true
            ) else SettingsManager.setSyncAllAccounts(false)
        return accountItem.account
    }

    private fun generateResource(): Resourcepart {
        return try {
            Resourcepart.from(
                Application.getInstance()
                    .getString(R.string.account_resource_default) + "-" + StringUtils.randomString(
                    8
                )
            )
        } catch (e: XmppStringprepException) {
            LogManager.exception(this, e)
            Resourcepart.EMPTY
        }
    }

    /**
     * Remove user`s account. Don't call any callbacks.
     */
    private fun removeAccountWithoutCallback(account: AccountJid) {
        val accountItem = getAccount(account) ?: return

        // remove contacts and account from cache
        ContactRepository.removeContacts(account)
        val wasEnabled = accountItem.isEnabled
        accountItem.isEnabled = false
        accountItem.disconnect()
        if (wasEnabled) {
            if (accountItem.rawStatusMode.isOnline) {
                accountItem.clearPassword()
                Application.getInstance().getManagers(OnAccountOfflineListener::class.java)
                    .forEach { it.onAccountOffline(accountItem) }
            }
            Application.getInstance().getManagers(OnAccountDisabledListener::class.java).forEach {
                it.onAccountDisabled(accountItem)
            }
        }
        AccountRepository.deleteAccountFromRealm(account.toString(), accountItem.id)
        accountItems.remove(account)
        for (listener in Application.getInstance().getManagers(
            OnAccountRemovedListener::class.java
        )) {
            listener.onAccountRemoved(accountItem)
        }
        removeAccountError(account)
    }

    /**
     * Remove user`s account.
     */
    fun removeAccount(account: AccountJid) {
        // disable synchronization for this account in xabber account
        SettingsManager.setSyncAllAccounts(false)
        XabberAccountManager.getInstance()
            .setAccountSyncState(account.fullJid.asBareJid().toString(), false)

        // removing local account
        removeAccountWithoutCallback(account)
        onAccountChanged(account)
    }

    /**
     * Remove user`s account.
     * without set sync for account
     */
    fun removeAccountWithoutSync(account: AccountJid) {
        removeAccountWithoutCallback(account)
        onAccountChanged(account)
    }

    fun updateAccountPassword(account: AccountJid?, pass: String?) {
        val result = getAccount(account) ?: return
        result.setPassword(pass)
        result.recreateConnectionWithEnable(result.account)
        AccountRepository.saveAccountToRealm(result)
    }

    /** Set x-token to account and remove password  */
    fun updateXToken(account: AccountJid?, token: XToken?) {
        val accountItem = getAccount(account)
        if (accountItem != null) {
            accountItem.setXToken(token)
            accountItem.setPassword("")
            accountItem.setConnectionIsOutdated(true)
            //accountItem.recreateConnectionWithEnable(accountItem.getAccount());
            AccountRepository.saveAccountToRealm(accountItem)
        } else {
            LogManager.d(this, "tried to update account with new xtoken, but account was null")
        }
    }

    /**
     * Update user`s account.
     * It will reconnect to the server with new generated Resourcepart
     * @param account       full source jid
     */
    fun generateNewResourceForAccount(account: AccountJid) {
        val accountItem = getAccount(account) ?: return
        val connectionSettings = accountItem.connectionSettings
        updateAccount(
            account,
            connectionSettings.isCustomHostAndPort,
            connectionSettings.host,
            connectionSettings.port,
            connectionSettings.serverName,
            connectionSettings.userName,
            accountItem.isStorePassword,
            connectionSettings.password,
            connectionSettings.token,
            generateResource(),
            accountItem.priority,
            accountItem.isEnabled,
            connectionSettings.isSaslEnabled,
            connectionSettings.tlsMode,
            connectionSettings.useCompression(),
            connectionSettings.proxyType,
            connectionSettings.proxyHost,
            connectionSettings.proxyPort,
            connectionSettings.proxyUser,
            connectionSettings.proxyPassword,
            accountItem.isSyncable,
            accountItem.archiveMode,
            accountItem.colorIndex
        )
    }

    /**
     * Update user`s account.
     *
     *
     * It will reconnect to the server if changes was made.
     *
     *
     * It will remove old account and create new one if full jid was changed.
     *
     * @param account       full source jid
     */
    fun updateAccount(
        account: AccountJid,
        custom: Boolean,
        host: String,
        port: Int,
        serverName: DomainBareJid,
        userName: Localpart,
        storePassword: Boolean,
        password: String,
        token: String,
        resource: Resourcepart,
        priority: Int,
        enabled: Boolean,
        saslEnabled: Boolean,
        tlsMode: TLSMode,
        compression: Boolean,
        proxyType: ProxyType,
        proxyHost: String,
        proxyPort: Int,
        proxyUser: String,
        proxyPassword: String,
        syncable: Boolean,
        archiveMode: ArchiveMode,
        colorIndex: Int
    ) {
        val result: AccountItem
        val accountItem = getAccount(account) ?: return
        if (accountItem.connectionSettings.serverName.equals(serverName)
            && accountItem.connectionSettings.userName == userName && accountItem.connectionSettings.resource == resource
        ) {
            result = accountItem
            result.colorIndex = colorIndex
            var reconnect = false
            if (accountItem.connectionSettings.isCustomHostAndPort != custom || accountItem.connectionSettings.host != host
                || accountItem.connectionSettings.port != port || accountItem.connectionSettings.password != password
                || accountItem.connectionSettings.tlsMode != tlsMode || accountItem.connectionSettings.isSaslEnabled != saslEnabled || accountItem.connectionSettings.useCompression() != compression || accountItem.connectionSettings.proxyType != proxyType || accountItem.connectionSettings.proxyHost != proxyHost
                || accountItem.connectionSettings.proxyPort != proxyPort || accountItem.connectionSettings.proxyUser != proxyUser
                || accountItem.connectionSettings.proxyPassword != proxyPassword
            ) {
                result.updateConnectionSettings(
                    custom, host, port, password, saslEnabled, tlsMode,
                    compression, proxyType, proxyHost, proxyPort, proxyUser, proxyPassword
                )
                reconnect = true
            }
            if (result.isSyncable != syncable) {
                result.isSyncable = syncable
                for (listener in Application.getInstance().getManagers(
                    OnAccountSyncableChangedListener::class.java
                )) {
                    listener.onAccountSyncableChanged(result)
                }
            }
            result.isStorePassword = storePassword
            val changed = result.isEnabled != enabled
            result.isEnabled = enabled
            if (result.priority != priority) {
                result.priority = priority
                try {
                    sendAccountPresence(account)
                } catch (e: NetworkException) {
                    LogManager.exception(this, e)
                }
            }
            if (result.archiveMode != archiveMode) {
                result.archiveMode = archiveMode
            }
            if (changed && enabled) {
                Application.getInstance().getManagers(OnAccountEnabledListener::class.java)
                    .forEach { it.onAccountEnabled(accountItem) }
                if (result.rawStatusMode.isOnline) {
                    Application.getInstance().getManagers(OnAccountOnlineListener::class.java)
                        .forEach { it.onAccountOnline(accountItem) }
                }
            }
            if (changed || reconnect) {
                result.isSuccessfulConnectionHappened = false
                result.recreateConnection()
            }
            if (changed && !enabled) {
                if (result.rawStatusMode.isOnline) {
                    accountItem.clearPassword()
                    Application.getInstance().getManagers(OnAccountOfflineListener::class.java)
                        .forEach { it.onAccountOffline(result) }
                }
                Application.getInstance().getManagers(OnAccountDisabledListener::class.java)
                    .forEach { it.onAccountDisabled(accountItem) }
            }
            AccountRepository.saveAccountToRealm(accountItem)
        } else {
            val statusMode = accountItem.rawStatusMode
            val statusText = accountItem.statusText
            val keyPair = accountItem.keyPair
            removeAccountWithoutCallback(account)
            result = addAccount(
                custom,
                host,
                port,
                serverName,
                userName,
                storePassword,
                password,
                token,
                resource,
                colorIndex,
                accountItem.order,
                accountItem.isSyncNotAllowed,
                accountItem.timestamp,
                priority,
                statusMode,
                statusText,
                enabled,
                saslEnabled,
                tlsMode,
                compression,
                proxyType,
                proxyHost,
                proxyPort,
                proxyUser,
                proxyPassword,
                syncable,
                keyPair,
                archiveMode,
                false
            )
        }
        onAccountChanged(result.account)

        // disable sync for account if it use not default settings
        val connectionSettings = result.connectionSettings
        result.isSyncNotAllowed = (connectionSettings.isCustomHostAndPort
                || connectionSettings.proxyType != ProxyType.none || connectionSettings.tlsMode == TLSMode.legacy)
    }

    fun haveNotAllowedSyncAccounts() = accountItems.values.any(AccountItem::isSyncNotAllowed)

    fun setKeyPair(account: AccountJid?, keyPair: KeyPair?) {
        val accountItem = getAccount(account)
        if (accountItem != null) {
            accountItem.keyPair = keyPair
            AccountRepository.saveAccountToRealm(accountItem)
        }
    }

    fun setEnabled(account: AccountJid?, enabled: Boolean) {
        val accountItem = getAccount(account) ?: return
        accountItem.isEnabled = enabled
        AccountRepository.saveAccountToRealm(accountItem)
    }

    /**
     * @return List of enabled accounts.
     */
    val enabledAccounts: Collection<AccountJid>
        get() {
            val accountsCopy: Map<AccountJid, AccountItem> = HashMap(accountItems)
            val enabledAccounts: MutableList<AccountJid> = ArrayList()
            for (accountItem in accountsCopy.values) {
                if (accountItem.isEnabled) {
                    val accountJid = accountItem.account
                    accountJid.setOrder(accountItem.order)
                    enabledAccounts.add(accountJid)
                }
            }
            return Collections.unmodifiableCollection(enabledAccounts)
        }

    val connectedAccounts: Collection<AccountJid>
        get() {
            val accountsCopy: Map<AccountJid, AccountItem> = HashMap(accountItems)
            val connectedAccounts: MutableList<AccountJid> = ArrayList()
            for (accountItem in accountsCopy.values) {
                if (accountItem.connection.isConnected) {
                    val accountJid = accountItem.account
                    accountJid.setOrder(accountItem.order)
                    connectedAccounts.add(accountJid)
                }
            }
            return Collections.unmodifiableCollection(connectedAccounts)
        }

    fun hasAccounts() = accountItems.isNotEmpty()

    /**
     * @return List of all accounts including disabled.
     */
    val allAccounts: Collection<AccountJid>
        get() {
            val accountsCopy: Map<AccountJid, AccountItem> = HashMap(accountItems)
            return Collections.unmodifiableCollection(accountsCopy.keys)
        }

    val allAccountItems: Collection<AccountItem>
        get() {
            val accountsCopy: Map<AccountJid, AccountItem> = HashMap(accountItems)
            return Collections.unmodifiableCollection(accountsCopy.values)
        }

    val commonState: CommonState
        get() {
            val accounts = accountItems.values
            return when {
                accounts.any { it.state == ConnectionState.connected } -> {
                    CommonState.online
                }

                accounts.any {
                    RosterManager.getInstance().isRosterReceived(it.account)
                } -> {
                    CommonState.roster
                }

                accounts.any {
                    it.state == ConnectionState.connecting || it.state == ConnectionState.authentication
                } -> {
                    CommonState.connecting
                }

                accounts.any { it.state == ConnectionState.waiting } -> {
                    CommonState.waiting
                }

                accounts.any { it.isEnabled } -> {
                    CommonState.offline
                }

                else -> {
                    CommonState.disabled
                }
            }
        }

    /**
     * @return Color drawable level or default colors if account was not found.
     */
    fun getColorLevel(account: AccountJid?): Int {
        var colorIndex: Int =
            getAccount(account)?.let { it.colorIndex % differentAccountColorsCount }
                ?: ColorManager.defaultAccountColorIndex

        if (colorIndex < 0) {
            colorIndex += differentAccountColorsCount
        }

        return colorIndex
    }

    /**
     * @return Verbose account name.
     */
    fun getVerboseName(account: AccountJid): String {
        val hasSomeBareAddress = accountItems.values.any {
            it.account != account && it.account.fullJid.asBareJid() == account.fullJid.asBareJid()
        }
        return if (hasSomeBareAddress) {
            account.toString()
        } else {
            account.fullJid.asBareJid().toString()
        }
    }

    /**
     * @return Account vCard based nick name or verbose name if nick is not
     * specified.
     */
    fun getNickName(account: AccountJid) =
        VCardManager.getInstance().getName(account.fullJid.asBareJid())?.takeIf { it != "" }
            ?: getVerboseName(account)

    /**
     * Sets status for account.
     */
    fun setStatus(account: AccountJid, statusMode: StatusMode, statusText: String?) {
        if (statusText != null && statusText.trim { it <= ' ' }.isNotEmpty()) {
            addSavedStatus(statusMode, statusText)
        }

        getAccount(account)?.let {
            it.setStatus(statusMode, statusText)
            AccountRepository.saveAccountToRealm(it)
        }

        try {
            sendAccountPresence(account)
        } catch (e: NetworkException) {
            LogManager.exception(this, e)
        }

        with(accountItems.values.filterNot(AccountItem::isEnabled)) {
            if (none { SettingsManager.statusMode() == it.rawStatusMode }) {
                SettingsManager.setStatusMode(statusMode)
            }
            if (none { SettingsManager.statusText() == it.statusText }) {
                SettingsManager.setStatusText(statusText)
            }
        }

        onAccountChanged(account)
    }

    /**
     * Sends new presence information for all accounts.
     */
    fun resendPresence() {
        accountItems.values.filter(AccountItem::isEnabled).forEach {
            try {
                sendAccountPresence(it.account)
            } catch (e: NetworkException) {
                LogManager.exception(this, e)
            }
        }
    }

    fun setColor(accountJid: AccountJid, colorIndex: Int) {
        getAccount(accountJid)?.let {
            it.colorIndex = colorIndex
            AccountRepository.saveAccountToRealm(it)
        }
        if (firstAccount == accountJid) {
            SettingsManager.setMainAccountColorLevel(colorIndex)
        }
    }

    val firstAccount: AccountJid?
        get() = enabledAccounts.minOrNull()

    fun setOrder(accountJid: AccountJid, order: Int) {
        getAccount(accountJid)?.let {
            it.order = order
            AccountRepository.saveAccountToRealm(it)
        }
    }

    fun setTimestamp(accountJid: AccountJid, timestamp: Int) {
        getAccount(accountJid)?.let {
            it.timestamp = timestamp
            AccountRepository.saveAccountToRealm(it)
        }
    }

    fun setClearHistoryOnExit(accountJid: AccountJid, clearHistoryOnExit: Boolean) {
        getAccount(accountJid)
            ?.takeIf { it.isClearHistoryOnExit != clearHistoryOnExit }
            ?.let {
                it.isClearHistoryOnExit = clearHistoryOnExit
                AccountRepository.saveAccountToRealm(it)
            }
    }

    fun setMamDefaultBehaviour(
        accountJid: AccountJid, mamDefaultBehavior: MamPrefsIQ.DefaultBehavior
    ) {
        getAccount(accountJid)
            ?.takeIf { it.mamDefaultBehaviour != mamDefaultBehavior }
            ?.let {
                it.mamDefaultBehaviour = mamDefaultBehavior
                AccountRepository.saveAccountToRealm(it)
            }
    }

    fun setLoadHistorySettings(accountJid: AccountJid, loadHistorySettings: LoadHistorySettings) {
        getAccount(accountJid)
            ?.takeIf { it.loadHistorySettings != loadHistorySettings }
            ?.let {
                it.loadHistorySettings = loadHistorySettings
                AccountRepository.saveAccountToRealm(it)
            }
    }

    fun setSuccessfulConnectionHappened(
        account: AccountJid, successfulConnectionHappened: Boolean
    ) {
        getAccount(account)?.let {
            it.isSuccessfulConnectionHappened = successfulConnectionHappened
            AccountRepository.saveAccountToRealm(it)
        }
    }

    /**
     * Sets status for all accounts.
     *
     * @param statusText can be `null` if value was not changed.
     */
    fun setStatus(statusMode: StatusMode, statusText: String?) {
        SettingsManager.setStatusMode(statusMode)
        if (statusText != null) {
            addSavedStatus(statusMode, statusText)
            SettingsManager.setStatusText(statusText)
        }
        for (accountItem in accountItems.values) {
            accountItem.setStatus(statusMode, statusText)
            AccountRepository.saveAccountToRealm(accountItem)
        }
        resendPresence()
        Application.getInstance().getUIListeners(OnAccountChangedListener::class.java)
            .forEachOnUi { it.onAccountsChanged(allAccounts) }
    }

    /**
     * Save status in presets.
     */
    private fun addSavedStatus(statusMode: StatusMode, statusText: String) {
        val savedStatus = SavedStatus(statusMode, statusText)
        if (savedStatuses.contains(savedStatus)) {
            return
        }
        savedStatuses.add(savedStatus)
        StatusRepository.saveStatusToRealm(savedStatus)
    }

    /**
     * Remove status from presets.
     */
    fun removeSavedStatus(savedStatus: SavedStatus) {
        if (!savedStatuses.remove(savedStatus)) {
            return
        }
        StatusRepository.deleteSavedStatusFromRealm(savedStatus)
    }

    /**
     * Clear list of status presets.
     */
    fun clearSavedStatuses() {
        savedStatuses.clear()
        StatusRepository.clearAllSavedStatusesInRealm()
    }

    /**
     * @return List of preset statuses.
     */
    fun getSavedStatuses(): Collection<SavedStatus> {
        return Collections.unmodifiableCollection(savedStatuses)
    }

    fun removeAccountError(account: AccountJid?) {
        accountErrorProvider.remove(account)
    }

    fun addAccountError(accountErrorEvent: AccountErrorEvent?) {
        accountErrorProvider.add(AccountError(accountErrorEvent), true)
    }

    fun removePasswordRequest(account: AccountJid?) {
        accountErrorProvider.remove(account)
    }

    fun onAccountChanged(account: AccountJid) {
        Application.getInstance().getUIListeners(OnAccountChangedListener::class.java)
            .forEachOnUi { it.onAccountsChanged(listOf(account)) }
    }

    override fun onWipe() {
        AccountRepository.clearAllAccountsFromRealm()
    }

    override fun onUnload() {
        allAccountItems.filter(AccountItem::isClearHistoryOnExit).forEach { _ ->
            MessageRepository.removeAllAccountMessagesFromRealm() //todo WTF WHY ALL?!
        }
    }


    fun setAllAccountAutoLoginToXabber(autoLogin: Boolean) {
        for (accountItem in allAccountItems) {
            accountItem.isXabberAutoLoginEnabled = autoLogin
            AccountRepository.saveAccountToRealm(accountItem)
        }
    }

}