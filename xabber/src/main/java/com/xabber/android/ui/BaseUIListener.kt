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
package com.xabber.android.ui

import com.xabber.android.data.Application
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.connection.ConnectionState
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.RosterContact
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsReplyIQ
import com.xabber.xmpp.vcard.VCard
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.Jid

/**
 * Base listener to notify UI about some changes.
 *
 * This listener should be registered from [Activity.onResume] and
 * unregistered from [Activity.onPause].
 *
 */
interface BaseUIListener

fun interface SamBaseUiListener : BaseUIListener {
    fun onAction()
}

inline fun <T : BaseUIListener> Iterable<T>.forEachOnUi(crossinline action: (T) -> Unit) {
    Application.getInstance().runOnUiThread { forEach { action(it) } }
}

fun <T : SamBaseUiListener> notifySamUiListeners(listener: Class<T>) {
    Application.getInstance().runOnUiThread {
        Application.getInstance().getUIListeners(listener).map { listener -> listener.onAction() }
    }
}

interface OnDevicesSessionsUpdatedListener : SamBaseUiListener

interface OnReorderClickListener : SamBaseUiListener

interface OnNewMessageListener : SamBaseUiListener

interface OnMessageUpdatedListener : SamBaseUiListener

interface OnChatUpdatedListener : SamBaseUiListener

interface OnAddAccountClickListener : SamBaseUiListener

interface OnNewIncomingMessageListener : BaseUIListener {
    fun onNewIncomingMessage(
        accountJid: AccountJid,
        contactJid: ContactJid,
        message: MessageRealmObject? = null,
        needNotification: Boolean = false,
    )
}

interface OnLastHistoryLoadStartedListener : BaseUIListener {
    fun onLastHistoryLoadStarted(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnLastHistoryLoadFinishedListener : BaseUIListener {
    fun onLastHistoryLoadFinished(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnLastHistoryLoadErrorListener : BaseUIListener {
    fun onLastHistoryLoadingError(accountJid: AccountJid, contactJid: ContactJid, errorText: String? = null)
}

interface OnGroupPresenceUpdatedListener : BaseUIListener {
    fun onGroupPresenceUpdated(accountJid: AccountJid, groupJid: ContactJid, presence: Presence)
}

interface OnErrorListener : BaseUIListener {
    /** @param resourceId String with error description. */
    fun onError(resourceId: Int)
}

interface OnContactChangedListener : BaseUIListener {
    fun onContactsChanged(entities: Collection<RosterContact>)
}

interface OnConnectionStateChangedListener : BaseUIListener {
    fun onConnectionStateChanged(newConnectionState: ConnectionState)
}

interface OnChatStateListener : BaseUIListener {
    fun onChatStateChanged(entities: Collection<RosterContact>)
}

interface OnBlockedListChangedListener : BaseUIListener {
    fun onBlockedListChanged(account: AccountJid?)
}

interface OnAuthAskListener : BaseUIListener {
    fun onAuthAsk(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnAccountChangedListener : BaseUIListener {
    fun onAccountsChanged(accounts: Collection<AccountJid>)
}

interface OnGroupchatRequestListener : BaseUIListener {
    fun onGroupchatMembersReceived(account: AccountJid, groupchatJid: ContactJid)
    fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid)
    fun onGroupchatMemberUpdated(accountJid: AccountJid, groupchatJid: ContactJid, groupchatMemberId: String)
}

interface OnGroupSelectorListToolbarActionResultListener : BaseUIListener {
    fun onActionSuccess(account: AccountJid, groupchatJid: ContactJid, successfulJids: List<String>)
    fun onPartialSuccess(
        account: AccountJid,
        groupchatJid: ContactJid,
        successfulJids: List<String>,
        failedJids: List<String>,
    )

    fun onActionFailure(account: AccountJid, groupchatJid: ContactJid, failedJids: List<String>)
}

interface OnVCardSaveListener : BaseUIListener {
    fun onVCardSaveSuccess(account: AccountJid?)
    fun onVCardSaveFailed(account: AccountJid?)
}

interface OnVCardListener : BaseUIListener {
    fun onVCardReceived(account: AccountJid?, jid: Jid?, vCard: VCard?)
    fun onVCardFailed(account: AccountJid?, jid: Jid?)
}

interface OnStatusChangeListener : BaseUIListener {
    fun onStatusChanged(account: AccountJid?, user: ContactJid?, statusText: String?)
    fun onStatusChanged(account: AccountJid?, user: ContactJid?, statusMode: StatusMode?, statusText: String?)
}

interface OnGroupStatusResultListener : BaseUIListener {
    fun onStatusDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onStatusSuccessfullyChanged(groupchat: GroupChat)
}

interface OnGroupSettingsResultsListener : BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onErrorAtDataFormRequesting(groupchat: GroupChat)
    fun onErrorAtSettingsSetting(groupchat: GroupChat)
    fun onGroupSettingsSuccessfullyChanged(groupchat: GroupChat)
}

interface OnGroupMemberRightsListener : BaseUIListener {
    fun onGroupchatMemberRightsFormReceived(groupchat: GroupChat, iq: GroupchatMemberRightsReplyIQ)
    fun onSuccessfullyChanges(groupchat: GroupChat)
    fun onError(groupchat: GroupChat)
}

interface OnGroupDefaultRestrictionsListener : BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onSuccessful(groupchat: GroupChat)
}