package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.DomainBareJid

class ChangeXTokenDescriptionIQ(
    serverAddress: DomainBareJid,
    private val tokenUid: String,
    private val description: String
): IQ(ELEMENT_NAME, XTokenManager.NAMESPACE) {

    init {
        type = Type.set
        to = serverAddress
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml?.apply {
        rightAngleBracket()
        optElement(
            XTokenChangeDescriptionElement(tokenUid, description)
        )
    }

    private companion object {
        const val ELEMENT_NAME = "query"
    }

    private class XTokenChangeDescriptionElement(
        private val uid: String,
        private val description: String
    ): ExtensionElement {

        override fun getElementName() = ELEMENT_NAME

        override fun getNamespace() = XTokenManager.NAMESPACE

        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT_NAME)
            attribute(ELEMENT_UID_ATTRIBUTE, uid)
            rightAngleBracket()
            append(
                SimpleNamedElement(ELEMENT_DESCRIPTION, description).toXML()
            )
            closeElement(ELEMENT_NAME)
        }

        private companion object {
            const val ELEMENT_NAME = "xtoken"
            const val ELEMENT_UID_ATTRIBUTE = "uid"
            const val ELEMENT_DESCRIPTION = "description"
        }
    }

}