package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.view.View
import androidx.core.os.ConfigurationCompat
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.cs.ChatStateManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.groups.GroupInviteManager
import com.xabber.android.data.groups.GroupMemberManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatAction
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.helper.MessageDeliveryStatusHelper
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils
import java.util.*

class SetupChatItemViewHolderHelper(val holder: ChatViewHolder, val contact: AbstractChat) {

    fun setup() {
        holder.messageRealmObject = contact.lastMessage
        setupAccountColorIndicator(holder, contact)
        setupContactAvatar(holder, contact)
        setupStatusBadge(holder, contact)
        setupContactName(holder, contact)
        setupNotificationMuteIcon(holder, contact)
        setupUnreadCount(holder, contact)
        setupTime(holder, contact)
        setupMessageText(holder, contact)
        setupMessageStatus(holder, contact)
    }

    private fun setupAccountColorIndicator(holder: ChatViewHolder, chat: AbstractChat) {
        val color: Int = ColorManager.getInstance().accountPainter
                .getAccountMainColor(chat.account)
        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            holder.accountColorIndicatorView.setBackgroundColor(color)
            holder.accountColorIndicatorBackView.setBackgroundColor(color)
            holder.accountColorIndicator = color
            holder.accountColorIndicatorView.visibility = View.VISIBLE
            holder.accountColorIndicatorBackView.visibility = View.VISIBLE
        } else {
            holder.accountColorIndicator = color
            holder.accountColorIndicatorView.visibility = View.INVISIBLE
            holder.accountColorIndicatorBackView.visibility = View.INVISIBLE
        }
    }

    private fun setupContactAvatar(holder: ChatViewHolder, chat: AbstractChat) {

        if (SettingsManager.contactsShowAvatars()) {
            holder.avatarIV.visibility = View.VISIBLE
            holder.avatarIV.setImageDrawable(RosterManager.getInstance()
                    .getAbstractContact(chat.account, chat.contactJid)
                    .getAvatar(true))
        } else {
            holder.avatarIV.visibility = View.GONE
        }
    }

    private fun setupStatusBadge(holder: ChatViewHolder, chat: AbstractChat) =
            StatusBadgeSetupHelper.setupImageViewForChat(chat, holder.statusIV)

    private fun setupContactName(holder: ChatViewHolder, chat: AbstractChat) {
        if (chat is GroupChat)
            holder.contactNameTV.text = chat.name
                    ?: RosterManager.getInstance().getBestContact(chat.account, chat.contactJid).name
        else holder.contactNameTV.text = RosterManager.getInstance().getBestContact(chat.account, chat.contactJid).name

        val accountJid = chat.account
        val contactJid = chat.contactJid
        val rosterContact = RosterManager.getInstance().getRosterContact(accountJid, contactJid)
        val statusLevel = rosterContact?.statusMode?.statusLevel

        val isBlocked = BlockingManager.getInstance()
                .contactIsBlockedLocally(accountJid, contactJid)
        val isRosterContact = (rosterContact != null && !rosterContact.isDirtyRemoved)
                || !VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid)

        if (isBlocked || (!isRosterContact && statusLevel != null && statusLevel < 8)) {
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context,
                    R.attr.contact_list_contact_second_line_text_color))
        } else {
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context,
                    R.attr.contact_list_contact_name_text_color))
        }
    }

    private fun setupNotificationMuteIcon(holder: ChatViewHolder, chat: AbstractChat) {

        val resources = holder.itemView.context.resources
        val isCustomNotification = CustomNotifyPrefsManager.getInstance()
                .isPrefsExist(Key.createKey(chat.account, chat.contactJid))
        val mode = chat.notificationState.determineModeByGlobalSettings()
        val iconId = when (mode) {
            NotificationState.NotificationMode.enabled -> R.drawable.ic_unmute
            NotificationState.NotificationMode.disabled -> R.drawable.ic_mute
            NotificationState.NotificationMode.byDefault -> 0
            else -> R.drawable.ic_snooze_mini
        }

        holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (iconId != 0) resources.getDrawable(iconId) else null, null)

        if (isCustomNotification && (mode == NotificationState.NotificationMode.enabled
                        || mode == NotificationState.NotificationMode.byDefault))
            holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    resources.getDrawable(R.drawable.ic_notif_custom), null)

        holder.unreadCountTV.background.mutate().clearColorFilter()
        holder.unreadCountTV.setTextColor(resources.getColor(R.color.white))
    }

    private fun setupUnreadCount(holder: ChatViewHolder, chat: AbstractChat) {
        val unreadCount = chat.unreadMessageCount
        val resources = holder.itemView.resources
        if (unreadCount > 0) {
            holder.unreadCountTV.text = unreadCount.toString()
            holder.unreadCountTV.visibility = View.VISIBLE
        } else holder.unreadCountTV.visibility = View.GONE

        if (!chat.notifyAboutMessage()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                holder.unreadCountTV.background.mutate()
                        .setColorFilter(resources.getColor(R.color.grey_500), PorterDuff.Mode.SRC_IN)
                holder.unreadCountTV.setTextColor(resources.getColor(R.color.grey_100))
            } else {
                holder.unreadCountTV.background.mutate()
                        .setColorFilter(resources.getColor(R.color.grey_700), PorterDuff.Mode.SRC_IN)
                holder.unreadCountTV.setTextColor(resources.getColor(R.color.black))
            }
        } else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            holder.unreadCountTV.background.mutate().clearColorFilter()

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
            holder.unreadCountTV.setTextColor(resources.getColor(R.color.grey_200))
    }

    private fun setupTime(holder: ChatViewHolder, chat: AbstractChat) {
        holder.timeTV.visibility = View.VISIBLE
        when {
            GroupInviteManager.hasActiveIncomingInvites(chat.account, chat.contactJid) -> {
                holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context,
                        Date(GroupInviteManager.getLastInvite(chat.account, chat.contactJid)!!.date))
            }
            chat.lastMessage != null && chat.lastMessage?.isValid ?: false -> {
                holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context,
                        Date(chat.lastMessage!!.timestamp))
            }
            else -> holder.timeTV.visibility = View.INVISIBLE
        }
    }

    private fun setupMessageText(holder: ChatViewHolder, chat: AbstractChat) {
        val context = holder.itemView.context
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

        fun colorizeIfPossible(){
            if (holder.accountColorIndicator != null){
                holder.messageTextTV.text = Html.fromHtml(
                        StringUtils.getColoredText(holder.messageTextTV.text.toString(), holder.accountColorIndicator!!))
            }
        }

        fun setItalicTypeface(){
            holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
        }

        fun setDefaultTypeface(){
            holder.messageTextTV.typeface = Typeface.DEFAULT
        }

        fun setIncomingInvite(){
            holder.messageTextTV.text = context.getString(R.string.groupchat_invitation_to_group_chat,
                    (chat as GroupChat).privacyType.getLocalizedString()
                    .decapitalize(ConfigurationCompat.getLocales(context.resources.configuration)[0]))

            colorizeIfPossible()
            setItalicTypeface()
        }

        fun setGroupSystem(){
            holder.messageTextTV.text = lastMessage?.text
            setItalicTypeface()
        }

        fun setGroupRegular(){
            val nickname = GroupMemberManager.getInstance().getGroupMemberById(lastMessage!!.groupchatUserId)?.nickname
            val sender = StringUtils.getColoredText((nickname ?: "") + ":", holder.accountColorIndicator!!)
            holder.messageTextTV.text = Html.fromHtml("$sender ${getDecodedTextIfPossible()}")
        }

        fun setChatState(){
            holder.messageTextTV.text = ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.contactJid)

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setToForwarded(){
            holder.messageTextTV.text = String.format(context.resources.getQuantityString(
                    R.plurals.forwarded_messages_count, forwardedCount ?: 0),
                    forwardedCount)

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setToAttachments(){
            holder.messageTextTV.text = StringUtils.getAttachmentDisplayName(context, lastMessage!!.attachmentRealmObjects[0])

            colorizeIfPossible()
            setDefaultTypeface()
        }

        fun setNoMessages(){
            holder.messageTextTV.text = context.resources.getString(R.string.no_messages)
            setItalicTypeface()
        }

        fun setAction(){
            holder.messageTextTV.text = lastMessage?.text
            setItalicTypeface()
        }

        fun setEncrypted(){
            holder.messageTextTV.text = context.getText(R.string.otr_not_decrypted_message)
            setItalicTypeface()
        }

        fun setRegular(){
            holder.messageTextTV.text = getDecodedTextIfPossible()
            setDefaultTypeface()
        }

        if (chat is GroupChat){
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
            when{
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
                lastMessage!= null && lastMessage.action != null && lastMessage.action != ChatAction.contact_deleted.toString() -> {
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

    private fun setupMessageStatus(holder: ChatViewHolder, chat: AbstractChat) {
        if (chat.lastMessage != null) {
            MessageDeliveryStatusHelper.setupStatusImageView(chat.lastMessage!!, holder.messageStatusTV)
        }
    }

}