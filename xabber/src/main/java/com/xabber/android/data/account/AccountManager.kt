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
import com.xabber.android.R
import com.xabber.android.data.*
import com.xabber.android.data.connection.*
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AccountRealmObject
import com.xabber.android.data.database.repositories.*
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.archive.LoadHistorySettings
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.extension.devices.DeviceVO
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
        val realm = DatabaseManager.getInstance().defaultRealmInstance

        for (accountRealmObject in realm.where(AccountRealmObject::class.java).findAll()) {
            val serverName =
                try {
                    JidCreate.domainBareFrom(accountRealmObject.serverName)
                } catch (e: XmppStringprepException) {
                    LogManager.exception(this, e)
                    null
                }

            val userName =
                try {
                    Localpart.from(accountRealmObject.userName)
                } catch (e: XmppStringprepException) {
                    LogManager.exception(this, e)
                    null
                }

            val resource =
                try {
                    Resourcepart.from(accountRealmObject.resource)
                } catch (e: XmppStringprepException) {
                    LogManager.exception(this, e)
                    null
                }

            if (serverName == null || userName == null || resource == null) {
                LogManager.e(
                    this,
                    "could not create account. username $userName, server name $serverName, resource $resource"
                )
                continue
            }

            AccountItem(
                accountRealmObject.isCustom,
                accountRealmObject.host,
                accountRealmObject.port,
                serverName,
                userName,
                resource,
                accountRealmObject.isStorePassword,
                accountRealmObject.password,
                accountRealmObject.token,
                if (accountRealmObject.device != null) {
                    accountRealmObject.device.toDeviceVO()
                } else {
                    null
                },
                accountRealmObject.colorIndex,
                accountRealmObject.order,
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
            ).apply {
                id = accountRealmObject.id
                isClearHistoryOnExit = accountRealmObject.isClearHistoryOnExit
                if (accountRealmObject.startHistoryTimestamp != 0L) {
                    startHistoryTimestamp = Date(accountRealmObject.startHistoryTimestamp)
                }
                accountRealmObject.mamDefaultBehavior?.let {
                    mamDefaultBehaviour = it
                }
                accountRealmObject.loadHistorySettings?.let {
                    loadHistorySettings = it
                }
                isSuccessfulConnectionHappened = accountRealmObject.isSuccessfulConnectionHappened
            }.also {
                addAccount(it)
            }
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            realm.close()
        }

        savedStatuses.addAll(StatusRepository.getAllSavedStatusesFromRealm())

        NotificationManager.getInstance().registerNotificationProvider(accountErrorProvider)

        isLoaded = true

        if (callAccountUpdate) {
            XabberAccountManager.getInstance().updateLocalAccountSettings()
        }
    }

    private fun addAccount(accountItem: AccountItem) {
        accountItems[accountItem.account] = accountItem
        Application.getInstance().getManagers(OnAccountAddedListener::class.java).forEach {
            it.onAccountAdded(accountItem)
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
            for (index in count.indices) {
                if (count[index] < count[ColorManager.defaultAccountColorIndex]) {
                    result = index
                }
            }
            return result
        }

    private val nextOrder: Int
        get() = accountItems.values.maxByOrNull(AccountItem::getOrder)?.order ?: 0 + 1

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
        val username = user.substring(0, user.indexOf('/').takeIf { it != -1 } ?: user.length)
        return allAccounts.any { it.fullJid.asBareJid().toString() == username }
    }

    /**
     * Creates new account and starts connection.
     */
    private fun addAccount(
        custom: Boolean, host: String, port: Int, serverName: DomainBareJid, userName: Localpart,
        storePassword: Boolean, password: String, token: String?, resource: Resourcepart,
        color: Int, order: Int, syncNotAllowed: Boolean, timestamp: Int, priority: Int,
        statusMode: StatusMode, statusText: String, enabled: Boolean, saslEnabled: Boolean,
        tlsMode: TLSMode, compression: Boolean, proxyType: ProxyType, proxyHost: String,
        proxyPort: Int, proxyUser: String, proxyPassword: String, syncable: Boolean,
        keyPair: KeyPair?, archiveMode: ArchiveMode, registerNewAccount: Boolean
    ): AccountItem {
        return AccountItem(
            custom, host, port, serverName, userName, resource, storePassword, password, token,
            null, color, order, syncNotAllowed, timestamp, priority, statusMode, statusText,
            enabled, saslEnabled, tlsMode, compression, proxyType, proxyHost, proxyPort, proxyUser,
            proxyPassword, syncable, keyPair, archiveMode, true, null
        ).also {
            AccountRepository.saveAccountToRealm(it)
            addAccount(it)
            ReconnectionManager.getInstance().requestReconnect(it.account)
        }

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
        user: String?, password: String, token: String?, syncable: Boolean,
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

        if (isAccountExist(user)) {
            throw NetworkException(R.string.settings_account__alert_account_exists)
        }

        val resource = XmppStringUtils.parseResource(user)
            .trim { it <= ' ' }
            .takeIf(String::isNotEmpty)
            ?.let { Resourcepart.from(it) }
            ?: generateResource()

        val accountItem = addAccount(
            Application.getInstance().resources.getBoolean(R.bool.account_use_custom_host_default),
            serverName.domain.toString(),
            5222,
            serverName,
            userName,
            storePassword,
            password,
            token,
            resource,
            nextColorIndex,
            nextOrder,
            false,
            (System.currentTimeMillis() / 1000L).toInt(),
            67,
            StatusMode.available,
            SettingsManager.statusText(),
            enabled,
            true,
            if (tlsRequired) TLSMode.required else TLSMode.enabled,
            Application.getInstance().resources.getBoolean(R.bool.account_use_compression_default),
            if (useOrbot) ProxyType.orbot else ProxyType.none,
            "localhost",
            8080,
            "",
            "",
            syncable,
            null,
            ArchiveMode.valueOf(
                Application.getInstance().getString(R.string.account_archive_mode_default_value)
            ),
            registerNewAccount
        )

        onAccountChanged(accountItem.account)

        if (accountItems.size > 1 && SettingsManager.contactsEnableShowAccounts()) {
            SettingsManager.enableContactsShowAccount()
        }

        // add xmpp account settings
        if (xabberSync) {
            XabberAccountManager.getInstance().addAccountSyncState(
                accountItem.account.fullJid.asBareJid().toString(), true
            )
        } else {
            SettingsManager.setSyncAllAccounts(false)
        }
        return accountItem.account
    }

    private fun generateResource(): Resourcepart {
        return try {
            Resourcepart.from(
                Application.getInstance().getString(R.string.account_resource_default)
                        + "-"
                        + StringUtils.randomString(8)
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

        // remove all data from database
        ContactRepository.removeContacts(account)

        GroupInviteRepository.removeAllInvitesRelatedToAccount(account)
        GroupMemberManager.removeAllAccountRelatedGroupMembers(account)
        MessageRepository.removeAccountMessagesFromRealm(account)
        GroupchatRepository.removeAccountRelatedGroupsFromRealm(account)
        RegularChatRepository.removeAllAccountRelatedRegularChatsFromRealm(account)
        DeviceRepository.removeDeviceFromRealm(getAccount(account)?.device?.id)

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

        Application.getInstance().getManagers(OnAccountRemovedListener::class.java).forEach {
            it.onAccountRemoved(accountItem)
        }
        removeAccountError(account)
    }

    /**
     * Remove XMPP Account and all related.
     * Doesn't affect to XabberAccount!
     */
    fun removeAccount(account: AccountJid) {
        // disable synchronization for this account in xabber account
        SettingsManager.setSyncAllAccounts(false)
        XabberAccountManager.getInstance().setAccountSyncState(
            account.fullJid.asBareJid().toString(),
            false
        )

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
        getAccount(account)?.apply {
            setPassword(pass)
        }?.also {
            it.recreateConnection(true)
            AccountRepository.saveAccountToRealm(it)
        }
    }

    /** Set x-token to account and remove password  */
    fun updateDevice(account: AccountJid?, token: DeviceVO?) {
        getAccount(account)?.apply {
            setDevice(token)
            setPassword("")
            setConnectionIsOutdated(true)
        }?.also {
            AccountRepository.saveAccountToRealm(it)
        } ?: LogManager.d(this, "tried to update account with new xtoken, but account was null")
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
                result.recreateConnection(false)
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
            removeAccountWithoutCallback(account)
            result = addAccount(
                custom, host, port, serverName, userName, storePassword, password, token, resource,
                colorIndex, accountItem.order, accountItem.isSyncNotAllowed, accountItem.timestamp,
                priority, accountItem.rawStatusMode, accountItem.statusText, enabled, saslEnabled,
                tlsMode, compression, proxyType, proxyHost, proxyPort, proxyUser, proxyPassword,
                syncable, accountItem.keyPair, archiveMode, false
            )
        }
        onAccountChanged(result.account)

        // disable sync for account if it use not default settings
        val connectionSettings = result.connectionSettings
        result.isSyncNotAllowed = (
                connectionSettings.isCustomHostAndPort
                        || connectionSettings.proxyType != ProxyType.none
                        || connectionSettings.tlsMode == TLSMode.legacy
                )
    }

    fun haveNotAllowedSyncAccounts() = accountItems.values.any(AccountItem::isSyncNotAllowed)

    fun setKeyPair(account: AccountJid?, keyPair: KeyPair?) {
        getAccount(account)
            ?.apply { setKeyPair(keyPair) }
            .also { AccountRepository.saveAccountToRealm(it) }
    }

    fun setEnabled(account: AccountJid?, enabled: Boolean) {
        getAccount(account)?.apply {
            isEnabled = enabled
        }.also {
            AccountRepository.saveAccountToRealm(it)
        }
    }

    /**
     * @return List of enabled accounts.
     */
    val enabledAccounts: Collection<AccountJid>
        get() = accountItems.values
            .filter(AccountItem::isEnabled)
            .map { it.account.apply { it.order } }

    val connectedAccounts: Collection<AccountJid>
        get() = accountItems.values
            .filter { it.connection.isConnected }
            .map { it.account.apply { it.order } }

    fun hasAccounts() = accountItems.isNotEmpty()

    /**
     * @return List of all accounts including disabled.
     */
    val allAccounts: Collection<AccountJid>
        get() = accountItems.keys

    val allAccountItems: Collection<AccountItem>
        get() = accountItems.values

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