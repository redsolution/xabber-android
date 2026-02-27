package com.xabber.xmpp.groups.create

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ
import com.xabber.xmpp.groups.create.CreateGroupchatIQ.Companion.ELEMENT_LOCALPART
import org.jivesoftware.smack.provider.IQProvider
import org.xmlpull.v1.XmlPullParser

/**
    WARN! This is not fully implemented parser!
 */
class GroupchatCreateIqResultProvider: IQProvider<CreateGroupchatIQ.ResultIq>() {

    override fun parse(parser: XmlPullParser?, initialDepth: Int): CreateGroupchatIQ.ResultIq {
        var localpart = ""

        outerloop@ while (true) {
            when (parser?.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    GroupchatAbstractQueryIQ.ELEMENT -> {
                        parser.next()
                    }
                    ELEMENT_LOCALPART -> {
                        localpart = parser.nextText()
                        parser.next()
                    }
                    else -> parser.next()
                }
                XmlPullParser.END_TAG -> if (GroupchatAbstractQueryIQ.ELEMENT == parser.name) {
                    break@outerloop
                } else parser.next()
                else -> parser!!.next()
            }
        }

        return CreateGroupchatIQ.ResultIq(localpart)
    }
}