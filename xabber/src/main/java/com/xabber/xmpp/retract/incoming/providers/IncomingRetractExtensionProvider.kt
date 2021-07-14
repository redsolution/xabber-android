package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.mam.MamResultExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingRetractExtensionProvider : ExtensionElementProvider<IncomingRetractExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingRetractExtensionElement {

        lateinit var messageStanzaId: String
        lateinit var contactJid: ContactJid
        var version: String? = null

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == IncomingReplaceExtensionElement.ELEMENT_NAME) {
                        parser.getAttributeValue("", IncomingRetractExtensionElement.BY_ATTRIBUTE)
                            ?.let { contactJid = ContactJid.from(it) }
                        version = parser.getAttributeValue("", IncomingRetractExtensionElement.VERSION_ATTRIBUTE)
                        messageStanzaId = parser.getAttributeValue("", IncomingRetractExtensionElement.ID_ATTRIBUTE)
                    }
                }
                XmlPullParser.END_TAG -> if (name == MamResultExtensionElement.ELEMENT) break@outerloop
            }
        }

        return IncomingRetractExtensionElement(messageStanzaId, contactJid, version)
    }

}