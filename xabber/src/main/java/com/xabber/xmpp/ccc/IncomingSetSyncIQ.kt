package com.xabber.xmpp.ccc

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.ccc.CccSyncManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingSetSyncIQ(val stamp: Long,
                        val extensionElement: ExtensionElement,
) : IQ(QUERY_ELEMENT, CccSyncManager.NAMESPACE) {

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml?.apply {
        attribute(STAMP_ATTRIBUTE, stamp.toString())
        rightAngleBracket()
        append(extensionElement.toXML())
    }

    companion object {
        const val QUERY_ELEMENT = "query"
        const val STAMP_ATTRIBUTE = "stamp"
    }

}

class ConversationExtensionElement(val jid: ContactJid, val childElement: NamedElement) : ExtensionElement {

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = ELEMENT_NAME

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        attribute(JID_ATTRIBUTE, jid.toString())
        rightAngleBracket()
        append(childElement.toXML())
        closeElement(ELEMENT_NAME)
    }

    companion object {
        const val NAMESPACE = ""
        const val JID_ATTRIBUTE = "jid"
        const val ELEMENT_NAME = "conversation"
    }

}

class DeletedElement: NamedElement {

    override fun toXML() = XmlStringBuilder().apply {
        emptyElement(elementName)
    }

    override fun getElementName() = ELEMENT_NAME

    companion object {
        const val ELEMENT_NAME = "deleted"
    }

}