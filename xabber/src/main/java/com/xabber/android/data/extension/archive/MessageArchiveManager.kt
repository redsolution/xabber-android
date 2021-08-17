package com.xabber.android.data.extension.archive

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
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
import io.realm.Realm
import io.realm.Sort
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import java.util.*

object MessageArchiveManager : OnRosterReceivedListener, OnPacketListener {

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

    override fun onStanza(connection: ConnectionItem, packet: Stanza) {
        val accountJid = connection.account
        if (packet !is Message
            && !packet.hasExtension(MamResultExtensionElement.ELEMENT, NAMESPACE)
        ) {
            return
        }

        packet.extensions.filterIsInstance<MamResultExtensionElement>()
            .forEach { mamResultElement ->
                val forwardedElement = mamResultElement.forwarded.forwardedStanza
                val contactJid =
                    if (forwardedElement.from.asBareJid() == accountJid.fullJid.asBareJid()) {
                        ContactJid.from(forwardedElement.to.asBareJid().toString())
                    } else {
                        ContactJid.from(forwardedElement.from.asBareJid().toString())
                    }
                val delayInformation = mamResultElement.forwarded.delayInformation

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

    fun reInitMessagesForChat(accountJid: AccountJid, contactJid: ContactJid) {
        //todo
    }

    fun isSupported(accountItem: AccountItem) = try {
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
            LogManager.i(
                this,
                "Start fetching single message with stanza id $stanzaId"
            )

            AccountManager.getInstance().getAccount(chat.account)?.connection
                ?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqMessageWithStanzaId(chat, stanzaId),
                    { packet ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            LogManager.i(
                                this,
                                "Finish fetching single message with stanza id $stanzaId"
                            )
                        }
                    },
                    { exception ->
                        LogManager.exception(this, exception)
                    }
                )
        }
    }

    fun loadAllMessagesInChat(chat: AbstractChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            LogManager.i(
                this,
                "Start fetching all messages in chat ${chat.account} with ${chat.contactJid}"
            )
            AccountManager.getInstance().getAccount(chat.account)?.connection
                ?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqAllMessagesInChat(chat),
                    { packet ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            LogManager.i(
                                this,
                                "Finish fetching all messages in chat ${chat.account} with ${chat.contactJid}"
                            )
                        }
                    },
                    { exception ->
                        LogManager.exception(this, exception)
                    }
                )
        }
    }

    private fun loadLastSavedMessage(accountItem: AccountItem) {
        Application.getInstance().runInBackground {
            accountItem.connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqLastSavedMessage(accountItem.account),
                {
                    LogManager.d(
                        this,
                        "Finish loading last saved message of account ${accountItem.account}"
                    )
                    if (RosterManager.getInstance().getAccountRosterContacts(accountItem.account)
                            .isEmpty()
                    ) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                            .map { it.onHistoryLoaded(accountItem) }
                    }
                },
                {
                    LogManager.d(
                        this,
                        "Error loading last saved message of account ${accountItem.account}"
                    )
                    if (RosterManager.getInstance().getAccountRosterContacts(accountItem.account)
                            .isEmpty()
                    ) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                            .map { it.onHistoryLoaded(accountItem) }
                    }
                }
            )
        }
    }

    private fun loadLastMessagesInAllChats(accountItem: AccountItem) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val contacts = RosterManager.getInstance().getAccountRosterContacts(accountItem.account)
            contacts.mapIndexed { index, rosterContact ->
                val chat = ChatManager.getInstance()
                    .getChat(rosterContact.account, rosterContact.contactJid)
                    ?: ChatManager.getInstance()
                        .createRegularChat(rosterContact.account, rosterContact.contactJid)

                LogManager.d(
                    this,
                    "Start loading last message in chat ${chat.account} with ${chat.contactJid}"
                )
                AccountManager.getInstance().getAccount(chat.account)?.connection
                    ?.sendIqWithResponseCallback(
                        MamQueryIQ.createMamRequestIqLastMessageInChat(chat),
                        {
                            LogManager.d(
                                this,
                                "Finish loading last message in chat ${chat.account} with ${chat.contactJid}"
                            )
                            if (index == contacts.size - 1) {
                                Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                                    .map { it.onHistoryLoaded(accountItem) }
                            }
                        },
                        { exception ->
                            LogManager.d(
                                this,
                                "Error while loading last message in chat ${chat.account} with ${chat.contactJid}"
                            )
                            LogManager.exception(this, exception)
                            if (index == contacts.size - 1) {
                                Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                                    .map { it.onHistoryLoaded(accountItem) }
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
            LogManager.d(this, "Start loading last message in chat $accountJid with $contactJid")
            AccountManager.getInstance().getAccount(accountJid)?.connection
                ?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqLastMessageInChat(chat),
                    {
                        LogManager.d(
                            this,
                            "Finish loading last message in chat $accountJid with $contactJid"
                        )
                    },
                    { exception ->
                        LogManager.d(
                            this,
                            "Error while loading last message in chat $accountJid with $contactJid"
                        )
                        LogManager.exception(this, exception)
                    }
                )
        }
    }

    fun loadAllMissedMessagedSinceLastReconnectFromOwnArchiveForWholeAccount(accountItem: AccountItem) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            LogManager.i(
                this,
                "Start fetching whole missed messages for account ${accountItem.account}"
            )
            isArchiveFetching = true

            accountItem.connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqAllMessagesSince(
                    timestamp = getLastAccountMessageInRealmTimestamp(accountItem.account)
                        ?.let { Date(it + 1) } ?: Date()
                ),
                { packet ->
                    if (packet is MamFinIQ && packet.isComplete ?: true) {
                        LogManager.i(
                            this,
                            "Finish fetching whole missed messages for account ${accountItem.account}"
                        )
                        if (groupsQueryToRequestArchive.size == 0) {
                            Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                                .forEach { it.onHistoryLoaded(accountItem) }
                            isArchiveFetching = false
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
                    LogManager.exception(this, exception)
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                            .forEach { it.onHistoryLoaded(accountItem) }
                        isArchiveFetching = false
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

            LogManager.i(
                this,
                "Start fetching missed messages in chat $accountJid with $contactJid"
            )

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java)
                .forEachOnUi { it.onLastHistoryLoadStarted(accountJid, contactJid) }

            val accountItem = AccountManager.getInstance().getAccount(accountJid)

            accountItem?.connection?.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqAllMessagesSince(
                    chat = chat,
                    timestamp = getLastChatMessageInRealmTimestamp(chat)?.let { Date(it + 1) }
                        ?: Date(),
                ),
                {
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
                        .forEachOnUi { it.onLastHistoryLoadFinished(accountJid, contactJid) }
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                            .forEach { listener ->
                                listener.onHistoryLoaded(accountItem)
                            }
                        isArchiveFetching = false
                    }
                    LogManager.i(
                        this,
                        "Finish fetching missed messages in chat $accountJid with $contactJid"
                    )
                },
                { exception ->
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadErrorListener::class.java)
                        .forEachOnUi { it.onLastHistoryLoadingError(accountJid, contactJid) }
                    if (groupsQueryToRequestArchive.size == 0) {
                        Application.getInstance().getManagers(OnHistoryLoaded::class.java)
                            .forEach { listener -> listener.onHistoryLoaded(accountItem) }
                        isArchiveFetching = false
                    }
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

    fun loadNextMessagesPortionInChat(chat: AbstractChat) {
        if (chat in activeRequests) {
            LogManager.i(this, "already fetching, aborting")
            return
        }

        activeRequests += chat
        LogManager.i(this, "added to fetched, size ${activeRequests.size}")

        Application.getInstance().runInBackgroundNetworkUserRequest {

            LogManager.i(
                this,
                "Start fetching next portion of messages in chat ${chat.account} with ${chat.contactJid}"
            )

            val accountItem = AccountManager.getInstance().getAccount(chat.account)
            if (accountItem == null
                || accountItem.loadHistorySettings == LoadHistorySettings.none
                || !isSupported(accountItem)
            ) {
                return@runInBackgroundNetworkUserRequest
            }

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java)
                .forEachOnUi { listener ->
                    listener.onLastHistoryLoadStarted(chat.account, chat.contactJid)
                }

            accountItem.connection.sendIqWithResponseCallback(
                MamQueryIQ.createMamRequestIqMessagesAfterInChat(
                    chat, getFirstChatMessageInRealmStanzaId(chat) ?: ""
                ),
                { packet: Stanza? ->
                    if (packet is IQ && packet.type == IQ.Type.result) {
                        Application.getInstance()
                            .getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
                            .forEachOnUi { listener ->
                                listener.onLastHistoryLoadFinished(chat.account, chat.contactJid)
                            }
                        LogManager.i(
                            this,
                            "Finish fetching next portion of messages in chat ${chat.account} with ${chat.contactJid}"
                        )
                    }
                    activeRequests -= chat
                },
                { exception: Exception? ->
                    Application.getInstance()
                        .getUIListeners(OnLastHistoryLoadErrorListener::class.java)
                        .forEachOnUi { listener ->
                            if (exception is XMPPException.XMPPErrorException) {
                                listener.onLastHistoryLoadingError(
                                    chat.account, chat.contactJid, exception.xmppError.conditionText
                                )
                            } else listener.onLastHistoryLoadingError(chat.account, chat.contactJid)
                        }
                    LogManager.exception(this, exception)
                    activeRequests -= chat
                },
                60000
            )
        }
    }

}