package com.xabber.android.data.extension.archive

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageHandler
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.OnRosterReceivedListener
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.OnLastHistoryLoadErrorListener
import com.xabber.android.ui.OnLastHistoryLoadFinishedListener
import com.xabber.android.ui.OnLastHistoryLoadStartedListener
import com.xabber.android.ui.forEachOnUi
import com.xabber.xmpp.mam.MamFinIQ
import com.xabber.xmpp.mam.MamQueryIQ
import com.xabber.xmpp.mam.MamResultExtensionElement
import com.xabber.xmpp.mam.hasMamResultExtensionElement
import io.realm.Realm
import io.realm.Sort
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import java.util.*

object MessageArchiveManager : OnRosterReceivedListener {

    const val NAMESPACE = "urn:xmpp:mam:2"

    /**
     * When loadAllMissedMessagedSinceLastReconnectFromOwnArchiveForWholeAccount
     * Use to store groups with new messages in local archive to be re-requested from group archive
     */
    private val groupsQueryToRequestArchive = mutableSetOf<GroupChat>()

    /**
     * List of blocks that needs to reduce requests spam, chat list hopping and etc.
     */
    private val activeRequests = mutableSetOf<AbstractChat>()

    var isArchiveFetching: Boolean = false
        private set(value) {
            field = value
            Application.getInstance().getManagers(OnMessageArchiveFetchingListener::class.java)
                .map(OnMessageArchiveFetchingListener::onMessageArchiveFetching)
        }

    private val stanzaFilter: StanzaFilter by lazy {
        StanzaFilter { stanza -> stanza is Message && stanza.hasMamResultExtensionElement() }
    }

    private fun createNewRegularMamResultListener(accountJid: AccountJid): StanzaListener {
        return StanzaListener { packet ->
            packet.extensions.filterIsInstance<MamResultExtensionElement>().forEach { element ->
                val forwardedElement = element.forwarded.forwardedStanza
                val contactJid =
                    if (forwardedElement.from.asBareJid() == accountJid.fullJid.asBareJid()) {
                        ContactJid.from(forwardedElement.to.asBareJid().toString())
                    } else {
                        ContactJid.from(forwardedElement.from.asBareJid().toString())
                    }

                val delayInformation = element.forwarded.delayInformation

                if (ChatManager.getInstance().getChat(accountJid, contactJid) is GroupChat
                    && packet.from.asBareJid().toString() == accountJid.bareJid.toString()
                ) {
                    // If we received group message from local archive
                    // Don't save this message and request it from remote archive
                    groupsQueryToRequestArchive.add(
                        ChatManager.getInstance().getChat(accountJid, contactJid) as GroupChat
                    )
                } else if (forwardedElement != null && forwardedElement is Message) {
                    MessageHandler.parseMessage(
                        accountJid, contactJid, forwardedElement, delayInformation
                    )
                }
            }
        }
    }

    override fun onRosterReceived(accountItem: AccountItem) {
        //If it first (cold) start, try to retrieve most last message to get account accountStartHistoryTimestamp
        if (accountItem.startHistoryTimestamp == null) {
            accountItem.startHistoryTimestamp = Date()
            AccountRepository.saveAccountToRealm(accountItem)
            loadLastSavedMessage(accountItem)
            loadLastMessagesInAllChats(accountItem)
        } else {
            loadAllMissedMessagedSinceLastReconnectFromOwnArchiveForWholeAccount(accountItem)
        }
    }

    fun isSupported(accountItem: AccountItem) =
        try {
            ServiceDiscoveryManager.getInstanceFor(accountItem.connection).supportsFeature(
                accountItem.connection.user.asBareJid(), NAMESPACE
            )
        } catch (e: Exception) {
            LogManager.exception(this, e)
            false
        }

    fun isSupported(connection: XMPPConnection) = try {
        ServiceDiscoveryManager.getInstanceFor(connection).supportsFeature(
            connection.user.asBareJid(), NAMESPACE
        )
    } catch (e: Exception) {
        LogManager.exception(this, e)
        false
    }

