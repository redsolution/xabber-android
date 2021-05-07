package com.xabber.android.data.connection.listeners

import com.xabber.android.data.BaseManagerInterface
import com.xabber.android.data.connection.ConnectionItem

interface OnAuthenticatedListener : BaseManagerInterface {
    fun onAuthenticated(connectionItem: ConnectionItem)
}