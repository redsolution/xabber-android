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
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.extension.groups.GroupPrivacyType
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat

object StatusBadgeSetupHelper {

    fun setupImageViewForContact(
        abstractContact: AbstractContact, imageView: ImageView,
        abstractChat: AbstractChat =
            ChatManager.getInstance().getChat(abstractContact.account, abstractContact.contactJid)
                ?: ChatManager.getInstance().createRegularChat(abstractContact.account, abstractContact.contactJid)
    ) {
        imageView.setImageLevel(getStatusImageLevel(abstractChat))
        imageView.visibility = if (isStatusVisibile(abstractChat)) View.VISIBLE else View.INVISIBLE
        if (isStatusBadgeFiltered(abstractChat)) {
            imageView.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else imageView.setColorFilter(0)
    }

    fun setupImageView(imageView: ImageView, imageLevel: Int, isVisible: Boolean, isFiltered: Boolean) {
        imageView.setImageLevel(imageLevel)
        imageView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        if (isFiltered) {
            imageView.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else imageView.setColorFilter(0)
    }

    fun isStatusBadgeFiltered(abstractChat: AbstractChat): Boolean {
        val accountJid = abstractChat.account

        val isServer = abstractChat.contactJid.jid.isDomainBareJid
        val isAccountConnected = AccountManager.connectedAccounts.contains(accountJid)
        val isPublicGroupChat = abstractChat is GroupChat
                && (abstractChat.privacyType == GroupPrivacyType.PUBLIC
                || abstractChat.privacyType == GroupPrivacyType.NONE)
        val isIncognitoGroupChat = abstractChat is GroupChat
                && abstractChat.privacyType == GroupPrivacyType.INCOGNITO

        return (isServer || isPublicGroupChat || isIncognitoGroupChat) && !isAccountConnected
    }

    fun isStatusVisibile(abstractChat: AbstractChat): Boolean {
        val accountJid = abstractChat.account
        val contactJid = abstractChat.contactJid
        val isServer = abstractChat.contactJid.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance().contactIsBlockedLocally(accountJid, contactJid)
        val isUnavailable =
            RosterManager.getInstance().getAbstractContact(accountJid, contactJid).statusMode.statusLevel ==
                    StatusMode.unavailable.ordinal
        val isAccountConnected = AccountManager.connectedAccounts.contains(accountJid)
        val isPublicGroupChat = abstractChat is GroupChat
                && (abstractChat.privacyType == GroupPrivacyType.PUBLIC
                || abstractChat.privacyType == GroupPrivacyType.NONE)
        val isIncognitoGroupChat = abstractChat is GroupChat
                && abstractChat.privacyType == GroupPrivacyType.INCOGNITO
        val isSavedMessages = accountJid.bareJid.toString() == contactJid.bareJid.toString()

        val hasActiveIncomingInvite = GroupInviteManager.hasActiveIncomingInvites(accountJid, contactJid)

        return !(!isServer
                && !isPublicGroupChat
                && !isIncognitoGroupChat
                && !hasActiveIncomingInvite
                && !isBlocked
                && (isUnavailable || !isAccountConnected)
                || isSavedMessages)
    }

    fun getStatusImageLevel(abstractChat: AbstractChat): Int {
        val accountJid = abstractChat.account
        val contactJid = abstractChat.contactJid

        var statusLevel = RosterManager.getInstance().getAbstractContact(accountJid, contactJid).statusMode.statusLevel
        val isServer = abstractChat.contactJid.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance().contactIsBlockedLocally(accountJid, contactJid)
        val isPublicGroupChat =
            abstractChat is GroupChat
                    && (abstractChat.privacyType == GroupPrivacyType.PUBLIC
                    || abstractChat.privacyType == GroupPrivacyType.NONE)
        val isIncognitoGroupChat = abstractChat is GroupChat && abstractChat.privacyType == GroupPrivacyType.INCOGNITO

        val hasActiveIncomingInvite = GroupInviteManager.hasActiveIncomingInvites(accountJid, contactJid)

        if (hasActiveIncomingInvite) statusLevel = StatusMode.available.statusLevel

        if (statusLevel == StatusMode.unavailable.statusLevel || statusLevel == StatusMode.connection.statusLevel)
            statusLevel = 5

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 90
            isPublicGroupChat -> statusLevel += StatusMode.PUBLIC_GROUP_OFFSET
            isIncognitoGroupChat -> statusLevel += StatusMode.INCOGNITO_GROUP_OFFSET
        }

        return statusLevel
    }

    fun setupImageViewForChat(abstractChat: AbstractChat, imageView: ImageView) =
        setupImageViewForContact(
            RosterManager.getInstance().getAbstractContact(abstractChat.account, abstractChat.contactJid),
            imageView,
            abstractChat
        )

    fun setupImageView(
        statusMode: StatusMode = StatusMode.unavailable, offset: Int = 0,
        imageView: ImageView
    ) {
        imageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                imageView.context.resources,
                R.drawable.ic_status_combined, null
            )
        )
        imageView.setImageLevel(statusMode.statusLevel + offset)
    }

    fun setupDefaultGroupBadge(imageView: ImageView) {
        imageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                imageView.context.resources,
                R.drawable.ic_status_combined, null
            )
        )
        imageView.setImageLevel(25)
    }

}