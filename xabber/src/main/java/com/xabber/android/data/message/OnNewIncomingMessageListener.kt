package com.xabber.android.data.message

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

interface OnNewIncomingMessageListener: BaseUIListener {
    fun onNewIncomingMessage(accountJid: AccountJid, contactJid: ContactJid)
}