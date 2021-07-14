package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.mam.MamResultExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingRetractAllExtensionElementProvider : ExtensionElementProvider<IncomingRetractAllExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingRetractAllExtensionElement {

        lateinit var contactJid: ContactJid
        var version: String? = null

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == IncomingRetractAllExtensionElement.ELEMENT_NAME) {
                        parser.getAttributeValue("", IncomingRetractAllExtensionElement.CONVERSATION_ATTRIBUTE)
                            ?.let { contactJid = ContactJid.from(it) }
                        version = parser.getAttributeValue("", IncomingRetractAllExtensionElement.VERSION_ATTRIBUTE)
                    }
                }
                XmlPullParser.END_TAG -> if (name == MamResultExtensionElement.ELEMENT) break@outerloop
            }
        }

        return IncomingRetractAllExtensionElement(contactJid, version)
    }

}