    fun loadMessageByStanzaId(chat: AbstractChat, stanzaId: String) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(chat.account)?.connection?.let { connection ->

                val listener = createNewRegularMamResultListener(chat.account)

                connection.addAsyncStanzaListener(listener, stanzaFilter)
                connection.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqMessageWithStanzaId(chat, stanzaId),
                    { connection.removeAsyncStanzaListener(listener) },
                    { exception ->
                        LogManager.exception(this, exception)
                        connection.removeAsyncStanzaListener(listener)
                    }
                )
            }
        }
    }

    fun loadAllMessagesInChat(chat: AbstractChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().getAccount(chat.account)?.connection?.let { connection ->
                val listener = createNewRegularMamResultListener(chat.account)
                connection.addAsyncStanzaListener(listener, stanzaFilter)
                connection.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqAllMessagesInChat(chat),
                    { connection.removeAsyncStanzaListener(listener) },
                    { exception ->
                        LogManager.exception(this, exception)
                        connection.removeAsyncStanzaListener(listener)
                    }
                )
            }
        }
    }

    private fun loadLastSavedMessage(accountItem: AccountItem) {
        Application.getInstance().runInBackground {
            val connection = accountItem.connection
            val accountJid = accountItem.account
            val listener = createNewRegularMamResultListener(accountJid)

            connection.addAsyncStanzaListener(listener, stanzaFilter)
            connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqLastSavedMessage(accountJid),
                {
                    Application.getInstance().getManagers(OnHistoryLoaded::class.java).map {
                        it.onHistoryLoaded(accountItem)
                    }
                    connection.removeAsyncStanzaListener(listener)
                },
                {
                    Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                        .map { it.onHistoryLoaded(accountItem) }
                    connection.removeAsyncStanzaListener(listener)
                }
            )
        }
    }

    private fun loadLastMessagesInAllChats(accountItem: AccountItem) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val accountJid = accountItem.account
            val connection = accountItem.connection
            val listener = createNewRegularMamResultListener(accountJid)
            val contacts = RosterManager.getInstance().getAccountRosterContacts(accountJid)
            connection.addAsyncStanzaListener(listener, stanzaFilter)

            contacts.mapIndexed { index, rosterContact ->
                val contactJid = rosterContact.contactJid
                val chat = ChatManager.getInstance().getChat(accountJid, contactJid)
                    ?: ChatManager.getInstance().createRegularChat(accountJid, contactJid)
                connection.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqLastMessageInChat(chat),
                    {
                        if (index == contacts.size - 1) {
                            Application.getInstance().getManagers(OnHistoryLoaded::class.java).map {
                                it.onHistoryLoaded(accountItem)
                            }
                            connection.removeAsyncStanzaListener(listener)
                        }
                    },
                    { exception ->
                        LogManager.exception(this, exception)
                        if (index == contacts.size - 1) {
                            Application.getInstance().getManagers(OnHistoryLoaded::class.java).map {
                                it.onHistoryLoaded(accountItem)
                            }
                            connection.removeAsyncStanzaListener(listener)
                        }
                    }
                )
            }
        }
    }

    fun loadLastMessageInChat(accountJid: AccountJid, contactJid: ContactJid) {
        val chat = ChatManager.getInstance().getChat(accountJid, contactJid)
            ?: ChatManager.getInstance().createRegularChat(accountJid, contactJid)
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val connection = AccountManager.getInstance().getAccount(accountJid)?.connection
                ?: return@runInBackgroundNetworkUserRequest
            val listener = createNewRegularMamResultListener(accountJid)

            connection.addAsyncStanzaListener(listener, stanzaFilter)
            connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqLastMessageInChat(chat),
                { connection.removeAsyncStanzaListener(listener) },
                { exception ->
                    connection.removeAsyncStanzaListener(listener)
                    LogManager.exception(this, exception)
                }
            )
        }
    }

    fun loadAllMissedMessagedSinceLastReconnectFromOwnArchiveForWholeAccount(accountItem: AccountItem) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            isArchiveFetching = true

            val connection = accountItem.connection
            val listener = createNewRegularMamResultListener(accountItem.account)
            connection.addAsyncStanzaListener(listener, stanzaFilter)
            connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqAllMessagesSince(
                    timestamp = getLastAccountMessageInRealmTimestamp(accountItem.account)
                        ?.let { Date(it + 1) } ?: Date()
                ),
                { packet ->
                    isArchiveFetching = false
                    if (packet is MamFinIQ && packet.isComplete ?: true) {
                        if (groupsQueryToRequestArchive.size == 0) {
                            Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                                .forEach { it.onHistoryLoaded(accountItem) }
                            connection.removeAsyncStanzaListener(listener)
                        } else {
                            groupsQueryToRequestArchive.map {
                                loadAllMissedMessagesSinceLastDisconnectForCurrentChat(it)
                            }
                            groupsQueryToRequestArchive.clear()
                        }
                    } else {
                        //todo better make invalidation (or else)
                        Thread.sleep(5000)
                        loadAllMissedMessagedSinceLastReconnectFromOwnArchiveForWholeAccount(
                            accountItem
                        )
                    }
                },
                { exception ->
                    isArchiveFetching = false
                    LogManager.exception(this, exception)
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java).forEach {
                            it.onHistoryLoaded(accountItem)
                        }
                        connection.removeAsyncStanzaListener(listener)
                    } else {
                        groupsQueryToRequestArchive.map {
                            loadAllMissedMessagesSinceLastDisconnectForCurrentChat(it)
                        }
                        groupsQueryToRequestArchive.clear()
                    }
                }
            )
        }
    }

    fun loadAllMissedMessagesSinceLastDisconnectForCurrentChat(chat: AbstractChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {

            val accountJid = chat.account
            val contactJid = chat.contactJid

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java)
                .forEachOnUi { it.onLastHistoryLoadStarted(accountJid, contactJid) }

            val accountItem = AccountManager.getInstance().getAccount(accountJid)
            val connection = accountItem?.connection ?: return@runInBackgroundNetworkUserRequest
            val listener = createNewRegularMamResultListener(accountJid)
            connection.addAsyncStanzaListener(listener, stanzaFilter)

            isArchiveFetching = true

            connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqAllMessagesSince(
                    chat = chat,
                    timestamp = getLastChatMessageInRealmTimestamp(chat)?.let { Date(it + 1) }
                        ?: Date(),
                ),
                {
                    isArchiveFetching = false
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
                        .forEachOnUi { it.onLastHistoryLoadFinished(accountJid, contactJid) }
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java).forEach {
                            it.onHistoryLoaded(accountItem)
                        }
                    }
                    connection.removeAsyncStanzaListener(listener)
                },
                { exception ->
                    isArchiveFetching = false
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadErrorListener::class.java)
                        .forEachOnUi { it.onLastHistoryLoadingError(accountJid, contactJid) }
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java).forEach {
                            it.onHistoryLoaded(accountItem)
                        }
                    }
                    connection.removeAsyncStanzaListener(listener)
                    LogManager.exception(this, exception)
                }
            )
        }
    }

    private fun getFirstChatMessageInRealmStanzaId(chat: AbstractChat): String? {
        val pinnedIdOrEmpty: String = (chat as? GroupChat)?.pinnedMessageId ?: ""
        var result: String? = ""
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .beginGroup()
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .notEqualTo(MessageRealmObject.Fields.STANZA_ID, pinnedIdOrEmpty)
                .endGroup()
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
                .firstOrNull()
                ?.stanzaId
        } catch (e: Exception) {
            LogManager.exception(this, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) {
                realm.close()
            }
        }
        return result
    }

    private fun getLastChatMessageInRealmTimestamp(chat: AbstractChat): Long? {
        var result: Long? = 0
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                .firstOrNull()
                ?.timestamp
        } catch (e: Exception) {
            LogManager.exception(this, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
        return result
    }

    private fun getLastAccountMessageInRealmTimestamp(accountJid: AccountJid): Long? {
        var result: Long? = 0
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                .firstOrNull()
                ?.timestamp
        } catch (e: Exception) {
            LogManager.exception(this, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) {
                realm.close()
            }
        }
        return result
    }

