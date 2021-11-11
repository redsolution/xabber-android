package com.xabber.xmpp.device

import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class DeviceExtensionElementProvider: ExtensionElementProvider<DeviceExtensionElement>() {
    override fun parse(parser: XmlPullParser, initialDepth: Int): DeviceExtensionElement? {
        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (DeviceExtensionElement.ELEMENT_NAME == parser.name) {
                        return DeviceExtensionElement(
                            parser.getAttributeValue(
                                null,
                                DeviceExtensionElement.UID_ATTRIBUTE
                            )
                        )
                    } else {
                        parser.next()
                    }
                }
                XmlPullParser.END_TAG -> break@outerloop
                else -> parser.next()
            }
        }
        return null
    }
}