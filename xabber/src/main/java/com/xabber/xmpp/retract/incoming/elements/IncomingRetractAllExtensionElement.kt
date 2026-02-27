package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingRetractAllExtensionElement(
    val contactJid: ContactJid, val version: String? = null,
) : ExtensionElement {

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        xmlnsAttribute(namespace)
        version?.let { attribute(VERSION_ATTRIBUTE, it) }
        attribute(CONVERSATION_ATTRIBUTE, contactJid.bareJid.toString())
        closeEmptyElement()
    }

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = RetractManager.NAMESPACE_NOTIFY

    companion object {
        const val ELEMENT_NAME = "retract-all"
        const val CONVERSATION_ATTRIBUTE = "conversation"
        const val VERSION_ATTRIBUTE = "version"

        fun Message.hasIncomingRetractAllExtensionElement() =
            this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

        fun Message.getIncomingRetractAllExtensionElement(): IncomingRetractAllExtensionElement? =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)
    }

}