//    suspend fun tryToLoadPortionOfMemberMessagesInGroup(
//        chat: GroupChat,
//        memberId: String,
//        lastMessageStanzaId: String? = null,
//        messagesCount: Int = 50,
//    ): List<MessageRealmObject>? {
//        if (chat in activeRequests) {
//            return null
//        }
//
//        val accountItem = AccountManager.getInstance().getAccount(chat.account)
//
//        if (accountItem == null
//            || accountItem.loadHistorySettings == LoadHistorySettings.none
//            || !isSupported(accountItem)
//        ) {
//            LogManager.w(this, "Aborted next portion of member messages fetching!")
//            return null
//        }
//
//        activeRequests += chat
//
//        LogManager.i(
//            this,
//            "Start fetching portion of messages in group ${chat.contactJid} with member $memberId"
//        )
//
//        Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java)
//            .forEachOnUi { listener ->
//                listener.onLastHistoryLoadStarted(chat.account, chat.contactJid)
//            }
//
//        accountItem.connection.sendIqWithResponseCallback(
//            MamQueryIQ.createMamRequestIqMessagesAfterInChat(
//                chat, getFirstChatMessageInRealmStanzaId(chat) ?: ""
//            ),
//            { packet: Stanza? ->
//                if (packet is IQ && packet.type == IQ.Type.result) {
//                    Application.getInstance()
//                        .getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
//                        .forEachOnUi { listener ->
//                            listener.onLastHistoryLoadFinished(chat.account, chat.contactJid)
//                        }
//                    LogManager.i(
//                        this,
//                        "Finish fetching next portion of messages in chat ${chat.account} with ${chat.contactJid}"
//                    )
//                }
//                activeRequests -= chat
//            },
//            { exception: Exception? ->
//                Application.getInstance()
//                    .getUIListeners(OnLastHistoryLoadErrorListener::class.java)
//                    .forEachOnUi { listener ->
//                        if (exception is XMPPException.XMPPErrorException) {
//                            listener.onLastHistoryLoadingError(
//                                chat.account, chat.contactJid, exception.xmppError.conditionText
//                            )
//                        } else listener.onLastHistoryLoadingError(chat.account, chat.contactJid)
//                    }
//                LogManager.exception(this, exception)
//                activeRequests -= chat
//            },
//            60000
//        )
//
//    }

    fun loadNextMessagesPortionInChat(chat: AbstractChat) {
        if (chat in activeRequests) {
            return
        }

        val accountItem = AccountManager.getInstance().getAccount(chat.account) ?: return
        val accountJid = chat.account
        val contactJid = chat.contactJid
        val connection = accountItem.connection
        val listener = createNewRegularMamResultListener(accountJid)
        connection.addAsyncStanzaListener(listener, stanzaFilter)

        if (accountItem.loadHistorySettings == LoadHistorySettings.none || !isSupported(accountItem)
        ) {
            LogManager.w(this, "Aborted next portion of messages fetching!")
            return
        }

        activeRequests += chat

        Application.getInstance().runInBackgroundNetworkUserRequest {

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java)
                .forEachOnUi { it.onLastHistoryLoadStarted(accountJid, contactJid) }

            connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqMessagesAfterInChat(
                    chat, getFirstChatMessageInRealmStanzaId(chat) ?: ""
                ),
                { packet: Stanza? ->
                    if (packet is IQ && packet.type == IQ.Type.result) {
                        Application.getInstance()
                            .getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
                            .forEachOnUi { it.onLastHistoryLoadFinished(accountJid, contactJid) }
                    }
                    connection.removeAsyncStanzaListener(listener)
                    activeRequests -= chat
                },
                { exception: Exception? ->
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadErrorListener::class.java)
                        .forEachOnUi { listener ->
                            if (exception is XMPPException.XMPPErrorException) {
                                listener.onLastHistoryLoadingError(
                                    accountJid, contactJid, exception.xmppError.conditionText
                                )
                            } else {
                                listener.onLastHistoryLoadingError(accountJid, contactJid)
                            }
                        }
                    LogManager.exception(this, exception)
                    activeRequests -= chat
                    connection.removeAsyncStanzaListener(listener)
                },
                60000
            )
        }
    }

}