package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
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
                    .getAbstractContact(chat.account, chat.user)
                    .getAvatar(true))
        } else {
            holder.avatarIV.visibility = View.GONE
        }
    }

    private fun setupRosterStatus(holder: ChatViewHolder, chat: AbstractChat) {
        var statusLevel = RosterManager.getInstance()
                .getAbstractContact(chat.account, chat.user).statusMode.statusLevel
        holder.rosterStatus = statusLevel

        val isServer = chat.user.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance()
                .contactIsBlockedLocally(chat.account, chat.user)
        val isVisible = holder.avatarIV.visibility == View.VISIBLE
        val isUnavailable = statusLevel == StatusMode.unavailable.ordinal
        val isAccountConnected = AccountManager.getInstance().connectedAccounts
                .contains(chat.account)
        val isGroupchat = chat.isGroupchat

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 10
            isGroupchat -> statusLevel = 9
        }

        if (statusLevel == 11) {
            if (holder.avatarIV.visibility == View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.avatarIV.imageAlpha = 128
                } else {
                    holder.avatarIV.alpha = 0.5f
                }
            }
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context,
                    R.attr.contact_list_contact_second_line_text_color))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holder.avatarIV.imageAlpha = 255
            } else {
                holder.avatarIV.alpha = 1.0f
            }
            holder.contactNameTV.setTextColor(Utils.getAttrColor(holder.contactNameTV.context,
                    R.attr.contact_list_contact_name_text_color))
        }

        holder.statusIV.setImageLevel(statusLevel)

        holder.statusIV.visibility =
                if (!isServer && !isGroupchat && !isBlocked && isVisible && (isUnavailable || !isAccountConnected))
                    View.INVISIBLE
                else
                    View.VISIBLE

        if ((isServer || isGroupchat) && !isAccountConnected){
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorFilter = ColorMatrixColorFilter(colorMatrix)
            holder.statusIV.colorFilter = colorFilter
        } else
            holder.statusIV.setColorFilter(0)

        // holder.onlyStatusIV.setImageLevel(statusLevel)
    }

    private fun setupContactName(holder: ChatViewHolder, chat: AbstractChat) {
        holder.contactNameTV.text = RosterManager.getInstance()
                .getBestContact(chat.account, chat.user).name
    }

    private fun setupNotificationMuteIcon(holder: ChatViewHolder, chat: AbstractChat) {

        val resources = holder.itemView.context.resources
        val isCustomNotification = CustomNotifyPrefsManager.getInstance()
                .isPrefsExist(Key.createKey(chat.account, chat.user))
        var iconId: Int
        val mode = chat.notificationState.determineModeByGlobalSettings()

        when (mode){
            NotificationState.NotificationMode.enabled -> iconId = R.drawable.ic_unmute
            NotificationState.NotificationMode.disabled -> iconId = R.drawable.ic_mute
            NotificationState.NotificationMode.bydefault -> iconId = 0
            else -> iconId = R.drawable.ic_snooze_mini
        }

        holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (iconId != 0) resources.getDrawable(iconId) else null, null)

        if (isCustomNotification && (mode == NotificationState.NotificationMode.enabled
                        || mode == NotificationState.NotificationMode.bydefault))
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
        if (chat.lastMessage != null){
            holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context,
                    Date(chat.lastMessage!!.timestamp))
            holder.timeTV.visibility = View.VISIBLE
        } else holder.timeTV.visibility = View.INVISIBLE
    }

    private fun setupMessageText(holder: ChatViewHolder, chat: AbstractChat) {
        val lastMessage = chat.lastMessage
        val context = holder.itemView.context
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size
        if (text.isNullOrEmpty()) {

            if (ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.user) != null)
                holder.messageTextTV.text = ChatStateManager.getInstance()
                        .getFullChatStateString(chat.account, chat.user)
            else if (forwardedCount != null && forwardedCount > 0) holder.messageTextTV.text = String
                    .format(context.resources.getString(R.string.forwarded_messages_count), forwardedCount)
            else if (lastMessage != null && lastMessage.haveAttachments()) {
                holder.messageTextTV.text = if (holder.accountColorIndicator != null)
                    Html.fromHtml(StringUtils.getColoredAttachmentDisplayName(context, lastMessage.attachmentRealmObjects[0], holder.accountColorIndicator!!))
                else
                    StringUtils.getAttachmentDisplayName(context, lastMessage.attachmentRealmObjects[0])
                holder.messageTextTV.typeface = Typeface.DEFAULT
                return
            } else
                holder.messageTextTV.text = context.resources.getString(R.string.no_messages)

            holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
        } else {
            holder.messageTextTV.typeface = Typeface.DEFAULT
            holder.messageTextTV.visibility = View.VISIBLE
            if (OTRManager.getInstance().isEncrypted(text)) {
                holder.messageTextTV.text = context.getText(R.string.otr_not_decrypted_message)
                holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
            } else {
                if (lastMessage.resource.equals("Groupchat") || (lastMessage.action != null && lastMessage.action.isNotEmpty()) ){
                    if (holder.accountColorIndicator != null){
                        holder.messageTextTV.text = Html.fromHtml(StringUtils
                                .getColoredText(lastMessage.text, holder.accountColorIndicator!!))
                        holder.messageTextTV.alpha = 0.6f
                    }
                    return
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
                    } finally {
                        holder.messageTextTV.alpha = 1f
                    }
                }
                holder.messageTextTV.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun setupMessageStatus(holder: ChatViewHolder, chat: AbstractChat) {
        val lastMessage = chat.lastMessage
        holder.messageStatusTV.visibility = if (lastMessage?.text == null || lastMessage.isIncoming)
            View.INVISIBLE else View.VISIBLE
        if (lastMessage != null) {
            holder.messageStatusTV.setImageResource(getMessageStatusImage(lastMessage))
        }
    }

    fun getMessageStatusImage(messageRealmObject: MessageRealmObject): Int{
        if (!messageRealmObject.isIncoming) {
            if (MessageRealmObject.isUploadFileMessage(messageRealmObject)
                    && !messageRealmObject.isSent
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