package com.xabber.android.data.extension.archive

import com.xabber.android.data.account.AccountItem
import com.xabber.android.ui.BaseUIListener

interface OnHistoryLoaded : BaseUIListener {
    fun onHistoryLoaded(accountItem: AccountItem)
}