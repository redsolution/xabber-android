package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.messagerealm.MessageItem
import com.xabber.android.data.extension.cs.ChatStateManager
import com.xabber.android.data.extension.muc.MUCManager
import com.xabber.android.data.extension.muc.RoomChat
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.AbstractContact
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.utils.StringUtils
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ChatItemVO(val contact: AbstractContact){
    val accountColorIndicator = setupAccountColorIndicator()
    val accountColorIndicatorVisibility = setupAccountColorIndicatorVisibility()
    val contactAvatarDrawable = setupContactAvatarDrawable()
    val contactAvatarVisibility = setupContactAvatarVisibility()
    val contactOnlyStatusVisibility = setupContactOnlyStatusVisibility()
    val contactStatusVisibility = setupContactStatusVisibility()
    val contactStatusLevel = setupContactStatusLevel()
    val contactName = setupContactName()
    val groupchatIndicatorDrawable = setupGroupchatIndicatorDrawable()
    val groupchatIndicatorVisibility = setupGroupchatIndicatorVisibility()
    val notificationMuteIcon = setupNotificationMuteIcon()
    val unreadCount = setupUnreadCount()
    val unreadCountVisibility = setupUnreadCountVisibility()
    val lastMessageTime = setupLastMessageTime()
    val messageText = setupMessageText()
    val messageSpannedText = setupMessageSpannedText()
    val messageTextColor = setupMessageTextColor()
    val messageStatusDrawable = setupMessageStatusDrawable()
    val messageStatusDrawableVisibility = setupMessageStatusDrawableVisibility()

    private fun setupAccountColorIndicator() = ColorManager.getInstance().accountPainter
            .getAccountMainColor(contact.account)

    private fun setupAccountColorIndicatorVisibility() =
            if (AccountManager.getInstance().enabledAccounts.size > 1) View.VISIBLE else View.INVISIBLE

    private fun setupContactAvatarDrawable() = contact.getAvatar(true)

    private fun setupContactAvatarVisibility() = if (SettingsManager.contactsShowAvatars()) View.VISIBLE
            else View.GONE

    private fun setupContactOnlyStatusVisibility() = if (SettingsManager.contactsShowAvatars()) View.GONE
            else View.VISIBLE

    private fun setupContactStatusVisibility() : Int {
        val mucIndicatorLevel = if (MUCManager.getInstance().hasRoom(contact.account, contact.user)) 1
        else if (MUCManager.getInstance().isMucPrivateChat(contact.account, contact.user)) 3 else 0
        val statusLevel = contact.statusMode.statusLevel
        val isServer = contact.user.jid.isDomainBareJid
        val isGroupchat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .isGroupchat
        if (((statusLevel == 6 || contact.user.jid.isDomainBareJid) ||
                        (mucIndicatorLevel != 0 && statusLevel != 1))
                || !SettingsManager.contactsShowAvatars() || isServer || isGroupchat ) return View.GONE
        else  return View.VISIBLE
    }

    private fun setupContactStatusLevel() = contact.statusMode.statusLevel

    private fun setupContactName() = contact.name

    private fun setupGroupchatIndicatorVisibility() : Int {
        val isServer = contact.user.jid.isDomainBareJid
        val isGroupchat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .isGroupchat
        return if (isGroupchat || isServer) View.VISIBLE else View.GONE
    }

    private fun setupGroupchatIndicatorDrawable(): Drawable? {
        val isServer = contact.user.jid.isDomainBareJid
        val context = Application.getInstance().applicationContext
        return if (isServer) context.resources.getDrawable(R.drawable.ic_server_14_border)
        else context.resources.getDrawable(R.drawable.ic_groupchat_14_border)
    }

    private fun setupNotificationMuteIcon(): Int?{
        val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val isCustomNotification = CustomNotifyPrefsManager.getInstance()
                .isPrefsExist(Key.createKey(contact.account, contact.user))
        val mode = chat.notificationState.determineModeByGlobalSettings(chat is RoomChat)

        if (mode == NotificationState.NotificationMode.enabled) return R.drawable.ic_unmute
        if (mode == NotificationState.NotificationMode.disabled) return R.drawable.ic_mute
        if (mode != NotificationState.NotificationMode.bydefault) return R.drawable.ic_snooze_mini

        if (isCustomNotification && (mode.equals(NotificationState.NotificationMode.enabled)
                        || mode.equals(NotificationState.NotificationMode.bydefault)))
            return R.drawable.ic_notif_custom
        return null
    }

    private fun setupUnreadCount() = MessageManager.getInstance()
            .getOrCreateChat(contact.account, contact.user).unreadMessageCount

    private fun setupUnreadCountVisibility() = if (setupUnreadCount() > 0) View.VISIBLE else View.GONE

    private fun setupLastMessageTime() = StringUtils
            .getSmartTimeTextForRoster(Application.getInstance().applicationContext,
                    MessageManager.getInstance().getOrCreateChat(contact.account, contact.user).lastTime)

    private fun setupMessageText(): String? {
        val lastMessage = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .lastMessage
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size

        if (text != null && text.isEmpty()){
            if (ChatStateManager.getInstance().getFullChatStateString(contact.account, contact.user) != null)
                return ChatStateManager.getInstance().getFullChatStateString(contact.account, contact.user)
            if (forwardedCount != null && forwardedCount > 0)
                return String.format(Application.getInstance().applicationContext.getString(R.string.forwarded_messages_count), forwardedCount)
            if (lastMessage.haveAttachments()){
                if (lastMessage.attachments.size > 1)
                    return String.format(Application.getInstance().applicationContext.getString(R.string.message_has_many_attachments), lastMessage.attachments.size)
                else return lastMessage.attachments[0].title
            }
            return Application.getInstance().getString(R.string.no_messages)
        } else {
            if (OTRManager.getInstance().isEncrypted(text))
                return Application.getInstance().applicationContext.getString(R.string.otr_not_decrypted_message)
            return text
        }
    }

    private fun setupMessageSpannedText(): Spanned? {
        val text = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user).lastMessage?.markupText
        if (text == null) return null
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            try {
                return Html.fromHtml(URLDecoder.decode(text, StandardCharsets.UTF_8.name()))
            } catch (e: Exception) { return Html.fromHtml(text) }
        } else return null
    }

    private fun setupMessageTextColor(): Int? {
        //TODO WEIRD SHIT
//        val lastMessage = MessageManager.getInstance()
//                .getOrCreateChat(contact.account, contact.user).lastMessage
//        if (lastMessage == null) return null
//        if (lastMessage.getText().isNotEmpty()) return null
//
//        if (lastMessage.haveAttachments() || lastMessage.forwardedIds.size > 0)
//            return ColorManager.getInstance().accountPainter.getAccountColorWithTint(contact.account, 600)
//        if (ChatStateManager.getInstance().getFullChatStateString(contact.account, contact.user) != null)
//            return ColorManager.getInstance().accountPainter.getAccountColorWithTint(contact.account, 300)

        return null
    }

    private fun setupMessageStatusDrawable(): Drawable? {
        val messageItem = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .lastMessage
        val resources = Application.getInstance().applicationContext.resources
        if (!messageItem!!.isIncoming) {
            if (MessageItem.isUploadFileMessage(messageItem) && !messageItem.isSent
                    && System.currentTimeMillis() - messageItem.timestamp > 1000)
                return resources.getDrawable(R.drawable.ic_message_not_sent_14dp)
            else if (messageItem.isDisplayed || messageItem.isReceivedFromMessageArchive)
                return resources.getDrawable(R.drawable.ic_message_displayed)
            else if (messageItem.isDelivered)
                return resources.getDrawable(R.drawable.ic_message_delivered_14dp)
            else if (messageItem.isError)
                return resources.getDrawable(R.drawable.ic_message_has_error_14dp)
            else if (messageItem.isAcknowledged || messageItem.isForwarded)
                return resources.getDrawable(R.drawable.ic_message_acknowledged_14dp)
            else return resources.getDrawable(R.drawable.ic_message_not_sent_14dp)
        } else return resources.getDrawable(R.drawable.ic_message_not_sent_14dp)

    }

    private fun setupMessageStatusDrawableVisibility(): Int {
        val lastMessage = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .lastMessage
        return if (lastMessage?.text!!.isEmpty() || lastMessage.isIncoming)
            View.INVISIBLE else View.VISIBLE
    }

}