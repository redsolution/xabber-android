package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.xmpp.ProviderUtils
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class IncomingInviteExtensionElementProvider: ExtensionElementProvider<IncomingInviteExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingInviteExtensionElement {
        val inviteElement = IncomingInviteExtensionElement()

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (IncomingInviteExtensionElement.ELEMENT == parser.name) {
                        inviteElement.groupJid = parser.getAttributeValue("", IncomingInviteExtensionElement.JID_ATTRIBUTE)
                    } else if (IncomingInviteExtensionElement.ReasonElement.ELEMENT_NAME == parser.name){
                        inviteElement.setReason(ProviderUtils.parseText(parser))
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