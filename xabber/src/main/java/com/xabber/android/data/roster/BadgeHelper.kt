package com.xabber.android.data.roster

import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.message.chat.AbstractChat

object BadgeHelper {

    fun getStatusLevel(accountJid: AccountJid): Int {

    }

    fun getStatusLevel(account: AccountItem) = getStatusLevel(account.account)




    fun getStatusLevel(abstractContact: AbstractContact): Int {

    }

    fun getStatusLevel(abstractChat: AbstractChat) = getStatusLevel(RosterManager.getInstance()
            .getAbstractContact(abstractChat.account, abstractChat.contactJid))


}