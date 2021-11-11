package com.xabber.xmpp.sync

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.sync.SyncManager
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.provider.IQProvider
import org.xmlpull.v1.XmlPullParser
import java.util.*

class IncomingSetSyncIqProvider : IQProvider<IncomingSetSyncIQ>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): IncomingSetSyncIQ {

        lateinit var conversationJid: ContactJid
        var timestamp: Long = 0
        lateinit var innerElement: NamedElement

        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.getAttributeValue(SyncManager.NAMESPACE, IncomingSetSyncIQ.STAMP_ATTRIBUTE) != null) {
                        timestamp =
                            parser.getAttributeValue(SyncManager.NAMESPACE, IncomingSetSyncIQ.STAMP_ATTRIBUTE).toLong()
                    }
                    when (parser.name) {
                        IncomingSetSyncIQ.QUERY_ELEMENT -> {
                            timestamp = parser
                                .getAttributeValue(null, "stamp")
                                .toLong()
                        }
                        ConversationExtensionElement.ELEMENT_NAME -> {
                            conversationJid = ContactJid.from(
                                parser.getAttributeValue(
                                    "",
                                    ConversationExtensionElement.JID_ATTRIBUTE
                                )
                            )
                        }
                        DeletedElement.ELEMENT_NAME -> {
                            innerElement = DeletedElement()
                        }
                    }
                    parser.next()
                }

                XmlPullParser.END_TAG ->
                    if (IncomingSetSyncIQ.QUERY_ELEMENT == parser.name) {
                        break@outerloop
                    } else parser.next()

                else -> parser.next()
            }
        }

        return IncomingSetSyncIQ(timestamp, ConversationExtensionElement(conversationJid, innerElement))
    }

}