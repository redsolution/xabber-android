package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.extension.muc.MUCManager
import com.xabber.android.data.extension.muc.RoomChat
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager
import com.xabber.android.data.notification.custom_notification.Key
import com.xabber.android.data.roster.AbstractContact
import com.xabber.android.ui.color.ColorManager

class ChatListAdapter(val list: MutableList<AbstractContact>, val listener: ChatClickListener) :
        RecyclerView.Adapter<ChatViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
            ChatViewHolder(LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_chat_in_contact_list, parent))

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val contact = list[position]
        setupAccountColorIndicator(holder, contact)
        setupContactAvatar(holder, contact)
        setupRosterStatus(holder, contact)
        setupContactName(holder,contact)
        setupMUCIndicator(holder, contact)
        setupGroupchatIndicator(holder, contact)
        setupNotificationMuteIcon(holder, contact)
        setupUnreadCount(holder, contact)
    }

    private fun setupAccountColorIndicator(holder: ChatViewHolder, contact: AbstractContact){
        if (AccountManager.getInstance().getEnabledAccounts().size > 1){
            val color : Int = ColorManager.getInstance().getAccountPainter()
                    .getAccountMainColor(contact.account)
            holder.accountColorIndicatorView.setBackgroundColor(color)
            holder.accountColorIndicatorBackView.setBackgroundColor(color)
        } else{ //TODO maybe should make it transparent (not invisible)
            holder.accountColorIndicatorView.visibility = View.INVISIBLE
            holder.accountColorIndicatorBackView.visibility = View.INVISIBLE
        }
    }

    private fun setupContactAvatar(holder: ChatViewHolder, contact: AbstractContact){
        if (SettingsManager.contactsShowAvatars()){
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

    private fun setupRosterStatus(holder: ChatViewHolder, contact: AbstractContact){
        val statusLevel = contact.statusMode.statusLevel
        val mucIndicatorLevel = if (MUCManager.getInstance().hasRoom(contact.account, contact.user)) 1
        else if (MUCManager.getInstance().isMucPrivateChat(contact.account, contact.user)) 3
        else 0

        if ((statusLevel == 6 && contact.user.jid.isDomainBareJid()) ||
                (mucIndicatorLevel != 0 && statusLevel != 1))
            holder.statusIV.visibility = View.INVISIBLE
        else if (SettingsManager.contactsShowAvatars()) holder.statusIV.visibility = View.VISIBLE

        holder.statusIV.setImageLevel(statusLevel)
        holder.onlyStatusIV.setImageLevel(statusLevel)
    }

    private fun setupContactName(holder: ChatViewHolder, contact: AbstractContact){
        holder.contactNameTV.text = contact.name
    }

    private fun setupMUCIndicator(holder: ChatViewHolder, contact: AbstractContact){
        val resources = holder.itemView.resources
        val mucIndicatorLevel = if (MUCManager.getInstance().hasRoom(contact.account, contact.user)) 1
        else if (MUCManager.getInstance().isMucPrivateChat(contact.account, contact.user)) 3
        else 0
        var mucIndicator : Drawable?
        if (mucIndicatorLevel != 0){
            mucIndicator = resources.getDrawable(R.drawable.muc_indicator_view)
            mucIndicator.level = mucIndicatorLevel
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//                mucIndicator.setTint(context.getResources().getColor(getThemeResource(context,
//                        R.attr.contact_list_contact_name_text_color)))
        }

    }

    private fun setupGroupchatIndicator(holder: ChatViewHolder, contact: AbstractContact){
        val isServer = contact.user.jid.isDomainBareJid()
        if (holder.statusIV.visibility.equals(View.VISIBLE)){
            val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
            holder.statusIV.setVisibility(if (chat.isGroupchat || isServer)
                View.INVISIBLE else View.VISIBLE)
            holder.statusGroupchatIV.setVisibility(if (chat.isGroupchat || isServer)
                View.VISIBLE else View.INVISIBLE)
            if (isServer) holder.statusGroupchatIV.setImageResource(R.drawable.ic_server_14_border)
            else holder.statusGroupchatIV.setImageResource(R.drawable.ic_groupchat_14_border)
        } else holder.statusGroupchatIV.setVisibility(View.GONE)
    }

    fun setupNotificationMuteIcon(holder: ChatViewHolder, contact: AbstractContact){
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

    fun setupUnreadCount(holder: ChatViewHolder, contact: AbstractContact){
        val chat = MessageManager.getInstance().getOrCreateChat(contact.account, contact.user)
        val unreadCount = chat.unreadMessageCount
        val resources = holder.itemView.resources
        if (unreadCount > 0){
            holder.unreadCountTV.text = unreadCount.toString()
            holder.unreadCountTV.visibility = View.VISIBLE
        } else holder.unreadCountTV.visibility = View.GONE

        if (!chat.notifyAboutMessage()){
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                holder.unreadCountTV.getBackground().mutate()
                        .setColorFilter(resources.getColor(R.color.grey_500), PorterDuff.Mode.SRC_IN)
                holder.unreadCountTV.setTextColor(resources.getColor(R.color.grey_100))
            } else {
                holder.unreadCountTV.getBackground().mutate()
                        .setColorFilter(resources.getColor(R.color.grey_700), PorterDuff.Mode.SRC_IN)
                holder.unreadCountTV.setTextColor(resources.getColor(R.color.black))
            }
        } else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            holder.unreadCountTV.getBackground().mutate().clearColorFilter()

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
            holder.unreadCountTV.setTextColor(resources.getColor(R.color.grey_200))
    }

    interface ChatClickListener {
        fun onChatContactAvatarClick(position: Int)
        fun onChatCreateContextMenu(positin: Int, menu: ContextMenu)
        fun onChatClick(position: Int)
    }

}