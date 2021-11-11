package com.xabber.xmpp.xtoken.providers

import com.xabber.xmpp.xtoken.IncomingNewXTokenIQ
import org.jivesoftware.smack.provider.IQProvider
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit

class XTokenProvider : IQProvider<IncomingNewXTokenIQ?>() {

    @Throws(Exception::class)
    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingNewXTokenIQ? {
        var token: String? = null
        val tokenUID: String = parser.getAttributeValue(null, IncomingNewXTokenIQ.ATTRIBUTE_UID)
        var expire: Long = 0

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (IncomingNewXTokenIQ.ELEMENT == parser.name && IncomingNewXTokenIQ.NAMESPACE == parser.namespace) {
                        parser.next()
                    } else if (IncomingNewXTokenIQ.ELEMENT_TOKEN == parser.name) {
                        token = parser.nextText()
                    } else if (IncomingNewXTokenIQ.ELEMENT_EXPIRE == parser.name) {
                        expire = java.lang.Long.valueOf(parser.nextText())
                    } else parser.next()
                }

                XmlPullParser.END_TAG -> {
                    if (IncomingNewXTokenIQ.ELEMENT == parser.name) {
                        break@outerloop
                    } else parser.next()
                }

                else -> parser.next()
            }
        }
        return if (token != null && tokenUID != null) IncomingNewXTokenIQ(
            token,
            tokenUID,
            TimeUnit.SECONDS.toMillis(expire)
        ) else null
    }
}