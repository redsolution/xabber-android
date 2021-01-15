package com.xabber.android.data.roster

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.xabber.android.R
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType

object StatusBadgeSetupHelper {

    fun setupImageViewForContact(abstractContact: AbstractContact, imageView: ImageView,
                                 abstractChat: AbstractChat? = ChatManager.getInstance()
                                         .getChat(abstractContact.account, abstractContact.contactJid)) {

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

        if (statusLevel == StatusMode.unavailable.statusLevel
                || statusLevel == StatusMode.connection.statusLevel)
            statusLevel = 5

        //todo isPrivateChat, isBot, isChannel, isRss, isMail, isMobile etc

        // Hiding badges in disconnected\unavailable state only for regular chats
        imageView.visibility =
                if (!isServer && !isPublicGroupChat && !isIncognitoGroupChat && !isBlocked
                        && (isUnavailable || !isAccountConnected))
                    View.INVISIBLE
                else
                    View.VISIBLE

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 90
            isPublicGroupChat -> statusLevel += StatusMode.PUBLIC_GROUP_OFFSET
            isIncognitoGroupChat -> statusLevel += StatusMode.INCOGNITO_GROUP_OFFSET
            //todo isPrivateChat, isBot, isChannel, isRss, isMail, isMobile etc
        }

        imageView.setImageLevel(statusLevel)

        if ((isServer || isPublicGroupChat || isIncognitoGroupChat) && !isAccountConnected) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorFilter = ColorMatrixColorFilter(colorMatrix)
            imageView.colorFilter = colorFilter
        } else
            imageView.setColorFilter(0)

    }

    fun setupImageViewForChat(abstractChat: AbstractChat, imageView: ImageView) =
            setupImageViewForContact(RosterManager.getInstance()
                    .getAbstractContact(abstractChat.account, abstractChat.contactJid), imageView,
                    abstractChat)

    fun setupImageView(statusMode: StatusMode = StatusMode.unavailable, offset: Int = 0,
                       imageView: ImageView) {
        imageView.setImageDrawable(ResourcesCompat.getDrawable(imageView.context.resources,
                R.drawable.ic_status_combined, null))
        imageView.setImageLevel(statusMode.statusLevel + offset)
    }

}