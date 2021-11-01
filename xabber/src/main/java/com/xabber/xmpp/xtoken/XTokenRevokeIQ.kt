package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.util.XmlStringBuilder
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
        ids.forEach {
            element(TokenElement(it))
        }
    }

    companion object {
        const val ELEMENT = "revoke"
        const val NAMESPACE = XTokenManager.NAMESPACE
    }

    private class TokenElement(private val tokenUid: String): ExtensionElement {
        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT_NAME)
            attribute(UID_ATTRIBUTE, tokenUid)
            closeEmptyElement()
        }

        override fun getNamespace(): String = NAMESPACE
        override fun getElementName() = ELEMENT_NAME

        companion object {
            const val ELEMENT_NAME = "token"
            const val UID_ATTRIBUTE = "uid"
        }
    }

}