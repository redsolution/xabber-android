package com.xabber.android.data.extension.otr

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

interface OnAuthAskListener: BaseUIListener {
    fun onAuthAsk(accountJid: AccountJid, contactJid: ContactJid)
}