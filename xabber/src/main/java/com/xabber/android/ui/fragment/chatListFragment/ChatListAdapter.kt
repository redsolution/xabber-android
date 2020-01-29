package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.messagerealm.MessageItem
import com.xabber.android.data.extension.muc.MUCManager
import com.xabber.android.data.extension.muc.RoomChat
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.AbstractContact
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.utils.StringUtils
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class ChatListAdapter(val list: MutableList<AbstractContact>, val listener: ChatViewHolder.ChatItemClickListener) :
        RecyclerView.Adapter<ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
            ChatViewHolder(LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_chat_in_contact_list, parent, false), listener)

    override fun getItemCount(): Int = list.size

    fun getAbstractContactFromPosition(position: Int) = list[position]

    fun addItem(index: Int, item: AbstractContact) = list.add(index, item) // TODO maybe need notifyDatasetChanged

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val contact = list[position]
        setupAccountColorIndicator(holder, contact)
        setupContactAvatar(holder, contact)
        setupRosterStatus(holder, contact)
        setupContactName(holder, contact)
        setupMUCIndicator(holder, contact)
        setupGroupchatIndicator(holder, contact)
        setupNotificationMuteIcon(holder, contact)
        setupUnreadCount(holder, contact)
        setupTime(holder, contact)
        setupSenderName(holder, contact)
        setupMessageText(holder, contact)
        setupMessageStatus(holder, contact)
    }

    private fun setupAccountColorIndicator(holder: ChatViewHolder, contact: AbstractContact) {
        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            val color: Int = ColorManager.getInstance().accountPainter
                    .getAccountMainColor(contact.account)
            holder.accountColorIndicatorView.setBackgroundColor(color)
            holder.accountColorIndicatorBackView.setBackgroundColor(color)
        } else { //TODO maybe should make it transparent (not invisible)
            holder.accountColorIndicatorView.visibility = View.INVISIBLE
            holder.accountColorIndicatorBackView.visibility = View.INVISIBLE
        }
    }

    private fun setupContactAvatar(holder: ChatViewHolder, contact: AbstractContact) {
        if (SettingsManager.contactsShowAvatars()) {
            holder.avatarIV.visibility = View.VISIBLE
            holder.statusIV.visibility = View.VISIBLE
            holder.onlyStatusIV.visibility = View.GONE
            holder.avatarIV.setImageDrawable(contact.avatar)
        } else {
            holder.avatarIV.visibility = View.GONE
            holder.statusIV.visibility = View.GONE
            holder.onlyStatusIV.visibility = View.VISIBLE
        }
    }

    private fun setupRosterStatus(holder: ChatViewHolder, contact: AbstractContact) {
        val statusLevel = contact.statusMode.statusLevel
        val mucIndicatorLevel = if (MUCManager.getInstance().hasRoom(contact.account, contact.user)) 1
        else if (MUCManager.getInstance().isMucPrivateChat(contact.account, contact.user)) 3
        else 0

        if ((statusLevel == 6 && contact.user.jid.isDomainBareJid) ||
                (mucIndicatorLevel != 0 && statusLevel != 1))
            holder.statusIV.visibility = View.INVISIBLE
        else if (SettingsManager.contactsShowAvatars()) holder.statusIV.visibility = View.VISIBLE

        holder.statusIV.setImageLevel(statusLevel)
        holder.onlyStatusIV.setImageLevel(statusLevel)
    }

    private fun setupContactName(holder: ChatViewHolder, contact: AbstractContact) {
        holder.contactNameTV.text = contact.name
    }

    private fun setupMUCIndicator(holder: ChatViewHolder, contact: AbstractContact) {
        val resources = holder.itemView.resources
        val mucIndicatorLevel = if (MUCManager.getInstance().hasRoom(contact.account, contact.user)) 1
        else if (MUCManager.getInstance().isMucPrivateChat(contact.account, contact.user)) 3
        else 0
        var mucIndicator: Drawable?
        if (mucIndicatorLevel != 0) {
            mucIndicator = resources.getDrawable(R.drawable.muc_indicator_view)
            mucIndicator.level = mucIndicatorLevel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mucIndicator.setTint(resources.getColor(getThemeResource(holder.itemView.context,
                        R.attr.contact_list_contact_name_text_color)))
        }
    }

    private fun setupGroupchatIndicator(holder: ChatViewHolder, contact: AbstractContact) {
        val isServer = contact.user.jid.isDomainBareJid
        if (holder.statusIV.visibility.equals(View.VISIBLE)) {
            val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
            holder.statusIV.visibility = if (chat.isGroupchat || isServer)
                View.INVISIBLE else View.VISIBLE
            holder.statusGroupchatIV.visibility = if (chat.isGroupchat || isServer)
                View.VISIBLE else View.INVISIBLE
            if (isServer) holder.statusGroupchatIV.setImageResource(R.drawable.ic_server_14_border)
            else holder.statusGroupchatIV.setImageResource(R.drawable.ic_groupchat_14_border)
        } else holder.statusGroupchatIV.visibility = View.GONE
    }

    private fun setupNotificationMuteIcon(holder: ChatViewHolder, contact: AbstractContact) {
        val resources = holder.itemView.context.resources
        val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val isCustomNotification = CustomNotifyPrefsManager.getInstance()
                .isPrefsExist(Key.createKey(contact.account, contact.user))
        var iconId = 0
        val mode = chat.notificationState.determineModeByGlobalSettings(chat is RoomChat)
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

    private fun setupUnreadCount(holder: ChatViewHolder, contact: AbstractContact) {
        val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
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

    private fun setupTime(holder: ChatViewHolder, contact: AbstractContact) {
        val context = holder.itemView.context
        val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val date = Date(chat.lastMessage!!.timestamp)
        holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(context, date)
        holder.timeTV.visibility = View.VISIBLE
    }

    private fun setupSenderName(holder: ChatViewHolder, contact: AbstractContact) { //TODO RABOTAET KAK GOVNO
        val lastMessage = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .lastMessage
        val messageOwner = lastMessage?.resource.toString()
        val color = ColorManager.getInstance().accountPainter.getAccountMainColor(contact.account)
        if (lastMessage!!.isIncoming) {
            holder.outgoingMessageTV.text = ""
            holder.outgoingMessageTV.visibility = View.VISIBLE
            holder.outgoingMessageTV.setTextColor(color)
        }
        if (!messageOwner.trim().isEmpty()) {
            holder.outgoingMessageTV.text = messageOwner + ": "
            holder.outgoingMessageTV.visibility = View.VISIBLE
            holder.outgoingMessageTV.setTextColor(color)
        }
    }

    private fun setupMessageText(holder: ChatViewHolder, contact: AbstractContact) {
        val lastMessage = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
                .lastMessage
        val context = holder.itemView.context
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size
        if (text!!.isEmpty()) {
            if (forwardedCount!! > 0) holder.messageTextTV.text = String
                    .format(context.resources.getString(R.string.forwarded_messages_count), forwardedCount)
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
                                    .fromHtml(URLDecoder.decode(text, StandardCharsets.UTF_8.name()))
                        } catch (e: Exception) {
                            holder.messageTextTV.text = Html.fromHtml(text)
                        }
                    } else holder.messageTextTV.text = text
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                    holder.messageTextTV.text = text
                }
                holder.messageTextTV.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun setupMessageStatus(holder: ChatViewHolder, contact: AbstractContact) {
        val lastMessage = MessageManager.getInstance()
                .getOrCreateChat(contact.account, contact.user).lastMessage
        holder.messageStatusTV.visibility = if (lastMessage?.text!!.isEmpty() || lastMessage.isIncoming)
            View.INVISIBLE else View.VISIBLE
        if (!lastMessage.isIncoming) {
            if (MessageItem.isUploadFileMessage(lastMessage) && !lastMessage.isSent
                    && System.currentTimeMillis() - lastMessage.timestamp > 1000)
                holder.messageStatusTV.setImageResource(R.drawable.ic_message_not_sent_14dp)
            else if (lastMessage.isDisplayed || lastMessage.isReceivedFromMessageArchive)
                holder.messageStatusTV.setImageResource(R.drawable.ic_message_displayed)
            else if (lastMessage.isDelivered)
                holder.messageStatusTV.setImageResource(R.drawable.ic_message_delivered_14dp)
            else if (lastMessage.isError)
                holder.messageStatusTV.setImageResource(R.drawable.ic_message_has_error_14dp)
            else if (lastMessage.isAcknowledged || lastMessage.isForwarded)
                holder.messageStatusTV.setImageResource(R.drawable.ic_message_acknowledged_14dp)
            else holder.messageStatusTV.setImageResource(R.drawable.ic_message_not_sent_14dp)
        }
    }

    private fun getThemeResource(context: Context, themeResourceId: Int): Int {
        val a = context.obtainStyledAttributes(TypedValue().data, intArrayOf(themeResourceId))
        val accountGroupColorsResourceId = a.getResourceId(0, 0)
        a.recycle()
        return accountGroupColorsResourceId
    }

}