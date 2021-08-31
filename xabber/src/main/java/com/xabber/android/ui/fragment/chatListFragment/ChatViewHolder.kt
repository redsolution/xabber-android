package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.PorterDuff
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import com.xabber.android.ui.helper.MessageDeliveryStatusHelper
import com.xabber.android.utils.getAttrColor
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

    fun bind(chatListItemData: ChatListItemData) {
        setupContactAvatar(chatListItemData)
        setupAccountColorIndicator(chatListItemData)
        setupContactName(chatListItemData)
        setupNotificationMuteIcon(chatListItemData)
        setupUnreadCount(chatListItemData)
        setupTime(chatListItemData)
        setupMessageStatus(chatListItemData)
        setupStatusBadge(chatListItemData)
        setupMessageText(chatListItemData)
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
                R.attr.contact_list_contact_second_line_text_color.getAttrColor(
                    contactNameTV.context
                )
            )
        } else {
            contactNameTV.setTextColor(
                R.attr.contact_list_contact_name_text_color.getAttrColor(contactNameTV.context)
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

    private fun setupMessageText(chatListItemData: ChatListItemData) {
        messageTextTV.text = Html.fromHtml(chatListItemData.messageText)
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