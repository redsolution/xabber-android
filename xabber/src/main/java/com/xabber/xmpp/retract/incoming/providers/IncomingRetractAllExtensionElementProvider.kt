package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingRetractAllExtensionElementProvider : ExtensionElementProvider<IncomingRetractAllExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingRetractAllExtensionElement {

        lateinit var contactJid: ContactJid
        var version: String? = null

        outerloop@ while (true) {
            val name = parser.name
            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    if (name == IncomingRetractAllExtensionElement.ELEMENT_NAME) {
                        parser.getAttributeValue("", IncomingRetractAllExtensionElement.CONVERSATION_ATTRIBUTE)
                            ?.let { contactJid = ContactJid.from(it) }
                        version = parser.getAttributeValue("", IncomingRetractAllExtensionElement.VERSION_ATTRIBUTE)
                    }
                    parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (name == IncomingRetractAllExtensionElement.ELEMENT_NAME) {
                        break@outerloop
                    }
                }

                else -> parser.next()
            }
        }

        return IncomingRetractAllExtensionElement(contactJid, version)
    }

}