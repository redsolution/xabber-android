package com.xabber.android.data.connection.listeners

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.connection.ConnectionState

interface OnConnectionStateChangedListener: BaseUIListener {
    fun onConnectionStateChanged(newConnectionState: ConnectionState)
}