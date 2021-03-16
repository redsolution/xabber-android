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
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.RosterContact
import com.xabber.xmpp.vcard.VCard
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.Jid

/**
 * Base listener to notify UI about some changes.
 *
 *
 * This listener should be registered from [Activity.onResume] and
 * unregistered from [Activity.onPause].
 *
 * @author alexander.ivanov
 */
interface BaseUIListener

inline fun <T: BaseUIListener> Iterable<T>.forEachOnUi(crossinline action: (T) -> Unit): Unit {
    Application.getInstance().runOnUiThread{
        for (element in this) action(element)
    }
}

interface OnXTokenSessionsUpdatedListener: BaseUIListener {
    fun onXTokenSessionsUpdated()
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

interface OnReorderClickListener: BaseUIListener {
    fun onReorderCLick()
}

interface OnNewMessageListener : BaseUIListener {
    fun onNewMessage()
}

interface OnNewIncomingMessageListener : BaseUIListener {
    fun onNewIncomingMessage(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnMessageUpdatedListener : BaseUIListener {
    fun onMessageUpdated()
}

interface OnLastHistoryLoadStartedListener: BaseUIListener {
    fun onLastHistoryLoadStarted(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnLastHistoryLoadFinishedListener : BaseUIListener {
    fun onLastHistoryLoadFinished(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnGroupchatRequestListener : BaseUIListener {
    fun onGroupchatMembersReceived(account: AccountJid, groupchatJid: ContactJid)
    fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid)
    fun onGroupchatMemberUpdated(accountJid: AccountJid, groupchatJid: ContactJid, groupchatMemberId: String)
}

interface OnGroupSelectorListToolbarActionResultListener : BaseUIListener {
    fun onActionSuccess(account: AccountJid, groupchatJid: ContactJid, successfulJids: List<String>)
    fun onPartialSuccess(account: AccountJid, groupchatJid: ContactJid, successfulJids: List<String>,
                         failedJids: List<String>)
    fun onActionFailure(account: AccountJid, groupchatJid: ContactJid, failedJids: List<String>)
}

interface OnGroupPresenceUpdatedListener : BaseUIListener {
    fun onGroupPresenceUpdated(groupJid: ContactJid)
}

interface OnErrorListener : BaseUIListener {
    /** @param resourceId String with error description. */
    fun onError(resourceId: Int)
}

interface OnContactChangedListener : BaseUIListener {
    fun onContactsChanged(entities: Collection<RosterContact>)
}

interface OnConnectionStateChangedListener: BaseUIListener {
    fun onConnectionStateChanged(newConnectionState: ConnectionState)
}

interface OnChatUpdatedListener: BaseUIListener {
    fun onChatUpdated()
}

interface OnChatStateListener : BaseUIListener {
    fun onChatStateChanged(entities: Collection<RosterContact>)
}

interface OnBlockedListChangedListener : BaseUIListener {
    fun onBlockedListChanged(account: AccountJid?)
}

interface OnAuthAskListener: BaseUIListener {
    fun onAuthAsk(accountJid: AccountJid, contactJid: ContactJid)
}

interface OnAddAccountClickListener: BaseUIListener {
    fun onAddAccountClick()
}

interface OnAccountChangedListener : BaseUIListener {
    fun onAccountsChanged(accounts: Collection<AccountJid?>?)
}

interface OnGroupStatusResultListener: BaseUIListener {
    fun onStatusDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onStatusSuccessfullyChanged(groupchat: GroupChat)
}

interface OnGroupSettingsResultsListener: BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onErrorAtDataFormRequesting(groupchat: GroupChat)
    fun onErrorAtSettingsSetting(groupchat: GroupChat)
    fun onGroupSettingsSuccessfullyChanged(groupchat: GroupChat)
}

interface OnGroupMemberRightsListener: BaseUIListener {
    fun onGroupchatMemberRightsFormReceived(groupchat: GroupChat, iq: GroupchatMemberRightsReplyIQ)
    fun onSuccessfullyChanges(groupchat: GroupChat)
    fun onError(groupchat: GroupChat)
}

interface OnGroupDefaultRestrictionsListener: BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onSuccessful(groupchat: GroupChat)
}