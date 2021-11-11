package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingInvalidateExtensionElement(val version: String) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = RetractManager.NAMESPACE_NOTIFY

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        attribute(VERSION_ATTRIBUTE, version)
    }

    companion object {
        const val ELEMENT_NAME = "invalidate"
        const val VERSION_ATTRIBUTE = "version"

        fun Message.hasIncomingInvalidateExtensionElement(): Boolean =
            this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

        fun Message.getIncomingInvalidateExtensionElement(): IncomingInvalidateExtensionElement =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

    }

}