package com.xabber.xmpp.xtoken.providers

import com.xabber.android.data.extension.xtoken.XTokenManager
import com.xabber.xmpp.xtoken.XTokenRevokeExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class XTokenRevokeExtensionElementProvider :
    ExtensionElementProvider<XTokenRevokeExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): XTokenRevokeExtensionElement {
        val tokenUidsList = mutableListOf<String>()

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (XTokenRevokeExtensionElement.ELEMENT_NAME == parser.name
                        && XTokenManager.NAMESPACE == parser.namespace
                    ) {
                        parser.next()
                    } else if (XTokenRevokeExtensionElement.UID_ELEMENT_NAME == parser.name) {
                        tokenUidsList.add(parser.nextText())
                    } else {
                        parser.next()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (XTokenRevokeExtensionElement.ELEMENT_NAME == parser.name) {
                        break@outerloop
                    } else {
                        parser.next()
                    }
                }
                else -> parser.next()
            }
        }

        return XTokenRevokeExtensionElement(tokenUidsList)
    }

}