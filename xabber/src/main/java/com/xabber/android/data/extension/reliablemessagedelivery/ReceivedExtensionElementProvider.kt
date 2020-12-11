package com.xabber.android.data.extension.reliablemessagedelivery

import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.OriginIdProvider
import com.xabber.xmpp.sid.StanzaIdElement
import com.xabber.xmpp.sid.StanzaIdProvider
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class ReceivedExtensionElementProvider : ExtensionElementProvider<ReceivedExtensionElement>() {

    override fun parse(parser: XmlPullParser?, initialDepth: Int): ReceivedExtensionElement {
        val receivedElement = ReceivedExtensionElement()

        outerloop@
        while (true) {
            when (parser!!.eventType) {
                XmlPullParser.START_TAG ->
                    when (parser.name) {
                        TimeElement.ELEMENT -> {
                            receivedElement.timeElement = TimeProvider().parse(parser)
                            parser.next()
                        }
                        OriginIdElement.ELEMENT -> {
                            receivedElement.originIdElement = OriginIdProvider().parse(parser)
                            parser.next()
                        }
                        StanzaIdElement.ELEMENT -> {
                            receivedElement.stanzaIdElement = StanzaIdProvider().parse(parser)
                        }
                    else -> parser.next()
                }
                XmlPullParser.END_TAG ->
                    if (ReceivedExtensionElement.ELEMENT == parser.name)
                        break@outerloop
                    else parser.next()
                else -> parser.next()
            }
        }

        return receivedElement
    }

}