package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.extension.retract.RetractManager
import com.xabber.xmpp.retract.incoming.elements.RetractsResultIq
import org.jivesoftware.smack.provider.IQProvider
import org.xmlpull.v1.XmlPullParser

class RetractResultIqProvider : IQProvider<RetractsResultIq>() {
    override fun parse(parser: XmlPullParser, initialDepth: Int): RetractsResultIq {
        outerloop@ while (true) {
            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    if (parser.namespace == RetractManager.NAMESPACE && parser.name == RetractsResultIq.ELEMENT) {
                        return RetractsResultIq(parser.getAttributeValue("", RetractsResultIq.VERSION_ATTRIBUTE))
                    } else {
                        parser.next()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.namespace == RetractManager.NAMESPACE && parser.name == RetractsResultIq.ELEMENT) {
                        break@outerloop
                    } else {
                        parser.next()
                    }
                }

                else -> {
                    parser.next()
                }
            }
        }
        throw StringIndexOutOfBoundsException("Can't parse RetractResultIqProvider!")
    }
}