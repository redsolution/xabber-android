package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingRetractExtensionElement(
    val messageId: String,
    val contactJid: ContactJid,
    val version: String? = null,
) : ExtensionElement {

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        xmlnsAttribute(namespace)
        version?.let { attribute(VERSION_ATTRIBUTE, it) }
        attribute(BY_ATTRIBUTE, contactJid.bareJid.toString())
        attribute(ID_ATTRIBUTE, messageId)
        closeEmptyElement()
    }

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = RetractManager.NAMESPACE_NOTIFY

    companion object {
        const val ELEMENT_NAME = "retract-message"
        const val ID_ATTRIBUTE = "id"
        const val BY_ATTRIBUTE = "by"
        const val VERSION_ATTRIBUTE = "version"

        fun Message.hasIncomingRetractExtensionElement() =
            this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

        fun Message.getIncomingRetractExtensionElement(): IncomingRetractExtensionElement? =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)
    }

}
