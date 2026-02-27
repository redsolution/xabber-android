package com.xabber.android.data.connection

import com.xabber.android.data.BaseManagerInterface
import org.jivesoftware.smack.packet.Stanza

interface OnAuthenticatedListener : BaseManagerInterface {
    fun onAuthenticated(connectionItem: ConnectionItem)
}

interface OnConnectedListener : BaseManagerInterface {
    /**
     * Connection with server was established.
     */
    fun onConnected(connection: ConnectionItem?)
}

interface OnDisconnectListener : BaseManagerInterface {
    /**
     * Disconnection occur on some reason.
     */
    fun onDisconnect(connection: ConnectionItem?)
}

interface OnPacketListener : BaseManagerInterface {
    /**
     * Process packet from connection.
     */
    fun onStanza(connection: ConnectionItem?, packet: Stanza?)
}