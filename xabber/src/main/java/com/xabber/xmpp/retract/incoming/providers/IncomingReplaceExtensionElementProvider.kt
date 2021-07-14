package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.mam.MamResultExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.util.PacketParserUtils
import org.xmlpull.v1.XmlPullParser

class IncomingReplaceExtensionElementProvider : ExtensionElementProvider<IncomingReplaceExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingReplaceExtensionElement {

        lateinit var messageStanzaId: String
        lateinit var contactJid: ContactJid
        lateinit var message: Message
        var version: String? = null

        outerloop@ while (true) {
            val eventType = parser.next()
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == IncomingReplaceExtensionElement.ELEMENT_NAME) {
                        messageStanzaId = parser.getAttributeValue("", IncomingReplaceExtensionElement.ID_ATTRIBUTE)
                        parser.getAttributeValue("", IncomingReplaceExtensionElement.BY_ATTRIBUTE)
                            ?.let { contactJid = ContactJid.from(it) }
                        version = parser.getAttributeValue("", IncomingReplaceExtensionElement.VERSION_ATTRIBUTE)
                    }
                    if (name == Message.ELEMENT) {
                        message = PacketParserUtils.parseMessage(parser)
                    }
                }
                XmlPullParser.END_TAG -> if (name == MamResultExtensionElement.ELEMENT) break@outerloop
            }
        }

        return IncomingReplaceExtensionElement(messageStanzaId, contactJid, message, version)
    }
}