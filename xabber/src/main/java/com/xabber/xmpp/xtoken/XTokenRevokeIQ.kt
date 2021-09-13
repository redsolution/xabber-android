package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.DomainBareJid

class XTokenRevokeIQ(
    server: DomainBareJid,
    private val ids: List<String>,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        for (id in ids) {
            optElement(ELEMENT_TOKEN_UID, id)
        }
    }

    companion object {
        const val ELEMENT = "revoke"
        const val ELEMENT_TOKEN_UID = "token-uid"
        const val NAMESPACE = XTokenManager.NAMESPACE
    }

}