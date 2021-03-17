package com.xabber.android.data.extension.ccc

import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.xmpp.ccc.IncomingSetSyncIQ
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Stanza

object CccSyncManager : OnPacketListener  {

    const val NAMESPACE = "https://xabber.com/protocol/synchronization"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingSetSyncIQ){
            //todo this
        }
    }

}