package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class ReplacedExtensionElement(val timestamp: String) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = RetractManager.NAMESPACE

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        xmlnsAttribute(namespace)
        attribute(STAMP_ATTRIBUTE, timestamp)
        closeEmptyElement()
    }

    companion object {
        const val ELEMENT_NAME = "replaced"
        const val STAMP_ATTRIBUTE = "stamp"

        fun Message.hasReplacedElement() = this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE)

        fun Message.getReplacedElement(): ReplacedExtensionElement =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE)
    }

}