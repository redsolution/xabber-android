package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class XTokenRequestIQ(
    server: DomainBareJid,
    private val client: String,
    private val device: String,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        optElement("client", client)
        optElement("device", device)
    }

    companion object {
        const val ELEMENT = "issue"
        const val NAMESPACE = XTokenManager.NAMESPACE
    }

}