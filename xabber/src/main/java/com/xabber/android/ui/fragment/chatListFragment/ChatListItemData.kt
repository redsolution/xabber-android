package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import androidx.core.os.ConfigurationCompat
import com.xabber.android.R
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.chat_state.ChatStateManager
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.text.*
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

        private fun getMessageText(
            chat: AbstractChat, context: Context, isSavedMessagesSpecialText: Boolean = false
        ): String {
            val account = chat.account
            val contact = chat.contactJid

            val lastMessage = chat.lastMessage
            val forwardedCount = lastMessage?.forwardedIds?.size ?: 0
            val attachmentsCount = lastMessage?.attachmentRealmObjects?.size ?: 0

            val color700 =
                ColorManager.getInstance().accountPainter.getAccountColorWithTint(account, 700)

            val color500 =
                ColorManager.getInstance().accountPainter.getAccountColorWithTint(account, 500)

            val coloredMemberNameOrNull =
                when {
                    lastMessage?.groupchatUserId == null || !lastMessage.isIncoming -> {
                        ((chat as? GroupChat)?.let { GroupMemberManager.getMe(it)?.nickname } + ":")
                    }
                    else -> {
                        GroupMemberManager.getGroupMemberById(
                            lastMessage.account, lastMessage.user, lastMessage.groupchatUserId
                        )?.bestName + ":"
                    }
                }.wrapWithColorTag(color700)

            val noMessagesText =
                context.resources.getString(R.string.no_messages).wrapWithItalicTag()

            fun String.tryToDecode() =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    try {
                        Html.fromHtml(getDecodedSpannable(this).toString())
                    } catch (e: Exception) {
                        Html.fromHtml(this)
                    }
                } else this

            return when {
                GroupInviteManager.hasActiveIncomingInvites(account, contact)
                        && chat is GroupChat -> {
                    context.getString(
                        R.string.groupchat_invitation_to_group_chat,
                        chat.privacyType.getLocalizedString()
                            .replaceFirstChar {
                                it.lowercase(
                                    ConfigurationCompat.getLocales(context.resources.configuration)[0]
                                )
                            }
                    )
                        .wrapWithColorTag(color500)
                        .wrapWithItalicTag()
                }

                lastMessage == null -> noMessagesText

                chat.account.bareJid.toString() == chat.contactJid.bareJid.toString() && isSavedMessagesSpecialText -> {
                    context.getString(R.string.saved_messages__hint_forward_here)
                }

                chat is GroupChat -> when {
                    lastMessage.isGroupchatSystem -> lastMessage.text.wrapWithItalicTag()

                    forwardedCount > 0 -> when {
                        lastMessage.text.isNullOrEmpty() -> {
                            coloredMemberNameOrNull + String.format(
                                context.resources.getQuantityString(
                                    R.plurals.forwarded_messages_count,
                                    forwardedCount
                                ), forwardedCount
                            ).wrapWithColorTag(color500)
                        }
                        else -> {
                            coloredMemberNameOrNull + lastMessage.text.tryToDecode().toString()
                        }
                    }

                    attachmentsCount > 0 -> when {
                        lastMessage.text.isNullOrEmpty() -> {
                            coloredMemberNameOrNull + getColoredAttachmentDisplayName(
                                context, lastMessage.attachmentRealmObjects, color500
                            )
                        }
                        else -> {
                            coloredMemberNameOrNull + lastMessage.text.tryToDecode().toString()
                        }
                    }

                    !lastMessage.text.isNullOrEmpty() -> coloredMemberNameOrNull + lastMessage.text

                    else -> noMessagesText
                }

                chat is RegularChat -> when {
                    ChatStateManager.getInstance()
                        .getFullChatStateString(account, contact) != null -> {
                        ChatStateManager.getInstance().getFullChatStateString(account, contact)
                            .wrapWithColorTag(color500)
                    }

                    lastMessage.action != null -> {
                        lastMessage.action.wrapWithColorTag(color500).wrapWithItalicTag()
                    }

                    forwardedCount > 0 -> when {
                        lastMessage.text.isNullOrEmpty() -> {
                            String.format(
                                context.resources.getQuantityString(
                                    R.plurals.forwarded_messages_count,
                                    forwardedCount
                                ), forwardedCount
                            ).wrapWithColorTag(color500)
                        }
                        else -> lastMessage.text ?: noMessagesText
                    }

                    attachmentsCount > 0 ->
                        when {
                            lastMessage.text.isNullOrEmpty() -> {
                                getColoredAttachmentDisplayName(
                                    context, lastMessage.attachmentRealmObjects, color500
                                ) ?: ""
                            }
                            else -> lastMessage.text.tryToDecode().toString()
                        }

                    else -> lastMessage.text.tryToDecode().toString()
                }

                else -> noMessagesText
            }
        }

        private fun getNotificationMode(abstractChat: AbstractChat) =
            abstractChat.notificationState.determineModeByGlobalSettings()

        private fun getNickname(abstractChat: AbstractChat, resources: Resources): String {
            val accountJid = abstractChat.account
            val contactJid = abstractChat.contactJid
            return when {
                accountJid.bareJid.toString() == contactJid.bareJid.toString() ->
                    resources.getText(R.string.saved_messages__header).toString()
                abstractChat is GroupChat ->
                    abstractChat.name ?: RosterManager.getInstance()
                        .getBestContact(accountJid, contactJid).name
                else -> RosterManager.getInstance().getBestContact(accountJid, contactJid).name
            }
        }

        private fun getAvatar(abstractChat: AbstractChat): Drawable =
            if (abstractChat.account.bareJid.toString() == abstractChat.contactJid.bareJid.toString()) {
                AvatarManager.getInstance().getSavedMessagesAvatar(abstractChat.account)
            } else {
                RosterManager.getInstance()
                    .getAbstractContact(abstractChat.account, abstractChat.contactJid)
                    .getAvatar(true)
            }

        private fun getTime(abstractChat: AbstractChat, context: Context): String =
            when {
                GroupInviteManager.hasActiveIncomingInvites(
                    abstractChat.account,
                    abstractChat.contactJid
                ) -> {
                    Date(
                        GroupInviteManager.getLastInvite(
                            abstractChat.account,
                            abstractChat.contactJid
                        )!!.date
                    ).getSmartTimeTextForRoster()
                }
                abstractChat.lastMessage != null && abstractChat.lastMessage?.isValid ?: false -> {
                    Date(abstractChat.lastMessage!!.timestamp).getSmartTimeTextForRoster()
                }
                else -> ""
            }

        private fun getAccountColorIndicatorColor(abstractChat: AbstractChat) =
            ColorManager.getInstance().accountPainter.getAccountMainColor(abstractChat.account)

        fun createFromChat(
            abstractChat: AbstractChat,
            context: Context,
            isSavedMessagesSpecialText: Boolean = false
        ): ChatListItemData {
            val accountJid = abstractChat.account
            val contactJid = abstractChat.contactJid
            val rosterContact = RosterManager.getInstance().getRosterContact(accountJid, contactJid)

            return ChatListItemData(
                avatar = getAvatar(abstractChat),
                contactStatusLevel = StatusBadgeSetupHelper.getStatusImageLevel(abstractChat),
                accountColorIndicator = getAccountColorIndicatorColor(abstractChat),
                nickname = getNickname(abstractChat, context.resources),
                notificationMode = getNotificationMode(abstractChat),
                unreadCount = abstractChat.unreadMessageCount,
                isNotifyAboutMessage = abstractChat.notifyAboutMessage(),
                time = getTime(abstractChat, context),
                messageStatus = abstractChat.lastMessage?.messageStatus ?: MessageStatus.NONE,
                messageText = getMessageText(abstractChat, context, isSavedMessagesSpecialText),
                isRosterContact = (rosterContact != null && !rosterContact.isDirtyRemoved)
                        || !VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid),
                isBlocked = BlockingManager.getInstance()
                    .contactIsBlockedLocally(accountJid, contactJid),
                isCustomNotification =
                CustomNotifyPrefsManager.getInstance()
                    .isPrefsExist(Key.createKey(accountJid, contactJid)),
                isStatusBadgeVisible = StatusBadgeSetupHelper.isStatusVisibile(abstractChat),
                isStatusBadgeFiltered = StatusBadgeSetupHelper.isStatusBadgeFiltered(abstractChat),
            )
        }

    }

}