package com.xabber.xmpp.devices.providers

import com.xabber.android.data.extension.devices.DevicesManager
import com.xabber.xmpp.devices.DeviceRevokeExtensionElement
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class RevokeDeviceExtensionElementProvider :
    ExtensionElementProvider<DeviceRevokeExtensionElement>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): DeviceRevokeExtensionElement {
        val devicesIdsList = mutableListOf<String>()

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (DeviceRevokeExtensionElement.ELEMENT_NAME == parser.name
                        && DevicesManager.NAMESPACE == parser.namespace
                    ) {
                        parser.next()
                    } else if (DeviceRevokeExtensionElement.DEVICE_ELEMENT_NAME == parser.name) {
                        devicesIdsList.add(
                            parser.getAttributeValue(
                                null, DeviceRevokeExtensionElement.ID_ATTRIBUTE
                            )
                        )
                    } else {
                        parser.next()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (DeviceRevokeExtensionElement.ELEMENT_NAME == parser.name) {
                        break@outerloop
                    } else {
                        parser.next()
                    }
                }
                else -> parser.next()
            }
        }

        return DeviceRevokeExtensionElement(devicesIdsList)
    }

}