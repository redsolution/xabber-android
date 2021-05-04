package com.xabber.android.data.extension.delivery

import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.StanzaIdElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

class ReceivedExtensionElement : ExtensionElement {

    var timeElement: TimeElement? = null
    var originIdElement: OriginIdElement? = null
    var stanzaIdElement: StanzaIdElement? = null

    override fun getNamespace(): String { return NAMESPACE }

    override fun getElementName(): String { return ELEMENT }

    override fun toXML() = XmlStringBuilder(this).apply {
        rightAngleBracket()

        if (timeElement != null)
            append(timeElement?.toXML())

        if (originIdElement != null)
            append(originIdElement?.toXML())

        if (stanzaIdElement != null)
            append(stanzaIdElement?.toXML())
        closeElement(ELEMENT)
    }

    companion object {
        const val NAMESPACE = DeliveryManager.NAMESPACE
        const val ELEMENT = "received"
    }

}