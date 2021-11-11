package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.XmlStringBuilder

class XTokenRevokeExtensionElement(val uids: List<String>) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = XTokenManager.NAMESPACE

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(elementName)
        xmlnsAttribute(namespace)
        rightAngleBracket()
        append(uids.map { SimpleNamedElement(UID_ELEMENT_NAME, it) })
        closeElement(elementName)
    }

    companion object {
        const val ELEMENT_NAME = "revoke"
        const val UID_ELEMENT_NAME = "token-uid"

        fun Stanza.hasXTokenRevokeExtensionElement() =
            this.hasExtension(ELEMENT_NAME, XTokenManager.NAMESPACE)

        fun Stanza.getXTokenRevokeExtensionElement(): XTokenRevokeExtensionElement =
            this.getExtension(ELEMENT_NAME, XTokenManager.NAMESPACE)
    }

}