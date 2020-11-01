package com.xabber.android.data.roster

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.view.View
import android.widget.ImageView
import com.xabber.android.R
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.utils.Utils

object StatusBadgeSetupHelper {

//    fun getStatusLevelForAccount(accountJid: AccountJid): Int {
//
//    }
//
//    fun getStatusLevelForAccount(account: AccountItem) = getStatusLevelForAccount(account.account)




    fun getStatusLevelForContact(abstractContact: AbstractContact, imageView: ImageView,
                                 abstractChat: AbstractChat? = ChatManager.getInstance()
                                         .getChat(abstractContact.account, abstractContact.contactJid)){

        val accountJid = abstractContact.account
        val contactJid = abstractContact.contactJid
        val rosterContact = RosterManager.getInstance().getRosterContact(accountJid, contactJid)

        var statusLevel = abstractContact.statusMode.statusLevel
        val isServer = abstractContact.contactJid.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance()
                .contactIsBlockedLocally(accountJid, contactJid)
        val isUnavailable = statusLevel == StatusMode.unavailable.ordinal
        val isAccountConnected = AccountManager.getInstance().connectedAccounts
                .contains(accountJid)
        val isRosterContact = (rosterContact != null && !rosterContact.isDirtyRemoved)
                || !VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid)
        val isPublicGroupChat = abstractChat is GroupChat
                && (abstractChat.privacyType == GroupchatPrivacyType.PUBLIC
                    || abstractChat.privacyType == GroupchatPrivacyType.NONE)
        val isIncognitoGroupChat = abstractChat is GroupChat
                && abstractChat.privacyType == GroupchatPrivacyType.INCOGNITO

        //todo isPrivateChat, isBot, isChannel, isRss, isMail, isMobile etc

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 10
            isPublicGroupChat -> statusLevel += StatusMode.PUBLIC_GROUP_OFFSET
            isIncognitoGroupChat -> statusLevel += StatusMode.INCOGNITO_GROUP_OFFSET
            //todo isPrivateChat, isBot, isChannel, isRss, isMail, isMobile etc
        }

        if (isBlocked || (!isRoster && statusLevel < 8)) {
            if (holder.avatarIV.visibility == View.VISIBLE && isBlocked) {
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
                if (!isServer && !isGroupchat && !isBlocked && isVisible
                        && (isUnavailable || !isAccountConnected))
                    View.INVISIBLE
                else
                    View.VISIBLE

        if ((isServer || isGroupchat) && !isAccountConnected) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorFilter = ColorMatrixColorFilter(colorMatrix)
            holder.statusIV.colorFilter = colorFilter
        } else
            holder.statusIV.setColorFilter(0)


    }

    fun getStatusLevelForChat(abstractChat: AbstractChat, imageView: ImageView) = getStatusLevelForContact(RosterManager.getInstance()
            .getAbstractContact(abstractChat.account, abstractChat.contactJid), imageView, abstractChat)


}