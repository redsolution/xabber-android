package com.xabber.xmpp.mam

import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jivesoftware.smackx.forward.provider.ForwardedProvider
import org.xmlpull.v1.XmlPullParser

class MamResultExtensionElementProvider : ExtensionElementProvider<MamResultExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): MamResultExtensionElement {

        var forwarded: Forwarded? = null
        val queryId = parser.getAttributeValue("", MamResultExtensionElement.QUERY_ID_ATTRIBUTE)
        val id = parser.getAttributeValue("", MamResultExtensionElement.ID_ATTRIBUTE)

        var anotherId: String? = null
        var anotherQueryId: String? = null

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == MamResultExtensionElement.ELEMENT) {
                        anotherQueryId = parser.getAttributeValue("", MamResultExtensionElement.QUERY_ID_ATTRIBUTE)
                        anotherId = parser.getAttributeValue("", MamResultExtensionElement.ID_ATTRIBUTE)
                    }
                    if (name == Forwarded.ELEMENT) forwarded = ForwardedProvider.INSTANCE.parse(parser)
                }
                XmlPullParser.END_TAG -> if (name == MamResultExtensionElement.ELEMENT) break@outerloop
            }
        }

        return MamResultExtensionElement(
                id ?: anotherId ?: throw NullPointerException("Error while parsing id"),
                forwarded ?: throw NullPointerException("Error while parsing forwarded element"),
                queryId ?: anotherQueryId)
    }

}