package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.IQ

abstract class AbstractRetractIq(
    archiveAddress: ContactJid? = null, elementName: String
) : IQ(elementName, RetractManager.NAMESPACE) {

    init {
        archiveAddress?.let { to = it.jid }
    }

}