package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
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
        var by: String? = null

        outerloop@ while (true) {
            val name = parser.name
            val eventType = parser.eventType
            when (eventType) {

                XmlPullParser.START_TAG -> {
                    if (name == IncomingReplaceExtensionElement.ELEMENT_NAME) {
                        messageStanzaId = parser.getAttributeValue("", IncomingReplaceExtensionElement.ID_ATTRIBUTE)
                        parser.getAttributeValue("", IncomingReplaceExtensionElement.CONVERSATION_ATTRIBUTE)
                            ?.let { contactJid = ContactJid.from(it) }
                        parser.getAttributeValue("", IncomingReplaceExtensionElement.BY_ATTRIBUTE)
                            ?.let { by = it }
                        version = parser.getAttributeValue("", IncomingReplaceExtensionElement.VERSION_ATTRIBUTE)
                    }
                    if (name == Message.ELEMENT) {
                        message = PacketParserUtils.parseMessage(parser)
                    }
                    parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (name == IncomingReplaceExtensionElement.ELEMENT_NAME) {
                        break@outerloop
                    }
                }

                else -> parser.next()
            }
        }

        return IncomingReplaceExtensionElement(messageStanzaId, contactJid, message, version, by)
    }

}