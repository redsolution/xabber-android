package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.extension.chat_state.ChatStateManager
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatAction
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import com.xabber.android.ui.helper.MessageDeliveryStatusHelper
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils
import github.ankushsachdeva.emojicon.EmojiconTextView

class ChatViewHolder(
    itemView: View, private val onAvatarClickListener: View.OnClickListener
) : RecyclerView.ViewHolder(itemView) {

    private val avatarIV: ImageView = itemView.findViewById(R.id.ivAvatar)
    private val statusIV: ImageView = itemView.findViewById(R.id.ivStatus)
    private val contactNameTV: TextView = itemView.findViewById(R.id.tvContactName)
    private val messageTextTV: EmojiconTextView = itemView.findViewById(R.id.tvMessageText)
    private val timeTV: TextView = itemView.findViewById(R.id.tvTime)
    private val messageStatusTV: ImageView = itemView.findViewById(R.id.ivMessageStatus)
    private val unreadCountTV: TextView = itemView.findViewById(R.id.tvUnreadCount)
    private val accountColorIndicatorBackView: View = itemView.findViewById(R.id.accountColorIndicatorBack)
    private val accountColorIndicatorView: View = itemView.findViewById(R.id.accountColorIndicator)

    fun bind(chatListItemData: ChatListItemData, isSavedMessageMode: Boolean = false) {
        setupContactAvatar(chatListItemData)
        setupAccountColorIndicator(chatListItemData)
        setupContactName(chatListItemData)
        setupNotificationMuteIcon(chatListItemData)
        setupUnreadCount(chatListItemData)
        setupTime(chatListItemData)
        setupMessageStatus(chatListItemData)
        setupStatusBadge(chatListItemData)
        //setupMessageText(contact, isSavedMessageMode, chatListItemData)
    }

    private fun setupAccountColorIndicator(itemData: ChatListItemData) {
        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            accountColorIndicatorView.setBackgroundColor(itemData.accountColorIndicator)
            accountColorIndicatorBackView.setBackgroundColor(itemData.accountColorIndicator)
            accountColorIndicatorView.visibility = View.VISIBLE
            accountColorIndicatorBackView.visibility = View.VISIBLE
        } else {
            accountColorIndicatorView.visibility = View.INVISIBLE
            accountColorIndicatorBackView.visibility = View.INVISIBLE
        }

        //todo make decision about back view
    }

    private fun setupContactAvatar(itemData: ChatListItemData) {
        avatarIV.setOnClickListener(onAvatarClickListener)
        if (SettingsManager.contactsShowAvatars()) {
            avatarIV.visibility = View.VISIBLE
            avatarIV.setImageDrawable(itemData.avatar)
        } else avatarIV.visibility = View.GONE
    }

    private fun setupStatusBadge(chatListItemData: ChatListItemData) =
        StatusBadgeSetupHelper.setupImageView(
            statusIV,
            chatListItemData.contactStatusLevel,
            chatListItemData.isStatusBadgeVisible,
            chatListItemData.isStatusBadgeFiltered
        )

    private fun setupContactName(chatListItemData: ChatListItemData) {
        contactNameTV.text = chatListItemData.nickname
        if (chatListItemData.isBlocked || (!chatListItemData.isRosterContact && chatListItemData.contactStatusLevel < 8)) {
            contactNameTV.setTextColor(
                Utils.getAttrColor(contactNameTV.context, R.attr.contact_list_contact_second_line_text_color)
            )
        } else {
            contactNameTV.setTextColor(
                Utils.getAttrColor(contactNameTV.context, R.attr.contact_list_contact_name_text_color)
            )
        }
    }

    private fun setupNotificationMuteIcon(chatListItemData: ChatListItemData) {
        val resources = itemView.context.resources
        val iconId = when (chatListItemData.notificationMode) {
            NotificationState.NotificationMode.enabled -> R.drawable.ic_unmute
            NotificationState.NotificationMode.disabled -> R.drawable.ic_mute
            NotificationState.NotificationMode.byDefault -> 0
            else -> R.drawable.ic_snooze_mini
        }

        contactNameTV.setCompoundDrawablesWithIntrinsicBounds(
            null, null, if (iconId != 0) resources.getDrawable(iconId) else null, null
        )

        if (chatListItemData.isCustomNotification
            && (chatListItemData.notificationMode == NotificationState.NotificationMode.enabled
                    || chatListItemData.notificationMode == NotificationState.NotificationMode.byDefault)
        )
            contactNameTV.setCompoundDrawablesWithIntrinsicBounds(
                null, null, resources.getDrawable(R.drawable.ic_notif_custom), null
            )

        unreadCountTV.background.mutate().clearColorFilter()
        unreadCountTV.setTextColor(resources.getColor(R.color.white))
    }

    private fun setupUnreadCount(chatListItemData: ChatListItemData) {
        val resources = itemView.resources
        if (chatListItemData.unreadCount > 0) {
            unreadCountTV.text = chatListItemData.unreadCount.toString()
            unreadCountTV.visibility = View.VISIBLE
        } else unreadCountTV.visibility = View.GONE

        if (!chatListItemData.isNotifyAboutMessage) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                unreadCountTV.background.mutate().setColorFilter(
                    resources.getColor(R.color.grey_500), PorterDuff.Mode.SRC_IN
                )
                unreadCountTV.setTextColor(resources.getColor(R.color.grey_100))
            } else {
                unreadCountTV.background.mutate().setColorFilter(
                    resources.getColor(R.color.grey_700), PorterDuff.Mode.SRC_IN
                )
                unreadCountTV.setTextColor(resources.getColor(R.color.black))
            }
        } else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            unreadCountTV.background.mutate().clearColorFilter()

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
            unreadCountTV.setTextColor(resources.getColor(R.color.grey_200))
    }

    private fun setupTime(chatListItemData: ChatListItemData) {
        if (chatListItemData.time.isNotEmpty()) {
            timeTV.visibility = View.VISIBLE
            timeTV.text = chatListItemData.time
        } else timeTV.visibility = View.INVISIBLE
    }

    private fun setupMessageText(
        chat: AbstractChat,
        isSavedMessagesChatSpecialText: Boolean = false,
        chatListItemData: ChatListItemData
    ) {
        val context = itemView.context

        if (isSavedMessagesChatSpecialText && chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()) {
            messageTextTV.text = context.getString(R.string.saved_messages__hint_forward_here)
            return
        }

        val lastMessage = chat.lastMessage
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size

        fun getDecodedTextIfPossible() =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                try {
                    Html.fromHtml(Utils.getDecodedSpannable(text).toString())
                } catch (e: Exception) {
                    Html.fromHtml(text)
                }
            } else text

        fun colorizeIfPossible() {
            messageTextTV.text = Html.fromHtml(
                StringUtils.getColoredText(messageTextTV.text.toString(), chatListItemData.accountColorIndicator)
            )
        }

        fun setItalicTypeface() {
            messageTextTV.setTypeface(messageTextTV.typeface, Typeface.ITALIC)
        }

        fun setDefaultTypeface() {
            messageTextTV.typeface = Typeface.DEFAULT
        }

        fun setIncomingInvite() {
            messageTextTV.text = context.getString(
                R.string.groupchat_invitation_to_group_chat,
                (chat as GroupChat).privacyType.getLocalizedString()
                    .decapitalize(ConfigurationCompat.getLocales(context.resources.configuration)[0])
            )

            colorizeIfPossible()
            setItalicTypeface()
        }

        fun setGroupSystem() {
            messageTextTV.text = lastMessage?.text
            setItalicTypeface()
        }

        fun setGroupRegular() {
            val nickname =
                if (lastMessage != null && lastMessage.groupchatUserId != null) {
                    GroupMemberManager.getGroupMemberById(lastMessage.groupchatUserId)?.nickname
                } else null
            val sender = StringUtils.getColoredText((nickname ?: "") + ":", chatListItemData.accountColorIndicator)
            messageTextTV.text = Html.fromHtml("$sender ${getDecodedTextIfPossible()}")
        }

        fun setChatState() {
            messageTextTV.text =
                ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.contactJid)

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setToForwarded() {
            messageTextTV.text = String.format(
                context.resources.getQuantityString(
                    R.plurals.forwarded_messages_count, forwardedCount ?: 0
                ),
                forwardedCount
            )

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setToAttachments() {
            messageTextTV.text = StringUtils.getColoredAttachmentDisplayName(
                context, lastMessage?.attachmentRealmObjects ?: return, chatListItemData.accountColorIndicator
            )

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setNoMessages() {
            messageTextTV.text = context.resources.getString(R.string.no_messages)
            setItalicTypeface()
        }

        fun setAction() {
            messageTextTV.text = lastMessage?.text
            setItalicTypeface()
        }

        fun setEncrypted() {
            messageTextTV.text = context.getText(R.string.otr_not_decrypted_message)
            setItalicTypeface()
        }

        fun setRegular() {
            messageTextTV.text = getDecodedTextIfPossible()
            setDefaultTypeface()
        }

        if (chat is GroupChat) {
            when {
                lastMessage == null && GroupInviteManager.hasActiveIncomingInvites(chat.account, chat.contactJid) -> {
                    setIncomingInvite()
                    return
                }
                lastMessage != null && lastMessage.isGroupchatSystem -> {
                    setGroupSystem()
                    return
                }
                //todo group message with attachment
                //todo group message with fwr
                lastMessage != null -> setGroupRegular()
                else -> setNoMessages()
            }
        } else {
            when {
                ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.contactJid) != null -> {
                    setChatState()
                    return
                }
                forwardedCount != null && forwardedCount > 0 -> {
                    setToForwarded()
                    return
                }
                lastMessage != null && lastMessage.haveAttachments() -> {
                    setToAttachments()
                    return
                }
                OTRManager.getInstance().isEncrypted(text) -> {
                    setEncrypted()
                    return
                }
                lastMessage != null && lastMessage.action != null && lastMessage.action != ChatAction.contact_deleted.toString() -> {
                    setAction()
                    return
                }
                lastMessage == null -> {
                    setNoMessages()
                    return
                }
                else -> setRegular()
            }
        }
    }

    private fun setupMessageStatus(chatListItemData: ChatListItemData) {
        messageStatusTV.visibility =
            if (chatListItemData.messageStatus != MessageStatus.NONE) {
                View.VISIBLE
            } else View.INVISIBLE

        messageStatusTV.setImageResource(
            MessageDeliveryStatusHelper.getMessageStatusIconResourceByStatus(chatListItemData.messageStatus)
        )
    }

}