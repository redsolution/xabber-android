package com.xabber.android.data.extension.mam

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

interface OnLastHistoryLoadFinishedListener: BaseUIListener {
    fun onLastHistoryLoadFinished(accountJid: AccountJid, contactJid: ContactJid)
}