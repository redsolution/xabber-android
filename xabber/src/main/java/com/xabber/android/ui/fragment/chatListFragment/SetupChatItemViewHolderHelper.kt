package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.util.TypedValue
import android.view.View
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.cs.ChatStateManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils
import java.util.*

class SetupChatItemViewHolderHelper(val holder: ChatViewHolder, val contact: AbstractChat){

    fun setup(){
        holder.messageRealmObject = contact.lastMessage
        setupAccountColorIndicator(holder, contact)
        setupContactAvatar(holder, contact)
        setupRosterStatus(holder, contact)
        setupContactName(holder, contact)
//        setupNotificationMuteIcon(holder, contact)
        setupUnreadCount(holder, contact)
        setupTime(holder, contact)
        setupMessageText(holder, contact)
        setupMessageStatus(holder, contact)
    }

    private fun setupAccountColorIndicator(holder: ChatViewHolder, contact: AbstractChat) {
        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            val color: Int = ColorManager.getInstance().accountPainter
                    .getAccountMainColor(contact.account)
            holder.accountColorIndicatorView.setBackgroundColor(color)
            holder.accountColorIndicatorBackView.setBackgroundColor(color)
            holder.accountColorIndicator = color
            holder.accountColorIndicatorView.visibility = View.VISIBLE
            holder.accountColorIndicatorBackView.visibility = View.VISIBLE
        } else {
            holder.accountColorIndicatorView.visibility = View.INVISIBLE
            holder.accountColorIndicatorBackView.visibility = View.INVISIBLE
        }
    }

    private fun setupContactAvatar(holder: ChatViewHolder, contact: AbstractChat) {

        if (SettingsManager.contactsShowAvatars()) {
            holder.avatarIV.visibility = View.VISIBLE
            holder.avatarIV.setImageDrawable(RosterManager.getInstance()
                    .getAbstractContact(contact.account, contact.user)
                    .getAvatar(true))
        } else {
            holder.avatarIV.visibility = View.GONE
        }
    }

    private fun setupRosterStatus(holder: ChatViewHolder, contact: AbstractChat) {
        var statusLevel = RosterManager.getInstance()
                .getAbstractContact(contact.account, contact.user).statusMode.statusLevel
        holder.rosterStatus = statusLevel

        val chat = ChatManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val isServer = contact.user.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance().contactIsBlockedLocally(contact.account, contact.user)

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 10
            chat.isGroupchat -> statusLevel = 9
        }
        if (statusLevel == 11) {
            if (holder.avatarIV.visibility == View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.avatarIV.imageAlpha = 128
                } else {
                    holder.avatarIV.alpha = 0.5f
                }
            }
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context, R.attr.contact_list_contact_second_line_text_color))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holder.avatarIV.imageAlpha = 255
            } else {
                holder.avatarIV.alpha = 1.0f
            }
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context, R.attr.contact_list_contact_name_text_color))
        }
        holder.statusIV.setImageLevel(statusLevel)
        holder.statusIV.visibility = if (statusLevel == StatusMode.unavailable.ordinal && holder.avatarIV.visibility == View.VISIBLE)
            View.INVISIBLE else View.VISIBLE
        // holder.onlyStatusIV.setImageLevel(statusLevel)
    }

    private fun setupContactName(holder: ChatViewHolder, contact: AbstractChat) {
        holder.contactNameTV.text = RosterManager.getInstance()
                .getBestContact(contact.account, contact.user).name

    }

    private fun setupNotificationMuteIcon(holder: ChatViewHolder, contact: AbstractChat) {
        val resources = holder.itemView.context.resources
        val chat = ChatManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val isCustomNotification = CustomNotifyPrefsManager.getInstance()
                .isPrefsExist(Key.createKey(contact.account, contact.user))
        var iconId = 0
        val mode = chat.notificationState.determineModeByGlobalSettings()
        if (mode == NotificationState.NotificationMode.enabled) iconId = R.drawable.ic_unmute
        else if (mode == NotificationState.NotificationMode.disabled) iconId = R.drawable.ic_mute
        else if (mode != NotificationState.NotificationMode.bydefault) iconId = R.drawable.ic_snooze_mini
        holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (iconId != 0) resources.getDrawable(iconId) else null, null)
        if (isCustomNotification && (mode.equals(NotificationState.NotificationMode.enabled)
                        || mode.equals(NotificationState.NotificationMode.bydefault)))
            holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    resources.getDrawable(R.drawable.ic_notif_custom), null)
        holder.unreadCountTV.background.mutate().clearColorFilter()
        holder.unreadCountTV.setTextColor(resources.getColor(R.color.white))
    }

    private fun setupUnreadCount(holder: ChatViewHolder, contact: AbstractChat) {
        val chat = ChatManager.getInstance().getOrCreateChat(contact.account, contact.user)
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

    private fun setupTime(holder: ChatViewHolder, contact: AbstractChat) {
        holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context, Date(contact.lastMessage!!.timestamp))
        holder.timeTV.visibility = View.VISIBLE
    }

    private fun setupMessageText(holder: ChatViewHolder, contact: AbstractChat) {
        val lastMessage = contact.lastMessage
        val context = holder.itemView.context
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size
        if (text.isNullOrEmpty()) {
            if (ChatStateManager.getInstance().getFullChatStateString(contact.account, contact.user) != null)
                holder.messageTextTV.text = ChatStateManager.getInstance()
                        .getFullChatStateString(contact.account, contact.user)
            else if (forwardedCount != null && forwardedCount > 0) holder.messageTextTV.text = String
                    .format(context.resources.getString(R.string.forwarded_messages_count), forwardedCount)
            else if (lastMessage != null && lastMessage.haveAttachments()) holder.messageTextTV.text = lastMessage.attachmentRealmObjects[0]?.title
            else holder.messageTextTV.text = context.resources.getString(R.string.no_messages)
            holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
        } else {
            holder.messageTextTV.typeface = Typeface.DEFAULT
            holder.messageTextTV.visibility = View.VISIBLE
            if (OTRManager.getInstance().isEncrypted(text)) {
                holder.messageTextTV.text = context.getText(R.string.otr_not_decrypted_message)
                holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
            } else {
                try {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        try {
                            holder.messageTextTV.text = Html
                                    .fromHtml(Utils.getDecodedSpannable(text).toString())
                        } catch (e: Exception) {
                            holder.messageTextTV.text = Html.fromHtml(text)
                        }
                    } else holder.messageTextTV.text = text
                } catch (e: Throwable) {
                    LogManager.exception(this.javaClass.simpleName, e)
                    holder.messageTextTV.text = text
                }
                holder.messageTextTV.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun setupMessageStatus(holder: ChatViewHolder, contact: AbstractChat) {

        val lastMessage = contact.lastMessage
        holder.messageStatusTV.visibility = if (lastMessage?.text == null || lastMessage.isIncoming)
            View.INVISIBLE else View.VISIBLE
        if (lastMessage != null) {
            holder.messageStatusTV.setImageResource(getMEssageStatusImage(lastMessage))
        }
    }

    fun getMEssageStatusImage(messageRealmObject: MessageRealmObject): Int{
        if (!messageRealmObject.isIncoming) {
            if (MessageRealmObject.isUploadFileMessage(messageRealmObject) && !messageRealmObject.isSent
                    && System.currentTimeMillis() - messageRealmObject.timestamp > 1000)
                return R.drawable.ic_message_not_sent_14dp
            else if (messageRealmObject.isDisplayed || messageRealmObject.isReceivedFromMessageArchive)
                return R.drawable.ic_message_displayed
            else if (messageRealmObject.isDelivered)
                return R.drawable.ic_message_delivered_14dp
            else if (messageRealmObject.isError)
                return R.drawable.ic_message_has_error_14dp
            else if (messageRealmObject.isAcknowledged || messageRealmObject.isForwarded)
                return R.drawable.ic_message_acknowledged_14dp
            else return R.drawable.ic_message_not_sent_14dp
        } else return R.drawable.ic_message_not_sent_14dp
    }

    private fun getThemeResource(context: Context, themeResourceId: Int): Int {
        val a = context.obtainStyledAttributes(TypedValue().data, intArrayOf(themeResourceId))
        val accountGroupColorsResourceId = a.getResourceId(0, 0)
        a.recycle()
        return accountGroupColorsResourceId
    }

}