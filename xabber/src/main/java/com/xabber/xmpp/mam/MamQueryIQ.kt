package com.xabber.xmpp.mam

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.archive.MessageArchiveManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.xmpp.SimpleNamedElement
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
    private val flipPage: Boolean = false,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        if (archiveAddress != null) {
            to = ContactJid.from(archiveAddress).bareJid
        }

        dataFormExtension?.let { addExtension(it) }
        rsmSet?.let { addExtension(it) }
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        if (queryId != null) {
            optAttribute(QUERY_ID_ATTRIBUTE, queryId)
        }
        if (node != null) {
            optAttribute(NODE_ATTRIBUTE, node)
        }
        rightAngleBracket()
        if (flipPage) {
            append(
                SimpleNamedElement(FLIP_PAGE_ELEMENT).toXML()
            )
        }
    }

    companion object {
        private const val ELEMENT = QUERY_ELEMENT
        private const val NAMESPACE = MessageArchiveManager.NAMESPACE
        private const val QUERY_ID_ATTRIBUTE = "queryid"
        private const val NODE_ATTRIBUTE = "node"
        private const val FLIP_PAGE_ELEMENT = "flip-page"

        fun createMamRequestIqAllMessagesInChat(
            chat: AbstractChat,
        ) = MamQueryIQ(
            archiveAddress = if (chat is GroupChat) chat.contactJid.bareJid else null,
            dataFormExtension = MamDataFormExtension(with = chat.contactJid.bareJid),
        )

        fun createMamRequestIqLastSavedMessage(
            accountJid: AccountJid,
        ) = MamQueryIQ(
            dataFormExtension = MamDataFormExtension(with = accountJid.bareJid),
            rsmSet = RSMSet(null, "", -1, -1, null, 1, null, -1),
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
            dataFormExtension = MamDataFormExtension(ids = listOf(stanzaId)),
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
            } else {
                null
            },
            rsmSet = RSMSet(null, messageStanzaId, -1, -1, null, max, null, -1),
            flipPage = true,
        )

        fun createMamRequestIqAllMessagesSince(
            chat: AbstractChat? = null,
            timestamp: Date = Date(),
            max: Int = 250,
        ) = when (chat) {
            is GroupChat -> {
                MamQueryIQ(
                    archiveAddress = chat.contactJid.bareJid,
                    dataFormExtension = MamDataFormExtension(start = timestamp),
                    rsmSet = RSMSet(null, null, -1, -1, null, max, null, -1)
                )
            }
            is RegularChat ->
                MamQueryIQ(
                    dataFormExtension = MamDataFormExtension(
                        with = chat.contactJid.bareJid,
                        start = timestamp
                    ),
                    rsmSet = RSMSet(null, null, -1, -1, null, max, null, -1)
                )
            else -> MamQueryIQ(
                dataFormExtension = MamDataFormExtension(start = timestamp),
                rsmSet = RSMSet(null, null, -1, -1, null, max, null, -1)
            )
        }

        fun createMamRequestGroupMembersMessages(
            group: GroupChat,
            memberId: String,
            afterStanzaId: String? = null,
            max: Int = 50,
        ) = MamQueryIQ(
            archiveAddress = group.contactJid.bareJid,
            rsmSet = RSMSet(null, afterStanzaId, -1, -1, null, max, null, -1),
            dataFormExtension = MamDataFormExtension(
                with = memberId,
            )
        )

    }

}

