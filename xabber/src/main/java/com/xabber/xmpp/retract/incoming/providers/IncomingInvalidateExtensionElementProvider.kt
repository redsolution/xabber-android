package com.xabber.xmpp.retract.incoming.providers

import com.xabber.xmpp.retract.incoming.elements.IncomingInvalidateExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingInvalidateExtensionElementProvider : ExtensionElementProvider<IncomingInvalidateExtensionElement>() {
    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingInvalidateExtensionElement {
        outerloop@ while (true) {
            val name = parser.name
            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    if (name == IncomingInvalidateExtensionElement.ELEMENT_NAME) {
                        parser.getAttributeValue("", IncomingInvalidateExtensionElement.VERSION_ATTRIBUTE)
                            ?.let { return IncomingInvalidateExtensionElement(it) }
                    }
                    parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (name == IncomingInvalidateExtensionElement.ELEMENT_NAME) {
                        break@outerloop
                    }
                }

                else -> parser.next()
            }
        }
        throw IllegalStateException("Can't parse incoming invalidate extension element!")
    }
}