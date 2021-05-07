package com.xabber.android.data.extension.archive

import com.xabber.android.data.BaseManagerInterface
import com.xabber.android.data.account.AccountItem

interface OnHistoryLoaded : BaseManagerInterface {
    fun onHistoryLoaded(accountItem: AccountItem)
}