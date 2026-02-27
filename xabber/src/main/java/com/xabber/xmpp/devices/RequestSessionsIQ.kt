package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class RequestSessionsIQ(server: DomainBareJid) : IQ(ELEMENT, NAMESPACE) {

    init {
        to = server
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

    companion object {
        const val ELEMENT = "query"
        private const val HASH_BLOCK = "#items"
        const val NAMESPACE = DevicesManager.NAMESPACE + HASH_BLOCK
    }

}