package com.xabber.xmpp.mam

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.mam.MessageArchiveManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.xmpp.mam.MamQueryIQ.Companion
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smackx.rsm.packet.RSMSet
import org.jxmpp.jid.Jid
import java.util.*

/**
 * Query IQ to Message Archive
 * Must use [Companion] methods for creating instance of this IQ
 */
class MamQueryIQ private constructor(
    private val queryId: String? = null,
    private val node: String? = null,
    dataFormExtension: MamDataFormExtension? = null,
    rsmSet: RSMSet? = null,
    archiveAddress: Jid? = null,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        if (archiveAddress != null) to = ContactJid.from(archiveAddress).bareJid

        if (dataFormExtension != null) addExtension(dataFormExtension)
        if (rsmSet != null) addExtension(rsmSet)
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        if (queryId != null) optAttribute(QUERY_ID_ATTRIBUTE, queryId)
        if (node != null) optAttribute(NODE_ATTRIBUTE, node)
        rightAngleBracket()
    }

    companion object {
        private const val ELEMENT = QUERY_ELEMENT
        private const val NAMESPACE = MessageArchiveManager.NAMESPACE
        private const val QUERY_ID_ATTRIBUTE = "queryid"
        private const val NODE_ATTRIBUTE = "node"

        fun createMamRequestIqAllMessagesInChat(
            chat: AbstractChat,
        ) = MamQueryIQ(
            archiveAddress = if (chat is GroupChat) chat.contactJid.bareJid else null,
            dataFormExtension = MamDataFormExtension(with = chat.contactJid.bareJid),
        )

        fun createMamRequestIqLastMessageInChat(
            chat: AbstractChat,
        ) = MamQueryIQ(
            archiveAddress = if (chat is GroupChat) chat.contactJid.bareJid else null,
            dataFormExtension =
            if (chat is RegularChat) {
                MamDataFormExtension(with = chat.contactJid.bareJid)
            } else null,
            rsmSet = RSMSet(null, "", -1, -1, null, 1, null, -1),
        )

        fun createMamRequestIqMessageWithStanzaId(
            chat: AbstractChat,
            stanzaId: String,
        ) = MamQueryIQ(
            archiveAddress = if (chat is GroupChat) chat.contactJid.bareJid else null,
            dataFormExtension = MamDataFormExtension(id = stanzaId),
        )

        fun createMamRequestIqMessagesAfterInChat(
            chat: AbstractChat,
            messageStanzaId: String,
            max: Int = 50,
        ) = MamQueryIQ(
            archiveAddress = if (chat is GroupChat) chat.contactJid.bareJid else null,
            dataFormExtension =
            if (chat is RegularChat) {
                MamDataFormExtension(with = chat.contactJid.bareJid)
            } else null,
            rsmSet = RSMSet(null, messageStanzaId, -1, -1, null, max, null, -1),
        )

        fun createMamRequestIqAllMessagesSince(
            chat: AbstractChat? = null,
            timestamp: Date = Date(),
        ) = when (chat) {
            is GroupChat -> {
                MamQueryIQ(
                    archiveAddress = chat.contactJid.bareJid,
                    dataFormExtension = MamDataFormExtension(start = timestamp)
                )
            }
            is RegularChat -> MamQueryIQ(
                dataFormExtension = MamDataFormExtension(
                    with = chat.contactJid.bareJid,
                    start = timestamp
                )
            )
            else -> MamQueryIQ(dataFormExtension = MamDataFormExtension(start = timestamp))
        }

    }

}

