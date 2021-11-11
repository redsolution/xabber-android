package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class RevokeAllXTokenRequestIQ(server: DomainBareJid) : IQ(ELEMENT, XTokenManager.NAMESPACE) {

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