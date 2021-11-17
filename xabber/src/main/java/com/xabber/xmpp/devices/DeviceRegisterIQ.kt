package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class DeviceRegisterIQ(
    server: DomainBareJid,
    private val client: String,
    private val info: String,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        optElement("client", client)
        optElement("info", info)
    }

    companion object {
        const val ELEMENT = "register"
        const val NAMESPACE = DevicesManager.NAMESPACE
    }

}