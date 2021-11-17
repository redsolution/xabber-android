package com.xabber.xmpp.devices.providers

import com.xabber.xmpp.devices.IncomingNewDeviceIQ
import org.jivesoftware.smack.provider.IQProvider
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit

class DeviceProvider : IQProvider<IncomingNewDeviceIQ?>() {

    @Throws(Exception::class)
    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingNewDeviceIQ? {
        var secret: String? = null
        val id: String = parser.getAttributeValue(null, IncomingNewDeviceIQ.ATTRIBUTE_UID)
        var expire: Long = 0

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (IncomingNewDeviceIQ.ELEMENT == parser.name && IncomingNewDeviceIQ.NAMESPACE == parser.namespace) {
                        parser.next()
                    } else if (IncomingNewDeviceIQ.ELEMENT_SECRET == parser.name) {
                        secret = parser.nextText()
                    } else if (IncomingNewDeviceIQ.ELEMENT_EXPIRE == parser.name) {
                        expire = java.lang.Long.valueOf(parser.nextText())
                    } else parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (IncomingNewDeviceIQ.ELEMENT == parser.name) {
                        break@outerloop
                    } else parser.next()
                }

                else -> parser.next()
            }
        }
        return if (secret != null && id != null) IncomingNewDeviceIQ(
            secret,
            id,
            TimeUnit.SECONDS.toMillis(expire)
        ) else null
    }
}