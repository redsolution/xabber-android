package com.xabber.android.data.extension.retract

import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnAuthenticatedListener
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.OnMessageUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement.Companion.getIncomingReplaceExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement.Companion.hasIncomingReplaceExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement.Companion.getIncomingRetractAllExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement.Companion.hasIncomingRetractAllExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractExtensionElement.Companion.getIncomingRetractExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractExtensionElement.Companion.hasIncomingRetractExtensionElement
import com.xabber.xmpp.retract.incoming.elements.ReplacedExtensionElement.Companion.getReplacedElement
import com.xabber.xmpp.retract.outgoing.ReplaceMessageIq
import com.xabber.xmpp.retract.outgoing.RetractAllIq
import com.xabber.xmpp.retract.outgoing.RetractMessageIq
import com.xabber.xmpp.retract.outgoing.SubscribeToRetractNotificationsIq
import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.StanzaIdElement
import com.xabber.xmpp.smack.XMPPTCPConnection
import io.realm.Realm
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.PacketParserUtils
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jxmpp.util.XmppDateTime

object RetractManager : OnPacketListener, OnAuthenticatedListener {

    const val NAMESPACE = "https://xabber.com/protocol/rewrite"
    const val NAMESPACE_NOTIFY = "$NAMESPACE#notify"

    override fun onAuthenticated(connectionItem: ConnectionItem) {
        val accountJid = connectionItem.account
        val xmppConnection = connectionItem.connection

        Application.getInstance().runInBackgroundNetworkUserRequest {

            /* Subscribe for changes and request missed changes with local archive */
            xmppConnection.sendIqWithResponseCallback(
                SubscribeToRetractNotificationsIq(
                    AccountManager.getInstance().getAccount(accountJid)?.retractVersion
                )
            ) { /* ignore result */ }

            /* Subscribe for changes and request missed changes with group remote archives */
            ChatManager.getInstance().getChats(accountJid).map { abstractChat ->
                (abstractChat as? GroupChat)?.let { groupChat ->
                    xmppConnection.sendIqWithResponseCallback(
                        SubscribeToRetractNotificationsIq(
                            groupChat.retractVersion, groupChat.contactJid
                        )
                    ) { /* ignore result */ }
                }
            }
        }
    }

    override fun onStanza(connection: ConnectionItem, packet: Stanza) {
        if (packet is Message && packet.type == Message.Type.headline) {
            when {
                packet.hasIncomingReplaceExtensionElement() ->
                    handleReplaceMessage(
                        connection.account,
                        packet.getIncomingReplaceExtensionElement()!!.conversationContactJid,
                        packet.getIncomingReplaceExtensionElement()!!.version,
                        packet.getIncomingReplaceExtensionElement()!!.message,
                        packet.getIncomingReplaceExtensionElement()!!.messageStanzaId,
                    )

                packet.hasIncomingRetractExtensionElement() ->
                    handleRetractMessage(
                        connection.account,
                        packet.getIncomingRetractExtensionElement()!!.contactJid,
                        packet.getIncomingRetractExtensionElement()!!.version,
                        packet.getIncomingRetractExtensionElement()!!.messageId
                    )

                packet.hasIncomingRetractAllExtensionElement() ->
                    handleRetractAllMessage(
                        connection.account,
                        packet.getIncomingRetractAllExtensionElement()!!.contactJid,
                        packet.getIncomingRetractAllExtensionElement()!!.version
                    )
            }
        }
    }

    fun isSupported(connection: XMPPTCPConnection): Boolean {
        return try {
            ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(NAMESPACE)
        } catch (e: Exception) {
            LogManager.exception(this, e)
            false
        }
    }

    fun isSupported(accountJid: AccountJid): Boolean {
        return isSupported(AccountManager.getInstance().getAccount(accountJid)?.connection ?: return false)
    }

    fun sendRetractMessagesRequest(
        accountJid: AccountJid,
        archiveJid: ContactJid,
        list: List<String>,
        symmetrically: Boolean,
        listener: BaseIqResultUiListener? = null,
    ) {
        list.map { sendRetractMessageRequest(accountJid, archiveJid, it, symmetrically, listener) }
    }

