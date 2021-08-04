package com.xabber.android.data.extension.chat_markers

import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.StanzaSender
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.notification.MessageNotificationManager
import com.xabber.android.ui.OnMessageUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.chat_state.ChatState
import com.xabber.xmpp.sid.StanzaIdElement
import io.realm.Realm
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPConnectionRegistry
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.PacketParserUtils
import org.jivesoftware.smackx.carbons.packet.CarbonExtension
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import java.util.*

object ChatMarkerManager : OnPacketListener {

    override fun onStanza(connection: ConnectionItem, packet: Stanza) {
        (packet as? Message)?.let {
            when {
                ChatMarkersElements.MarkableExtension.from(packet) != null ->
                    sendReceived(packet, connection.account) // markable
                ChatMarkersElements.ReceivedExtension.from(packet) != null ->
                    markAsReceived(ChatMarkersElements.ReceivedExtension.from(packet)) // received
                ChatMarkersElements.DisplayedExtension.from(packet) != null ->
                    markAsDisplayed(ChatMarkersElements.DisplayedExtension.from(packet)) // displayed
                ChatMarkersElements.AcknowledgedExtension.from(packet) != null ->
                    LogManager.i(
                        ChatMarkerManager::class.java.simpleName,
                        "Got \"Acknowledged\" stanza, but no actions performed!"
                    )
            }
        }
    }

    fun sendDisplayed(messageRealmObject: MessageRealmObject) {
        if (messageRealmObject.stanzaId == null && messageRealmObject.originId == null) return

        val originalMessage: Message =
            PacketParserUtils.parseStanza(messageRealmObject.originalStanza) ?: return
        val stanzaIds =
            originalMessage.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE)

        if (originalMessage.stanzaId == null || originalMessage.stanzaId.isEmpty()) {

            if (stanzaIds.isEmpty()) {
                LogManager.exception(
                    ChatMarkerManager::class.java.simpleName,
                    NullPointerException("Can't find any stanza id in message!")
                )
                return
            }
            stanzaIds.filterIsInstance<StanzaIdElement>().map { stanzaIdElement ->
                if (stanzaIdElement.by != null && stanzaIdElement.by == messageRealmObject.user.bareJid.toString()) {
                    originalMessage.stanzaId = stanzaIdElement.id
                    return@map
                } else originalMessage.stanzaId = stanzaIdElement.id
            }

        }

