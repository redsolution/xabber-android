package com.xabber.android.data.account

import com.xabber.android.data.BaseManagerInterface

interface OnAccountAddedListener : BaseManagerInterface {
    /**
     * New account was added to the account list.
     */
    fun onAccountAdded(accountItem: AccountItem?)
}

interface OnAccountDisabledListener : BaseManagerInterface {
    /**
     * Account was disabled.
     * [OnAccountOfflineListener.onAccountOffline] and
     * [OnDisconnectListener.onDisconnect] will be call first.
     */
    fun onAccountDisabled(accountItem: AccountItem?)
}

interface OnAccountEnabledListener : BaseManagerInterface {
    /**
     * Account was enabled.
     * [OnAccountAddedListener.onAccountAdded] will be called first.
     */
    fun onAccountEnabled(accountItem: AccountItem?)
}

interface OnAccountOfflineListener : BaseManagerInterface {
    /**
     * Go offline requested.
     */
    fun onAccountOffline(accountItem: AccountItem?)
}

interface OnAccountOnlineListener : BaseManagerInterface {
    /**
     * Go online requested.
     * [OnAccountEnabledListener.onAccountEnabled]
     * and [OnConnectionListener.onConnection] will be called first.
     */
    fun onAccountOnline(accountItem: AccountItem?)
}

interface OnAccountRemovedListener : BaseManagerInterface {
    /**
     * Account was removed from account list.
     * [OnAccountDisabledListener.onAccountDisabled] will be call first.
     */
    fun onAccountRemoved(accountItem: AccountItem?)
}

interface OnAccountSyncableChangedListener : BaseManagerInterface {
    /**
     * Account's syncable has been changed.
     */
    fun onAccountSyncableChanged(accountItem: AccountItem?)
}
