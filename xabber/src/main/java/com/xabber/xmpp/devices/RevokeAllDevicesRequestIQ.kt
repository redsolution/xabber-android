package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class RevokeAllDevicesRequestIQ(server: DomainBareJid) : IQ(ELEMENT, DevicesManager.NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml?.apply {
        rightAngleBracket()
    }

    private companion object {
        const val ELEMENT = "revoke-all"
    }
}