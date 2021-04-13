package com.xabber.xmpp.mam

import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jivesoftware.smackx.forward.provider.ForwardedProvider
import org.xmlpull.v1.XmlPullParser

class MamResultExtensionElementProvider : ExtensionElementProvider<MamResultExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): MamResultExtensionElement {
        var queryId: String? = null
        var id: String? = null

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG ->{
                    if (parser.getAttributeValue("", MamResultExtensionElement.ID_ATTRIBUTE) != null){
                        id = parser.getAttributeValue("", MamResultExtensionElement.ID_ATTRIBUTE)
                    }
                    if (parser.getAttributeValue("", MamResultExtensionElement.QUERY_ID_ATTRIBUTE) != null){
                        queryId = parser.getAttributeValue("", MamResultExtensionElement.QUERY_ID_ATTRIBUTE)
                    }
                    when (name) {
                        Forwarded.ELEMENT ->
                            return MamResultExtensionElement(
                                    id ?: throw NullPointerException("Error while parsing id"),
                                    ForwardedProvider.INSTANCE.parse(parser),
                                    queryId)
                    }
                }

                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) break@outerloop
            }
        }

        throw IllegalStateException("Can't parse mam result extension element, infinity loop!")
    }

}