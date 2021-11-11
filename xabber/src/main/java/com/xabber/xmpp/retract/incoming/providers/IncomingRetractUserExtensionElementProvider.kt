package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractUserExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.util.ParserUtils
import org.xmlpull.v1.XmlPullParser

class IncomingRetractUserExtensionElementProvider
    : ExtensionElementProvider<IncomingRetractUserExtensionElement>() {

    override fun parse(
        parser: XmlPullParser, initialDepth: Int
    ): IncomingRetractUserExtensionElement {

        var id: String? = null
        var symmetric: Boolean? = null
        var by: ContactJid? = null

        outerloop@ while (true) {
            val name = parser.name
            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    if (name == IncomingRetractUserExtensionElement.ELEMENT_NAME) {
                        try {
                            by = ContactJid.from(
                                parser.getAttributeValue(
                                    "", IncomingRetractUserExtensionElement.BY_ATTRIBUTE
                                )
                            )
                        } catch (e: Exception) {
                            throw NullPointerException("Got incoming retract user message, but can't parse \"by\" attribute")
                        }

                        id = parser.getAttributeValue(
                            "", IncomingRetractUserExtensionElement.ID_ATTRIBUTE
                        ) ?: throw NullPointerException("Got incoming retract user message, but can't parse \"id\" attribute")

                        symmetric = ParserUtils.getBooleanAttribute(
                            parser, IncomingRetractUserExtensionElement.ID_ATTRIBUTE
                        ) ?: throw NullPointerException("Got incoming retract user message, but can't parse \"id\" attribute")
                    }
                    parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (name == IncomingRetractUserExtensionElement.ELEMENT_NAME) {
                        break@outerloop
                    }
                }

                else -> parser.next()
            }
        }

        return IncomingRetractUserExtensionElement(id!!, by!!, symmetric!!)
    }

}