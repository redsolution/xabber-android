package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.xabber.android.R
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.utils.StringUtils
import java.util.*

data class ChatListItemData(
    val avatar: Drawable,
    val contactStatusLevel: Int,
    val nickname: String,
    val messageText: String,
    val time: String,
    val accountColorIndicator: Int,
    val messageStatus: MessageStatus = MessageStatus.NONE,
    val unreadCount: Int = 0,
    val notificationMode: NotificationState.NotificationMode = NotificationState.NotificationMode.byDefault,
    val isNotifyAboutMessage: Boolean = true,
    val isRosterContact: Boolean = true,
    val isBlocked: Boolean = false,
    val isCustomNotification: Boolean = false,
    val isStatusBadgeVisible: Boolean = false,
    val isStatusBadgeFiltered: Boolean = false,
) {

    companion object {

        private fun getNotificationMode(abstractChat: AbstractChat) =
            abstractChat.notificationState.determineModeByGlobalSettings()

        private fun getNickname(abstractChat: AbstractChat, resources: Resources): String {
            val accountJid = abstractChat.account
            val contactJid = abstractChat.contactJid
            return when {
                accountJid.bareJid.toString() == contactJid.bareJid.toString() ->
                    resources.getText(R.string.saved_messages__header).toString()
                abstractChat is GroupChat ->
                    abstractChat.name ?: RosterManager.getInstance().getBestContact(accountJid, contactJid).name
                else -> RosterManager.getInstance().getBestContact(accountJid, contactJid).name
            }
        }

        private fun getAvatar(abstractChat: AbstractChat): Drawable =
            if (abstractChat.account.bareJid.toString() == abstractChat.contactJid.bareJid.toString()) {
                AvatarManager.getInstance().getSavedMessagesAvatar(abstractChat.account)
            } else {
                RosterManager.getInstance().getAbstractContact(abstractChat.account, abstractChat.contactJid)
                    .getAvatar(true)
            }

        private fun getTime(abstractChat: AbstractChat, context: Context): String =
            when {
                GroupInviteManager.hasActiveIncomingInvites(abstractChat.account, abstractChat.contactJid) -> {
                    StringUtils.getSmartTimeTextForRoster(
                        context,
                        Date(GroupInviteManager.getLastInvite(abstractChat.account, abstractChat.contactJid)!!.date)
                    )
                }
                abstractChat.lastMessage != null && abstractChat.lastMessage?.isValid ?: false -> {
                    StringUtils.getSmartTimeTextForRoster(context, Date(abstractChat.lastMessage!!.timestamp))
                }
                else -> ""
            }

        private fun getAccountColorIndicatorColor(abstractChat: AbstractChat) =
            ColorManager.getInstance().accountPainter.getAccountMainColor(abstractChat.account)

        fun createFromChat(abstractChat: AbstractChat, context: Context): ChatListItemData {
            val accountJid = abstractChat.account
            val contactJid = abstractChat.contactJid
            val rosterContact = RosterManager.getInstance().getRosterContact(accountJid, contactJid)

            return ChatListItemData(
                avatar = getAvatar(abstractChat),
                contactStatusLevel = StatusBadgeSetupHelper.getStatusImageLevel(abstractChat), //todo fix
                accountColorIndicator = getAccountColorIndicatorColor(abstractChat),
                nickname = getNickname(abstractChat, context.resources),
                notificationMode = getNotificationMode(abstractChat),
                unreadCount = abstractChat.unreadMessageCount,
                isNotifyAboutMessage = abstractChat.notifyAboutMessage(),
                time = getTime(abstractChat, context),
                messageStatus = abstractChat.lastMessage?.messageStatus ?: MessageStatus.NONE,
                messageText = "", //todo fix
                isRosterContact =
                (rosterContact != null && !rosterContact.isDirtyRemoved)
                        || !VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid),
                isBlocked = BlockingManager.getInstance().contactIsBlockedLocally(accountJid, contactJid),
                isCustomNotification =
                CustomNotifyPrefsManager.getInstance().isPrefsExist(Key.createKey(accountJid, contactJid)),
                isStatusBadgeVisible = StatusBadgeSetupHelper.isStatusVisibile(abstractChat),
                isStatusBadgeFiltered = StatusBadgeSetupHelper.isStatusBadgeFiltered(abstractChat),
            )
        }

    }

}