package com.xabber.android.data.extension.mam

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

interface OnLastHistoryLoadStartedListener: BaseUIListener {
    fun onLastHistoryLoadStarted(accountJid: AccountJid, contactJid: ContactJid)
}