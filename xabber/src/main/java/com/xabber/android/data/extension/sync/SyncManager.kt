package com.xabber.android.data.extension.sync

import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnPacketListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.xmpp.sync.ConversationExtensionElement
import com.xabber.xmpp.sync.DeletedElement
import com.xabber.xmpp.sync.IncomingSetSyncIQ
import org.jivesoftware.smack.packet.Stanza

object SyncManager : OnPacketListener {

    const val NAMESPACE = "https://xabber.com/protocol/synchronization"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingSetSyncIQ
            && packet.extensionElement is ConversationExtensionElement
            && packet.extensionElement.childElement is DeletedElement
        ) {
            GroupInviteManager.onConversationDeleted(
                AccountJid.from(packet.to.toString()),
                packet.extensionElement.jid
            )
        }
    }

}