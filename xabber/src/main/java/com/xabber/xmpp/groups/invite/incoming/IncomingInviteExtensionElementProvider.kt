package com.xabber.xmpp.groups.invite.incoming

import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingInviteExtensionElementProvider : ExtensionElementProvider<IncomingInviteExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingInviteExtensionElement {
        val inviteElement = IncomingInviteExtensionElement()

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        IncomingInviteExtensionElement.ELEMENT -> {
                            inviteElement.groupJid =
                                parser.getAttributeValue("", IncomingInviteExtensionElement.JID_ATTRIBUTE) ?: ""
                        }
                        IncomingInviteExtensionElement.ReasonElement.ELEMENT_NAME -> {
                            inviteElement.setReason(parser.text)
                        }
                        IncomingInviteExtensionElement.UserElement.ELEMENT -> {
                            val userJid =
                                parser.getAttributeValue("", IncomingInviteExtensionElement.UserElement.JID_ATTRIBUTE)
                                    ?: ""
                            val userId =
                                parser.getAttributeValue("", IncomingInviteExtensionElement.UserElement.ID_ATTRIBUTE)
                                    ?: ""
                            inviteElement.setUser(userJid, userId)
                        }
                    }
                    parser.next()
                }
                XmlPullParser.END_TAG -> if (IncomingInviteExtensionElement.ELEMENT == parser.name) {
                    break@outerloop
                } else parser.next()
                else -> parser.next()
            }
        }

        return inviteElement
    }

}