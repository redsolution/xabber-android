package com.xabber.android.data.extension.retract

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.archive.MessageArchiveManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageHandler
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.OnRosterReceivedListener
import com.xabber.android.ui.OnMessageUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.retract.incoming.elements.IncomingInvalidateExtensionElement.Companion.getIncomingInvalidateExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingInvalidateExtensionElement.Companion.hasIncomingInvalidateExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement.Companion.getIncomingReplaceExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingReplaceExtensionElement.Companion.hasIncomingReplaceExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement.Companion.getIncomingRetractAllExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractAllExtensionElement.Companion.hasIncomingRetractAllExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractExtensionElement.Companion.getIncomingRetractExtensionElement
import com.xabber.xmpp.retract.incoming.elements.IncomingRetractExtensionElement.Companion.hasIncomingRetractExtensionElement
import com.xabber.xmpp.retract.incoming.elements.RetractsResultIq
import com.xabber.xmpp.retract.outgoing.ReplaceMessageIq
import com.xabber.xmpp.retract.outgoing.RequestRetractsIq
import com.xabber.xmpp.retract.outgoing.RetractAllIq
import com.xabber.xmpp.retract.outgoing.RetractMessageIq
import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.StanzaIdElement
import com.xabber.xmpp.smack.XMPPTCPConnection
import io.realm.Realm
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.PacketParserUtils
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager

object RetractManager : OnPacketListener, OnRosterReceivedListener {

    const val NAMESPACE = "https://xabber.com/protocol/rewrite"
    const val NAMESPACE_NOTIFY = "$NAMESPACE#notify"

    override fun onRosterReceived(accountItem: AccountItem) {
        if (accountItem.retractVersion.isNullOrEmpty()) {
            sendLocalArchiveRetractVersionRequest(accountItem.account)
        } else {
            sendMissedChangesInLocalArchiveRequest(accountItem.account)
        }
    }

