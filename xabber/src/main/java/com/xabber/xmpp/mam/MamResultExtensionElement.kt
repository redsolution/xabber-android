package com.xabber.xmpp.mam

import com.xabber.android.data.extension.mam.MessageArchiveManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jivesoftware.smackx.forward.packet.Forwarded

class MamResultExtensionElement(val id: String,
                                val forwarded: Forwarded,
                                val queryId: String? = null,
): ExtensionElement{

    override fun getElementName() = ELEMENT

    override fun getNamespace() = NAMESPACE

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT)
        xmlnsAttribute(NAMESPACE)
        if (queryId != null) optAttribute(QUERY_ID_ATTRIBUTE, queryId)
        optAttribute("id", id)
        rightAngleBracket()
        element(forwarded)
        closeElement(ELEMENT)
    }

    companion object {
        const val ELEMENT = "result"
        private const val NAMESPACE = MessageArchiveManager.NAMESPACE
        const val QUERY_ID_ATTRIBUTE = "queryid"
        const val ID_ATTRIBUTE = "id"
    }

}