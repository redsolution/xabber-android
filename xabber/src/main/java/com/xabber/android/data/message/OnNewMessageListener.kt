package com.xabber.android.data.message

import com.xabber.android.data.BaseUIListener

interface OnNewMessageListener: BaseUIListener {
    fun onNewMessage()
}