package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.view.View
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.cs.ChatStateManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.groups.GroupInviteManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatAction
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.groups.GroupsManager
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
            GroupInviteManager.hasInvite(chat.account, chat.contactJid) -> {
                holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context,
                        Date(GroupInviteManager.getInvite(chat.account, chat.contactJid)!!.date))
            }
            chat.lastMessage != null -> {
                holder.timeTV.text = StringUtils.getSmartTimeTextForRoster(holder.itemView.context,
                        Date(chat.lastMessage!!.timestamp))
            }
            else -> holder.timeTV.visibility = View.INVISIBLE
        }
    }

    private fun setupMessageText(holder: ChatViewHolder, chat: AbstractChat) {
        val context = holder.itemView.context

        if (chat is GroupChat && chat.lastMessage == null
                && GroupInviteManager.hasInvite(chat.account, chat.contactJid)){
            holder.messageTextTV.text = context.getString(R.string.groupchat_invitation_to_group_chat,
                    chat.privacyType.getLocalizedString().decapitalize())
            holder.messageTextTV.setTypeface(holder.messageTextTV.typeface, Typeface.ITALIC)
            return
        }

        val lastMessage = chat.lastMessage
        val text = lastMessage?.text
        val forwardedCount = lastMessage?.forwardedIds?.size
        if (text.isNullOrEmpty()) {

            if (ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.contactJid) != null) {
                val chatState = ChatStateManager.getInstance()
                        .getFullChatStateString(chat.account, chat.contactJid)
                holder.messageTextTV.text = if (holder.accountColorIndicator != null)
                    Html.fromHtml(StringUtils.getColoredText(chatState, holder.accountColorIndicator!!))
                else chatState
                holder.messageTextTV.typeface = Typeface.DEFAULT
                return
            } else if (forwardedCount != null && forwardedCount > 0) holder.messageTextTV.text = String
                    .format(context.resources.getString(R.string.forwarded_messages_count), forwardedCount)
            else if (lastMessage != null && lastMessage.haveAttachments()) {
                holder.messageTextTV.text = if (holder.accountColorIndicator != null)
                    Html.fromHtml(StringUtils.getColoredAttachmentDisplayName(context,
                            lastMessage.attachmentRealmObjects[0], holder.accountColorIndicator!!))
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
                if (ChatStateManager.getInstance().getFullChatStateString(chat.account, chat.contactJid) != null) {
                    val chatState = ChatStateManager.getInstance()
                            .getFullChatStateString(chat.account, chat.contactJid)
                    holder.messageTextTV.text = if (holder.accountColorIndicator != null)
                        Html.fromHtml(StringUtils.getColoredText(chatState, holder.accountColorIndicator!!))
                    else chatState
                } else if (lastMessage.resource.equals("Groupchat") || (lastMessage.action != null
                                && lastMessage.action.isNotEmpty())) {
                    if (holder.accountColorIndicator != null
                            && (lastMessage.action != null
                                    && lastMessage.action != ChatAction.contact_deleted.toString())) {
                        holder.messageTextTV.text = Html.fromHtml(StringUtils
                                .getColoredText(lastMessage.text, holder.accountColorIndicator!!))
                    } else {
                        holder.messageTextTV.text = lastMessage.text
                    }
                    holder.messageTextTV.alpha = 0.6f
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
        if (chat.lastMessage != null) {
            MessageDeliveryStatusHelper.setupStatusImageView(chat.lastMessage!!, holder.messageStatusTV)
        }
    }

}