    fun sendRetractMessageRequest(
        accountJid: AccountJid,
        contactJid: ContactJid,
        messagePrimaryKey: String,
        symmetrically: Boolean,
        listener: BaseIqResultUiListener? = null,
    ) {
        val isGroup = ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat

        Application.getInstance().runInBackgroundNetworkUserRequest {

            listener?.onSend()

            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction: Realm ->
                    realmTransaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messagePrimaryKey)
                        .findFirst()
                        ?.let { message ->
                            AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                                RetractMessageIq(
                                    message.stanzaId,
                                    accountJid,
                                    symmetrically,
                                    if (isGroup) contactJid else null
                                ),
                                { packet: Stanza? ->
                                    if (packet is IQ && packet.type == IQ.Type.result) {
                                        message.deleteFromRealm()
                                        notifySamUiListeners(OnMessageUpdatedListener::class.java)
                                        listener?.onResult()
                                    }
                                },
                                listener
                            )
                        }
                }
            }
        }
    }

    fun sendReplaceMessageRequest(
        accountJid: AccountJid,
        contactJid: ContactJid,
        primaryKey: String,
        message: Message,
        baseIqResultUiListener: BaseIqResultUiListener? = null
    ) {
        //todo this
    }

    fun sendReplaceMessageTextRequest(
        accountJid: AccountJid,
        contactJid: ContactJid,
        primaryKey: String,
        newMessageText: String,
        baseIqResultUiListener: BaseIqResultUiListener? = null
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                val isGroup = ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat
                val messageRealmObject = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, primaryKey)
                    .findFirst() ?: throw NullPointerException("Unable find message by provided primary key")

                val oldMessageStanza = PacketParserUtils.parseStanza<Message>(messageRealmObject.originalStanza)

                val messageStanza = Message().apply {
                    stanzaId = messageRealmObject.originId
                    body = newMessageText
                    addExtension(oldMessageStanza.getExtension(OriginIdElement.ELEMENT))
                    addExtension(oldMessageStanza.getExtension(StanzaIdElement.ELEMENT))
                }
                AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                    ReplaceMessageIq(
                        messageRealmObject.stanzaId,
                        accountJid,
                        messageStanza,
                        contactJid.takeIf { isGroup }
                    ),
                    { packet ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            realm.executeTransaction { transactionRealm ->
                                transactionRealm.where(MessageRealmObject::class.java)
                                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, primaryKey)
                                    .findFirst()
                                    ?.apply { text = newMessageText }
                                    ?.also { transactionRealm.copyToRealmOrUpdate(it) }
                            }
                        }
                    },
                    baseIqResultUiListener
                )
            }
        }
    }

    fun sendRetractAllMessagesRequest(
        accountJid: AccountJid,
        contactJid: ContactJid,
        symmetric: Boolean,
        listener: BaseIqResultUiListener? = null
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val isGroup = ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat

            listener?.onSend()

            AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                RetractAllIq(
                    contactJid,
                    symmetric,
                    if (isGroup) contactJid else null
                ),
                { packet ->
                    if (packet is IQ && packet.type == IQ.Type.result) {
                        MessageManager.getInstance().clearHistory(accountJid, contactJid)
                        listener?.onResult()
                        notifySamUiListeners(OnMessageUpdatedListener::class.java)
                    }
                },
                listener
            )

        }
    }

    private fun handleRetractAllMessage(accountJid: AccountJid, contactJid: ContactJid, version: String?) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction: Realm ->
                    realmTransaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(MessageRealmObject.Fields.USER, contactJid.bareJid.toString())
                        .findAll()
                        ?.deleteAllFromRealm()

                    notifySamUiListeners(OnMessageUpdatedListener::class.java)
                }
            }
            version?.let { updateRetractVersion(accountJid, contactJid, it) }
        }
    }

    private fun handleRetractMessage(
        accountJid: AccountJid, contactJid: ContactJid, version: String?, messageStanzaId: String
    ) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction: Realm ->
                    realmTransaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(MessageRealmObject.Fields.USER, contactJid.bareJid.toString())
                        .equalTo(MessageRealmObject.Fields.STANZA_ID, messageStanzaId)
                        .findFirst()
                        ?.deleteFromRealm()

                    notifySamUiListeners(OnMessageUpdatedListener::class.java)
                }
            }

            version?.let { updateRetractVersion(accountJid, contactJid, it) }
        }
    }

    private fun handleReplaceMessage(
        accountJid: AccountJid,
        contactJid: ContactJid,
        version: String?,
        newMessage: Message,
        messageStanzaId: String,
    ) {
//        newMessage.from = contactJid.jid // hack because inner replaced message has not any from
//        MessageHandler.parseMessage(accountJid, contactJid, newMessage)
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction ->
                    realmTransaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(MessageRealmObject.Fields.STANZA_ID, messageStanzaId)
                        .findFirst()
                        ?.apply {
                            text = newMessage.body
                            newMessage.getReplacedElement()?.timestamp?.let {
                                editedTimestamp = XmppDateTime.parseDate(it).time
                            }
                        }
                        ?.also { message -> realmTransaction.copyToRealmOrUpdate(message) }
                }
            }
        }

        version?.let { updateRetractVersion(accountJid, contactJid, it) }
    }

    private fun updateRetractVersion(accountJid: AccountJid, contactJid: ContactJid, version: String) {
        if (ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat) {
            (ChatManager.getInstance().getChat(accountJid, contactJid) as? GroupChat)
                ?.apply { retractVersion = version }
                .also { ChatManager.getInstance().saveOrUpdateChatDataToRealm(it) }
        } else {
            AccountManager.getInstance().getAccount(accountJid)
                ?.apply { retractVersion = version }
                .also { AccountRepository.saveAccountToRealm(it) }
        }
    }

}