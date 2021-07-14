package com.xabber.xmpp.retract.incoming.providers

import com.xabber.xmpp.mam.MamResultExtensionElement
import com.xabber.xmpp.retract.incoming.elements.ReplacedExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class ReplacedExtensionElementProvider : ExtensionElementProvider<ReplacedExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): ReplacedExtensionElement {

        lateinit var timestamp: String

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == ReplacedExtensionElement.ELEMENT_NAME) {
                        timestamp = parser.getAttributeValue("", ReplacedExtensionElement.STAMP_ATTRIBUTE)
                    }
                }
                XmlPullParser.END_TAG -> if (name == MamResultExtensionElement.ELEMENT) break@outerloop
            }
        }

        return ReplacedExtensionElement(timestamp)
    }

}