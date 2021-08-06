package com.xabber.xmpp.mam

import org.jivesoftware.smack.provider.IQProvider
import org.jivesoftware.smack.util.ParserUtils
import org.jivesoftware.smackx.rsm.packet.RSMSet
import org.jivesoftware.smackx.rsm.provider.RSMSetProvider
import org.xmlpull.v1.XmlPullParser

class MamFinIqProvider : IQProvider<MamFinIQ>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): MamFinIQ {
        val queryId = parser.getAttributeValue("", MamFinIQ.QUERY_ID_ATTRIBUTE)
        val complete = ParserUtils.getBooleanAttribute(parser, MamFinIQ.COMPLETE_ATTRIBUTE, false)
        var rsmSet: RSMSet? = null

        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == RSMSet.ELEMENT) {
                        rsmSet = RSMSetProvider.INSTANCE.parse(parser)
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
            }
        }

        return MamFinIQ(
            rsmSet = rsmSet!!,
            isComplete = complete,
            queryId = queryId
        )
    }

}