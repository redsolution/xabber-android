package com.xabber.android.data.extension.ccc

import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.xmpp.ccc.ConversationExtensionElement
import com.xabber.xmpp.ccc.DeletedElement
import com.xabber.xmpp.ccc.IncomingSetSyncIQ
import org.jivesoftware.smack.packet.Stanza

object CccSyncManager : OnPacketListener  {

    const val NAMESPACE = "https://xabber.com/protocol/synchronization"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingSetSyncIQ
                && packet.extensionElement is ConversationExtensionElement
                && packet.extensionElement.childElement is DeletedElement) {
            GroupInviteManager.onConversationDeleted(AccountJid.from(packet.to.toString()), packet.extensionElement.jid)
        }
    }

}