    override fun onStanza(connection: ConnectionItem, packet: Stanza) {
        if (packet is Message && packet.type == Message.Type.headline) {
            when {
                packet.hasIncomingInvalidateExtensionElement() ->
                    handleInvalidateMessage(
                        connection.account,
                        ContactJid.from(packet.from),
                        packet.getIncomingInvalidateExtensionElement().version
                    )

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
                realm.where(MessageRealmObject::class.java)
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
                                    MessageManager.getInstance().removeMessage(messagePrimaryKey)
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
                    {
                        DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                            realm.executeTransaction { transaction ->
                                transaction.where(MessageRealmObject::class.java)
                                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, primaryKey)
                                    .findFirst()
                                    ?.apply {
                                        text = newMessageText
                                    }
                                    ?.also {
                                        transaction.copyToRealmOrUpdate(it)
                                    }
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
        listener: BaseIqResultUiListener? = null
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val isGroup = ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat

            listener?.onSend()

            AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                RetractAllIq(
                    contactJid,
                    false,
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

    private fun handleInvalidateMessage(
        accountJid: AccountJid,
        from: ContactJid,
        version: String? = null
    ) {
        if (accountJid.bareJid.toString() == from.bareJid.toString()) {
            reInitLocalAtchive(accountJid, version)
        } else {
            reInitRemoteArchive(accountJid, from, version)
        }
    }

    private fun reInitLocalAtchive(accountJid: AccountJid, version: String? = null) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { transaction ->
                    transaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .findAll()
                        ?.deleteAllFromRealm()
                }
            }
            AccountManager.getInstance().getAccount(accountJid)
                ?.apply {
                    startHistoryTimestamp = null
                    retractVersion = version
                }
                ?.also {
                    AccountRepository.saveAccountToRealm(it)
                    MessageArchiveManager.onRosterReceived(it)
                }
        }
    }

    private fun reInitRemoteArchive(accountJid: AccountJid, contactJid: ContactJid, version: String? = null) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { transaction ->
                    transaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(MessageRealmObject.Fields.USER, contactJid.bareJid.toString())
                        .findAll()
                        ?.deleteAllFromRealm()
                }
            }
            (ChatManager.getInstance().getChat(accountJid, contactJid) as? GroupChat)
                ?.apply { retractVersion = version }
                ?.also { ChatManager.getInstance().saveOrUpdateChatDataToRealm(it) }
            MessageArchiveManager.loadLastMessageInChat(accountJid, contactJid)
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
            if (!isMessageExistsInDatabase(accountJid, contactJid, messageStanzaId)) {
                return@runInBackground
            }

            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction: Realm ->
                    realmTransaction.where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
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
        Application.getInstance().runInBackground {
            if (!isMessageExistsInDatabase(accountJid, contactJid, messageStanzaId)) {
                return@runInBackground
            }

            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                val isIncoming = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, messageStanzaId)
                    .findFirst()
                    ?.isIncoming ?: false

                // hack because inner replaced message has not any from
                newMessage.from = if (isIncoming) contactJid.jid else accountJid.fullJid

                MessageHandler.parseMessage(accountJid, contactJid, newMessage)
            }
        }
        version?.let { updateRetractVersion(accountJid, contactJid, it) }
    }

    private fun sendMissedChangesInLocalArchiveRequest(accountJid: AccountJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(accountJid)?.let { account ->
                if (!account.retractVersion.isNullOrEmpty()) {
                    account.connection.sendIqWithResponseCallback(
                        RequestRetractsIq(version = account.retractVersion),
                        { /* ignore */ },
                        {
                            LogManager.exception(this, it)
                        }
                    )
                }
            }
        }
    }

    fun sendMissedChangesInRemoteArchiveForChatRequest(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(accountJid)?.let { account ->
                (ChatManager.getInstance().getChat(accountJid, contactJid) as? GroupChat)?.let { chat ->
                    if (!account.retractVersion.isNullOrEmpty()) {
                        account.connection.sendIqWithResponseCallback(
                            RequestRetractsIq(version = chat.retractVersion, archiveAddress = contactJid),
                            { /* ignore */ },
                            {
                                LogManager.exception(this, it)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Request only current retract version from own local archive
     * Mainly use after cold start (when account has no any retract version yet)
     */
    private fun sendLocalArchiveRetractVersionRequest(accountJid: AccountJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(accountJid)?.let { account ->
                account.connection.sendIqWithResponseCallback(
                    RequestRetractsIq(lessThan = null),
                    { stanza ->
                        if (stanza is RetractsResultIq && stanza.type == IQ.Type.result && stanza.version != null) {
                            updateAccountLocalArchiveRetractVersion(accountJid, stanza.version)
                        }
                    },
                    {
                        LogManager.exception(this, it)
                    }
                )
            }
        }
    }

    /**
     * Request only current retract version from remote archive of chat
     * Mainly use after cold start for groups (when we has no any retract version yet for group)
     */
    fun sendRemoteArchiveRetractVersionRequest(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(accountJid)?.let { account ->
                account.connection.sendIqWithResponseCallback(
                    RequestRetractsIq(archiveAddress = contactJid, lessThan = null),
                    { stanza ->
                        if (stanza is RetractsResultIq && stanza.type == IQ.Type.result && stanza.version != null) {
                            updateRetractVersion(accountJid, contactJid, stanza.version)
                        }
                    },
                    {
                        LogManager.exception(this, it)
                    }
                )
            }
        }
    }

    /**
     * Save current retract version to database with remote\local archive recognizing
     */
    private fun updateRetractVersion(accountJid: AccountJid, contactJid: ContactJid, version: String) {
        if (ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat) {
            (ChatManager.getInstance().getChat(accountJid, contactJid) as? GroupChat)
                ?.apply { retractVersion = version }
                .also { ChatManager.getInstance().saveOrUpdateChatDataToRealm(it) }
        } else {
            updateAccountLocalArchiveRetractVersion(accountJid, version)
        }
    }

    /**
     * Save local archive retract version to account
     */
    private fun updateAccountLocalArchiveRetractVersion(accountJid: AccountJid, version: String) {
        AccountManager.getInstance().getAccount(accountJid)
            ?.apply { retractVersion = version }
            .also { AccountRepository.saveAccountToRealm(it) }
    }

    /**
     * If database doesn't contains message, we must ignore replace or retract request
     */
    private fun isMessageExistsInDatabase(accountJid: AccountJid, contactJid: ContactJid, stanzaId: String): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalThreadStateException("This method isn't ready to be used in main thread")
        }
        DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
            return realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageRealmObject.Fields.USER, contactJid.toString())
                .equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId) != null
        }
    }

}