        try {
            StanzaSender.sendStanza(
                messageRealmObject.account,
                Message(messageRealmObject.user.jid).apply {
                    addExtension(
                        ChatMarkersElements.DisplayedExtension(originalMessage.stanzaId).apply {
                            setStanzaIdExtensions(stanzaIds)
                        }
                    )
                    type = Message.Type.chat
                }
            )
        } catch (e: NetworkException) {
            LogManager.exception(ChatMarkerManager::class.java.simpleName, e)
        }
    }

    fun processCarbonsMessage(
        account: AccountJid,
        message: Message,
        direction: CarbonExtension.Direction
    ) {
        if (direction == CarbonExtension.Direction.sent) {
            val extension = ChatMarkersElements.DisplayedExtension.from(message)
            if (extension != null) {
                val companion: ContactJid = try {
                    ContactJid.from(message.to).bareUserJid
                } catch (e: ContactJidCreateException) {
                    LogManager.exception(ChatMarkerManager::class.java.simpleName, e)
                    return
                }
                ChatManager.getInstance().getChat(account, companion)?.let {
                    it.markAsRead(extension.getId(), extension.stanzaId, false)
                    MessageNotificationManager.removeChatWithTimer(account, companion)

                    // start grace period
                    AccountManager.getInstance().startGracePeriod(account)
                }
            }
        } else if (direction == CarbonExtension.Direction.received) {
            if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                BackpressureMessageMarker.getInstance().markMessage(
                    ChatMarkersElements.ReceivedExtension.from(message).getId(),
                    ChatMarkersElements.ReceivedExtension.from(message).stanzaId,
                    ChatMarkersState.received,
                    account
                )
            } else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                BackpressureMessageMarker.getInstance().markMessage(
                    ChatMarkersElements.DisplayedExtension.from(message).getId(),
                    ChatMarkersElements.DisplayedExtension.from(message).stanzaId,
                    ChatMarkersState.displayed,
                    account
                )
            }
        }
    }

    private fun sendReceived(message: Message, account: AccountJid) {
        if (message.stanzaId == null || message.stanzaId.isEmpty()) return
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                StanzaSender.sendStanza(
                    account,
                    Message(message.from).apply {
                        addExtension(ChatMarkersElements.ReceivedExtension(message.stanzaId))
                        thread = message.thread
                        type = Message.Type.chat
                    }
                )
            } catch (e: NetworkException) {
                LogManager.exception(ChatMarkerManager::class.java.simpleName, e)
            }
        }
    }

    //possible bug
    private fun markAsDisplayed(displayedExtension: ChatMarkersElements.DisplayedExtension) {
        if (displayedExtension.getId() == null || displayedExtension.getId().isEmpty()) {
            if (displayedExtension.stanzaId.isNotEmpty()) markAsDisplayed(displayedExtension.stanzaId.first())
        } else markAsDisplayed(displayedExtension.getId())
    }

    private fun markAsDisplayed(messageID: String) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realm1: Realm ->
                    realm1.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ORIGIN_ID, messageID)
                        .findFirst()
                        ?.let { first ->
                            realm1.where(MessageRealmObject::class.java)
                                .equalTo(
                                    MessageRealmObject.Fields.ACCOUNT,
                                    first.account.toString()
                                )
                                .equalTo(MessageRealmObject.Fields.USER, first.user.toString())
                                .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                .notEqualTo(
                                    MessageRealmObject.Fields.MESSAGE_STATUS,
                                    MessageStatus.DISPLAYED.toString()
                                )
                                .notEqualTo(
                                    MessageRealmObject.Fields.MESSAGE_STATUS,
                                    MessageStatus.UPLOADING.toString()
                                )
                                .lessThanOrEqualTo(
                                    MessageRealmObject.Fields.TIMESTAMP,
                                    first.timestamp
                                )
                                .findAll()
                                ?.let { results ->
                                    results.setString(
                                        MessageRealmObject.Fields.MESSAGE_STATUS,
                                        MessageStatus.DISPLAYED.toString()
                                    )
                                    notifySamUiListeners(OnMessageUpdatedListener::class.java)
                                }
                        }
                }
            }
        }
    }

    //possible bug
    private fun markAsReceived(receivedExtension: ChatMarkersElements.ReceivedExtension) {
        if (receivedExtension.getId() == null || receivedExtension.getId().isEmpty()) {
            if (receivedExtension.stanzaId.isNotEmpty()) markAsReceived(receivedExtension.stanzaId.first())
        } else markAsReceived(receivedExtension.getId())
    }

    private fun markAsReceived(stanzaID: String) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realm1: Realm ->
                    realm1.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ORIGIN_ID, stanzaID)
                        .findFirst()
                        ?.let { first ->
                            realm1.where(MessageRealmObject::class.java)
                                .equalTo(
                                    MessageRealmObject.Fields.ACCOUNT,
                                    first.account.toString()
                                )
                                .equalTo(MessageRealmObject.Fields.USER, first.user.toString())
                                .equalTo(MessageRealmObject.Fields.INCOMING, false)
                                .notEqualTo(
                                    MessageRealmObject.Fields.MESSAGE_STATUS,
                                    MessageStatus.RECEIVED.toString()
                                )
                                .notEqualTo(
                                    MessageRealmObject.Fields.MESSAGE_STATUS,
                                    MessageStatus.UPLOADING.toString()
                                )
                                .lessThanOrEqualTo(
                                    MessageRealmObject.Fields.TIMESTAMP,
                                    first.timestamp
                                )
                                .findAll()
                                ?.let { results ->
                                    results.setString(
                                        MessageRealmObject.Fields.MESSAGE_STATUS,
                                        MessageStatus.RECEIVED.toString()
                                    )
                                    notifySamUiListeners(OnMessageUpdatedListener::class.java)
                                }
                        }
                }
            }

        }
    }

    init {
        XMPPConnectionRegistry.addConnectionCreationListener { connection: XMPPConnection ->
            ServiceDiscoveryManager.getInstanceFor(connection)
                .addFeature(ChatMarkersElements.NAMESPACE)

            val eligibleForChatMarkerFilter =
                object : StanzaExtensionFilter(ChatStateManager.NAMESPACE) {
                    override fun accept(message: Stanza): Boolean {
                        if (!message.hasStanzaIdSet()) return false
                        if (super.accept(message)) {
                            message.getExtension(ChatStateManager.NAMESPACE).elementName
                                ?.let { ChatState.valueOf(it) == ChatState.active }
                        }
                        return true
                    }
                }

            val chatMarketFilter = object : StanzaExtensionFilter(ChatMarkersElements.NAMESPACE) {}

            connection.addPacketInterceptor(
                { it.addExtension(ChatMarkersElements.MarkableExtension()) },
                AndFilter(
                    MessageTypeFilter.NORMAL_OR_CHAT,
                    MessageWithBodiesFilter.INSTANCE,
                    chatMarketFilter,
                    eligibleForChatMarkerFilter
                )
            )
        }
    }

}