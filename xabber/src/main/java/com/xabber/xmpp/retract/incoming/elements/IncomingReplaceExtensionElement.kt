package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingReplaceExtensionElement(
    val messageStanzaId: String,
    val contactJid: ContactJid,
    val message: Message,
    val version: String? = null,
) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = RetractManager.NAMESPACE_NOTIFY

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        xmlnsAttribute(RetractManager.NAMESPACE_NOTIFY)
        attribute(ID_ATTRIBUTE, messageStanzaId)
        attribute(BY_ATTRIBUTE, contactJid.bareJid.toString())
        version?.let { attribute(VERSION_ATTRIBUTE, it) }
        rightAngleBracket()
        append(message.toXML())
        closeElement(ELEMENT_NAME)
    }

    companion object {
        const val ELEMENT_NAME = "replace"
        const val ID_ATTRIBUTE = "id"
        const val BY_ATTRIBUTE = "by"
        const val VERSION_ATTRIBUTE = "version"

        fun Message.hasIncomingReplaceExtensionElement() =
            this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

        fun Message.getIncomingReplaceExtensionElement(): IncomingReplaceExtensionElement? =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)
    }

}