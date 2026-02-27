package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingRetractUserExtensionElement(
    val id: String, val by: ContactJid, val symmetric: Boolean,
) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME
    override fun getNamespace() = RetractManager.NAMESPACE_NOTIFY
    override fun toXML() = XmlStringBuilder().apply {
        TODO("Not implemented yet!")
    }

    companion object {
        const val ELEMENT_NAME = "retract-user"
        const val SYMMETRIC_ATTRIBUTE = "symmetric"
        const val ID_ATTRIBUTE = "id"
        const val BY_ATTRIBUTE = "by"

        fun Message.hasIncomingRetractUserExtensionElement(): Boolean =
            this.hasExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)

        fun Message.getIncomingRetractUserExtensionElement(): IncomingRetractUserExtensionElement =
            this.getExtension(ELEMENT_NAME, RetractManager.NAMESPACE_NOTIFY)
    }

}