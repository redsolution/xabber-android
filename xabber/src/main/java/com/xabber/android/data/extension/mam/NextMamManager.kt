package com.xabber.android.data.extension.mam

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.groups.GroupInviteManager.processIncomingInvite
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.references.ReferencesManager
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.ForwardManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.notification.NotificationManager
import com.xabber.android.data.push.SyncManager
import com.xabber.android.data.roster.OnRosterReceivedListener
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.roster.RosterContact
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.OnLastHistoryLoadFinishedListener
import com.xabber.android.ui.OnLastHistoryLoadStartedListener
import com.xabber.android.ui.OnNewMessageListener
import com.xabber.android.utils.StringUtils
import com.xabber.xmpp.groups.hasGroupExtensionElement
import com.xabber.xmpp.groups.hasGroupSystemMessage
import com.xabber.xmpp.groups.invite.incoming.getIncomingInviteExtension
import com.xabber.xmpp.groups.invite.incoming.hasIncomingInviteExtension
import com.xabber.xmpp.sid.UniqueIdsHelper
import io.realm.Realm
import io.realm.Sort
import net.java.otr4j.io.SerializationUtils
import net.java.otr4j.io.messages.AbstractMessage
import net.java.otr4j.io.messages.PlainTextMessage
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.PacketParserUtils
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jivesoftware.smackx.mam.MamManager
import org.jivesoftware.smackx.mam.MamManager.MamQueryResult
import org.jivesoftware.smackx.mam.element.MamElements
import org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension
import org.jivesoftware.smackx.mam.element.MamFinIQ
import org.jivesoftware.smackx.mam.element.MamQueryIQ
import org.jivesoftware.smackx.rsm.packet.RSMSet
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.Jid
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object NextMamManager : OnRosterReceivedListener, OnPacketListener {

    private val LOG_TAG = NextMamManager::class.java.simpleName
    const val NAMESPACE = "urn:xmpp:mam:tmp"
    private const val DATA_FORM_FIELD_STANZA_ID = "{urn:xmpp:sid:0}stanza-id"
    private const val DATA_FORM_FIELD_WITH = "with"

    private val supportedByAccount: MutableMap<AccountJid, Boolean> = ConcurrentHashMap()
    private var isRequested = false
    private val lock = Any()
    private val waitingRequests: MutableMap<String, ContactJid> = HashMap()
    private val rosterItemIterators: MutableMap<AccountItem?, Iterator<RosterContact>?> = ConcurrentHashMap()

    override fun onRosterReceived(accountItem: AccountItem) {
        LogManager.d(LOG_TAG, "onRosterReceivedStarted")
        updateIsSupported(accountItem)

        val realm = DatabaseManager.getInstance().defaultRealmInstance
        if (accountItem.startHistoryTimestamp == 0L) {
            initializeStartTimestamp(accountItem)
            loadMostRecentMessages(accountItem)
            startLoadingLastMessageInAllChats(accountItem)
        } else {
            val lastArchivedId = getLastMessageArchivedId(accountItem, realm)
            if (lastArchivedId != null) {
                val historyCompleted = loadAllNewMessages(accountItem, lastArchivedId)
                if (!historyCompleted) {
                    startLoadingLastMessageInAllChats(accountItem)
                } else startLoadingLastMessageInMissedChats(accountItem)
            } else startLoadingLastMessageInAllChats(accountItem)
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
    }

    fun onChatOpen(chat: AbstractChat) {
        val accountItem = AccountManager.getInstance().getAccount(chat.account)
        if (accountItem == null
                || accountItem.loadHistorySettings == LoadHistorySettings.none
                || !isSupported(accountItem.account)) {
                    return
        }
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val realm = DatabaseManager.getInstance().defaultRealmInstance

            // if history is empty - load last message
            if (hasMessage(chat)) loadLastMessage(accountItem, chat)
            synchronized(lock) {
                isRequested = if (isRequested) return@runInBackgroundNetworkUserRequest else true
            }

            // load prev page if history is not enough
            if (historyIsNotEnough(chat) && !chat.historyIsFull()) {

                Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java).forEach {
                    listener -> listener.onLastHistoryLoadStarted(chat.account, chat.contactJid)
                }

                loadNextHistory(realm, accountItem, chat)

                Application.getInstance().getUIListeners(OnLastHistoryLoadFinishedListener::class.java).forEach {
                    listener -> listener.onLastHistoryLoadFinished(chat.account, chat.contactJid)
                }
            }

            // load missed messages if need
            val messages = findMissedMessages(realm, chat)
            if (messages != null && messages.isNotEmpty()) {
                messages.forEach { message -> loadMissedMessages(realm, accountItem, chat, message) }
            }
            synchronized(lock) { isRequested = false }
            realm.close()
        }
    }

    fun onScrollInChat(chat: AbstractChat) {
        val accountItem = AccountManager.getInstance().getAccount(chat.account)
        if (accountItem == null
                || accountItem.loadHistorySettings == LoadHistorySettings.none
                || !isSupported(accountItem.account)) {
                    return
        }

        if (chat.historyIsFull()) return

        Application.getInstance().runInBackgroundNetworkUserRequest {

            synchronized(lock) {
                isRequested = if (isRequested) {
                    return@runInBackgroundNetworkUserRequest
                } else true
            }

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java).forEach {
                listener -> listener.onLastHistoryLoadStarted(chat.account, chat.contactJid)
            }

            val realm = DatabaseManager.getInstance().defaultRealmInstance

            loadNextHistory(realm, accountItem, chat)

            if (Looper.myLooper() != Looper.getMainLooper()) realm.close()

            Application.getInstance().getUIListeners(OnLastHistoryLoadFinishedListener::class.java).forEach {
                listener -> listener.onLastHistoryLoadFinished(chat.account, chat.contactJid)
            }

            synchronized(lock) { isRequested = false }
        }
    }

    fun loadFullChatHistory(chat: AbstractChat) {
        val accountItem = AccountManager.getInstance().getAccount(chat.account)

        if (accountItem == null || !isSupported(accountItem.account) || chat.historyIsFull()) return

        val realm = DatabaseManager.getInstance().defaultRealmInstance

        if (hasMessage(chat)) loadLastMessage(accountItem, chat)

        var complete = false
        while (!complete) {
            complete = loadNextHistory(realm, accountItem, chat)
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
    }

    override fun onStanza(connection: ConnectionItem,
                          packet: Stanza,
    ) {
        when (packet){
            is Message -> {
                packet.getExtensions()
                        .filterIsInstance<MamResultExtension>()
                        .forEach { packetExtension ->
                            val resultID = packetExtension.queryId
                            if (waitingRequests.containsKey(resultID)) {
                                val forwardedStanza = packetExtension.forwarded.forwardedStanza
                                if (forwardedStanza.hasIncomingInviteExtension()) {
                                    try {
                                        val inviteElement = forwardedStanza.getIncomingInviteExtension()
                                        var timestamp: Long = 0
                                        if (forwardedStanza.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                                            val timeElement = forwardedStanza.getExtension<TimeElement>(TimeElement.ELEMENT,
                                                    TimeElement.NAMESPACE)
                                            timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.stamp).time
                                        }
                                        processIncomingInvite(inviteElement!!, connection.account,
                                                ContactJid.from(forwardedStanza.from), timestamp)
                                    } catch (e: Exception) {
                                        LogManager.exception(LOG_TAG, e)
                                    }
                                    return
                                }
                                val realm = DatabaseManager.getInstance().defaultRealmInstance
                                parseAndSaveMessageFromMamResult(connection.account, packetExtension.forwarded)
                                val contactJid = waitingRequests[resultID]
                                val chat = ChatManager.getInstance().getChat(connection.account, contactJid)
                                if (chat != null && !chat.isHistoryRequestedAtStart) chat.setHistoryRequestedAtStart()
                                waitingRequests.remove(resultID)
                                loadNextLastMessageAsync(connection.account)
                                if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
                            }
                        }
            }

            is MamFinIQ -> {
                if (packet.isComplete && waitingRequests.containsKey(packet.queryId)) {
                    val contactJid = waitingRequests[packet.queryId]
                    val chat = ChatManager.getInstance().getChat(connection.account, contactJid)

                    if (chat != null && !chat.isHistoryRequestedAtStart) chat.setHistoryRequestedAtStart()

                    waitingRequests.remove(packet.queryId)
                    loadNextLastMessageAsync(connection.account)
                }
            }

            is MamQueryIQ -> {
                if (packet.error != null && waitingRequests.containsKey(packet.queryId)) {
                    waitingRequests.remove(packet.queryId)
                    loadNextLastMessageAsync(connection.account)
                }
            }

        }
    }

    fun isSupported(accountJid: AccountJid): Boolean {
        val isSupported = supportedByAccount[accountJid]
        return isSupported ?: false
    }

    fun resetContactHistoryIterator(accountJid: AccountJid?) {
        val accountItem = AccountManager.getInstance().getAccount(accountJid)
        rosterItemIterators.remove(accountItem)
    }

    /**
     * Start the process of loading last messages one by one in chats
     * without last message and with history not requested.
     *
     *
     * This is needed just in case the initial last message
     * loading was interrupted previously.
     */
    private fun startLoadingLastMessageInMissedChats(accountItem: AccountItem) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return

        val contactsWithoutHistory: MutableCollection<RosterContact> = ArrayList()

        RosterManager.getInstance().getAccountRosterContacts(accountItem.account).forEach { contact ->
            var chat = ChatManager.getInstance().getChat(contact.account, contact.contactJid)

            if (chat == null) chat = ChatManager.getInstance().createRegularChat(contact.account, contact.contactJid)

            if (!hasMessage(chat!!) && !chat.isHistoryRequestedAtStart) {
                contactsWithoutHistory.add(contact)
            }
        }

        if (rosterItemIterators[accountItem] == null) {
            rosterItemIterators[accountItem] = contactsWithoutHistory.iterator()
        }

        loadNextLastMessageAsync(accountItem)
    }

    /**
     * Start the process of loading last messages one by one for all contacts.
     */
    private fun startLoadingLastMessageInAllChats(accountItem: AccountItem) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return

        if (rosterItemIterators[accountItem] == null) {
            val contacts = RosterManager.getInstance().getAccountRosterContacts(accountItem.account)
            rosterItemIterators[accountItem] = contacts.iterator()
        }

        loadNextLastMessageAsync(accountItem)
    }

    private fun loadNextLastMessageAsync(accountJid: AccountJid) {
        val accountItem = AccountManager.getInstance().getAccount(accountJid)
        accountItem?.let { loadNextLastMessageAsync(it) }
    }

    private fun loadNextLastMessageAsync(accountItem: AccountItem) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return

        val iterator = rosterItemIterators[accountItem]

        if (iterator != null) {
            if (iterator.hasNext()) {
                val contact = iterator.next()
                LogManager.d(LOG_TAG, "load last message in $contact chat")
                var chat = ChatManager.getInstance().getChat(contact.account, contact.contactJid)
                if (chat == null) chat = ChatManager.getInstance().createRegularChat(contact.account, contact.contactJid)

                requestLastMessageAsync(accountItem, chat!!)
            } else {
                LogManager.d(LOG_TAG, "finished loading first messages of " + accountItem.account)
                VCardManager.getInstance().onHistoryLoaded(accountItem)
                PresenceManager.getInstance().onHistoryLoaded(accountItem)
                rosterItemIterators.remove(accountItem)
            }
        }
    }

    private fun loadLastMessage(accountItem: AccountItem,
                                chat: AbstractChat,
    ) {
        LogManager.d(LOG_TAG, "load last messages in chat: " + chat.contactJid)
        val queryResult = requestLastMessage(accountItem, chat)
        if (queryResult != null) {
            val messages: List<Forwarded> = ArrayList(queryResult.forwardedMessages)
            saveOrUpdateMessages(parseMessage(accountItem, chat.account, chat.contactJid, messages))
        }
        updateLastMessageIdInChat(chat)
    }

    private fun loadMostRecentMessages(accountItem: AccountItem,
    ) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return

        LogManager.d(LOG_TAG, "load new messages")
        val messages: MutableList<Forwarded> = ArrayList()
        val queryResult = requestRecentMessages(accountItem)
        if (queryResult != null) messages.addAll(queryResult.forwardedMessages)
        if (messages.isNotEmpty()) {
            val parsedMessages: MutableList<MessageRealmObject> = ArrayList()
            val chatsNeedUpdateLastMessageId: MutableList<AbstractChat> = ArrayList()
            messages.groupToChats(accountItem).forEach { (jid, messagesList) ->
                try {
                    val chat = ChatManager.getInstance().getChat(accountItem.account, ContactJid.from(jid))
                            ?: ChatManager.getInstance().createRegularChat(accountItem.account, ContactJid.from(jid))

                    val oldSize = parsedMessages.size
                    parsedMessages.addAll(parseNewMessagesInChat(ArrayList(messagesList), chat, accountItem))

                    if (parsedMessages.size - oldSize > 0) chatsNeedUpdateLastMessageId.add(chat)
                } catch (e: ContactJidCreateException) {
                    LogManager.d(LOG_TAG, e.toString())
                }
            }
            saveOrUpdateMessages(parsedMessages)
            chatsNeedUpdateLastMessageId.forEach { chat ->
                run {
                    updateLastMessageIdInChat(chat)
                    chat.setHistoryRequestedWithoutRealm(true)
                    ChatManager.getInstance().saveOrUpdateChatDataToRealm(chat)
                }
            }
        }
    }

    private fun loadAllNewMessages(accountItem: AccountItem,
                                   lastArchivedId: String,
    ): Boolean {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return true

        LogManager.d(LOG_TAG, "load new messages")
        val messages: MutableList<Forwarded> = ArrayList()
        var complete = false
        var id: String? = lastArchivedId
        var pageLoaded = 0
        // Request all new messages after last archived id
        while (!complete && id != null && pageLoaded < 2) {
            val queryResult = requestMessagesFromId(accountItem, id)
            if (queryResult != null) {
                messages.addAll(queryResult.forwardedMessages)
                complete = queryResult.mamFin.isComplete
                id = getNextArchivedId(queryResult)
                pageLoaded++
            } else complete = true
        }
        if (messages.isNotEmpty()) {
            val parsedMessages: MutableList<MessageRealmObject> = ArrayList()
            val chatsNeedUpdateLastMessageId: MutableList<AbstractChat> = ArrayList()
            
            // parse message lists
            messages.groupToChats(accountItem).forEach { (jid, messagesList) ->
                try {
                    val chat = ChatManager.getInstance().getChat(accountItem.account, ContactJid.from(jid))
                            ?: ChatManager.getInstance().createRegularChat(accountItem.account, ContactJid.from(jid))

                    parsedMessages.addAll(parseNewMessagesInChat(ArrayList(messagesList), chat, accountItem))
                    if (parsedMessages.size - parsedMessages.size > 0) chatsNeedUpdateLastMessageId.add(chat)
                } catch (e: ContactJidCreateException) {
                    LogManager.d(LOG_TAG, e.toString())
                }
            }

            // save messages to Realm
            saveOrUpdateMessages(parsedMessages)
            chatsNeedUpdateLastMessageId.forEach { chat -> updateLastMessageIdInChat(chat) }
        }
        return complete
    }

    private fun parseNewMessagesInChat(chatMessages: ArrayList<Forwarded>,
                                       chat: AbstractChat?,
                                       accountItem: AccountItem,
    ): List<MessageRealmObject> {
        chatMessages.sortWith{ f1, f2 -> f1.delayInformation.stamp.time.compareTo(f2.delayInformation.stamp.time) }
        return ArrayList(parseMessage(accountItem, accountItem.account, chat!!.contactJid,
                chatMessages))
    }

    private fun loadNextHistory(realm: Realm,
                                accountItem: AccountItem,
                                chat: AbstractChat,
    ): Boolean {
        LogManager.d(LOG_TAG, "load next history in chat: " + chat.contactJid)
        if (hasMessage(chat)) {
            val queryResult = requestMessagesBeforeId(accountItem, chat, getFirstMessageId(chat) ?: "")
            if (queryResult != null) {
                val messages: List<Forwarded> = ArrayList(queryResult.forwardedMessages)
                if (messages.isNotEmpty()) {
                    saveOrUpdateMessages(parseMessage(accountItem, chat.account, chat.contactJid, messages))
                } else if (queryResult.mamFin.isComplete) {
                    realm.beginTransaction()
                    realm.commitTransaction()
                }
            }
        }
        return true
    }

    private fun loadMissedMessages(realm: Realm,
                                   accountItem: AccountItem,
                                   chat: AbstractChat,
                                   m1: MessageRealmObject,
    ) {
        LogManager.d(LOG_TAG, "load missed messages in chat: " + chat.contactJid)
        val m2 = getMessageForCloseMissedMessages(realm, m1)
        if (m2 != null && m2.stanzaId != m1.stanzaId) {
            var startDate: Date? = Date(m2.timestamp)
            val endDate = Date(m1.timestamp)
            val messages: MutableList<Forwarded> = ArrayList()
            var complete = false
            while (!complete && startDate != null) {
                val queryResult = requestMissedMessages(accountItem, chat, startDate, endDate)
                if (queryResult != null) {
                    messages.addAll(queryResult.forwardedMessages)
                    complete = queryResult.mamFin.isComplete
                    startDate = getNextDate(queryResult)
                } else complete = true
            }
            if (messages.isNotEmpty()) {
                saveOrUpdateMessages(parseMessage(accountItem, chat.account, chat.contactJid, messages))
            } else {
                realm.beginTransaction()
                realm.commitTransaction()
            }
        }
    }

    /** Request most recent message from all history and save it timestamp to startHistoryTimestamp
     * If message is null save current time to startHistoryTimestamp  */
    private fun initializeStartTimestamp(accountItem: AccountItem) {
        var startHistoryTimestamp = System.currentTimeMillis()
        val queryResult = requestLastMessage(accountItem, null)
        if (queryResult != null && queryResult.forwardedMessages.isNotEmpty()) {
            val forwarded = queryResult.forwardedMessages[0]
            startHistoryTimestamp = forwarded.delayInformation.stamp.time
            val forwardedStanza = forwarded.forwardedStanza
            if (forwardedStanza.hasIncomingInviteExtension()) {
                try {
                    val inviteElement = forwardedStanza.getIncomingInviteExtension()
                    var timestamp: Long = 0
                    if (forwardedStanza.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                        val timeElement =
                                forwardedStanza.getExtension<TimeElement>(TimeElement.ELEMENT, TimeElement.NAMESPACE)
                        timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.stamp).time
                    }
                    processIncomingInvite(
                            inviteElement!!, accountItem.account, ContactJid.from(forwardedStanza.from), timestamp)
                } catch (e: Exception) {
                    LogManager.exception(LOG_TAG, e)
                }
                accountItem.startHistoryTimestamp = startHistoryTimestamp
                return
            }
            parseAndSaveMessageFromMamResult(accountItem.account, forwarded)
        }
        accountItem.startHistoryTimestamp = startHistoryTimestamp
    }

    private fun updateIsSupported(accountItem: AccountItem) {
        val isSupported: Boolean = try {
            ServiceDiscoveryManager
                    .getInstanceFor(accountItem.connection)
                    .supportsFeature(accountItem.connection.user.asBareJid(), MamElements.NAMESPACE)
        } catch (e: Exception) {
            LogManager.exception(LOG_TAG, e)
            false
        }
        supportedByAccount[accountItem.account] = isSupported
        AccountManager.getInstance().onAccountChanged(accountItem.account)
        if (!isSupported) VCardManager.getInstance().onHistoryLoaded(accountItem)
    }


    internal abstract class MamRequest<T> {
        abstract fun execute(manager: MamManager): T
    }

    private fun <T> requestToMessageArchive(accountItem: ConnectionItem,
                                            request: MamRequest<T>,
    ): T? {
        var result: T? = null
        val connection = accountItem.connection
        if (connection.isAuthenticated) {
            val mamManager = MamManager.getInstanceFor(connection)
            try {
                result = request.execute(mamManager)
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
            }
        }
        return result
    }

    /** Request recent message from chat history if chat not null
     * Else request most recent message from all history */
    private fun requestLastMessage(accountItem: AccountItem,
                                   chat: AbstractChat?,
    ): MamQueryResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult>() {
            override fun execute(manager: MamManager): MamQueryResult {
                return if (chat != null) {
                    manager.mostRecentPage(chat.contactJid.jid, 1)
                } else manager.mostRecentPage(null, 1)
            }
        })
    }

    private fun requestRecentMessages(accountItem: AccountItem,
    ): MamQueryResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult>() {
            override fun execute(manager: MamManager): MamQueryResult {
                return manager.mostRecentPage(null, 50)
            }
        })
    }

    /** Send async request for recent message from chat history  */
    private fun requestLastMessageAsync(accountItem: ConnectionItem,
                                        chat: AbstractChat,
    ) {
        requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult?>() {
            override fun execute(manager: MamManager): MamQueryResult? {
                // add request id to waiting list
                val queryID = UUID.randomUUID().toString()
                waitingRequests[queryID] = chat.contactJid

                // send request stanza
                val rsmSet = RSMSet(null, "", -1, -1, null, 1, null, -1)
                val dataForm = createNewMamDataForm()

                val formField = FormField(DATA_FORM_FIELD_WITH)
                formField.addValue(chat.contactJid.jid.toString())
                dataForm.addField(formField)

                val mamQueryIQ = MamQueryIQ(queryID, null, dataForm)
                mamQueryIQ.type = IQ.Type.set
                if (chat is GroupChat) {
                    mamQueryIQ.to = chat.getContactJid().bareJid
                } else mamQueryIQ.to = null
                mamQueryIQ.addExtension(rsmSet)
                accountItem.connection.sendStanza(mamQueryIQ)
                return null
            }
        })
    }

    fun requestSingleMessageAsync(accountItem: ConnectionItem,
                                  chat: AbstractChat,
                                  stanzaId: String?,
    ) {
        requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult?>() {
            override fun execute(manager: MamManager): MamQueryResult? {
                // add request id to waiting list
                val queryID = UUID.randomUUID().toString()
                waitingRequests[queryID] = chat.contactJid

                // send request stanza
                val dataForm = createNewMamDataForm()
                if (stanzaId != null) {
                    val formField = FormField(DATA_FORM_FIELD_STANZA_ID)
                    formField.addValue(stanzaId)
                    dataForm.addField(formField)
                }
                val mamQueryIQ = MamQueryIQ(queryID, null, dataForm)
                mamQueryIQ.type = IQ.Type.set
                if (chat is GroupChat) {
                    mamQueryIQ.to = chat.getTo()
                } else mamQueryIQ.to = null
                accountItem.connection.sendStanza(mamQueryIQ)
                return null
            }
        })
    }

    /** Request messages after archivedID from chat history
     * Else request messages after archivedID from all history  */
    private fun requestMessagesFromId(accountItem: AccountItem,
                                      archivedId: String,
    ): MamQueryResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult>() {
            override fun execute(manager: MamManager): MamQueryResult {
                return manager.pageAfter(null, archivedId, 50)
            }
        })
    }

    /** Request messages before archivedID from chat history  */
    private fun requestMessagesBeforeId(accountItem: AccountItem,
                                        chat: AbstractChat,
                                        archivedId: String,
    ): MamQueryResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult>() {
            override fun execute(manager: MamManager): MamQueryResult {
                return manager.pageBefore(chat.contactJid.jid, archivedId, 50)
            }
        })
    }

    /** Request messages started with startID and ending with endID from chat history  */
    private fun requestMissedMessages(accountItem: AccountItem,
                                      chat: AbstractChat,
                                      startDate: Date,
                                      endDate: Date,
    ): MamQueryResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamQueryResult>() {
            override fun execute(manager: MamManager): MamQueryResult {
                return manager.queryArchive(50, startDate, endDate, chat.contactJid.jid, null)
            }
        })
    }


    /** PARSING  */
    private fun parseAndSaveMessageFromMamResult(account: AccountJid,
                                                 forwarded: Forwarded,
    ) {
        val stanza = forwarded.forwardedStanza
        val accountItem = AccountManager.getInstance().getAccount(account)
        var user: Jid = stanza.from.asBareJid()
        if (user.equals(account.fullJid.asBareJid())) user = stanza.to.asBareJid()
        try {
            var chat = ChatManager.getInstance().getChat(account, ContactJid.from(user))
            if (chat == null) chat = ChatManager.getInstance().createRegularChat(account, ContactJid.from(user))
            val messageRealmObject = parseMessage(accountItem, account, chat!!.contactJid, forwarded)
            if (messageRealmObject != null) {
                saveOrUpdateMessages(listOf(messageRealmObject), true)
                updateLastMessageIdInChat(chat)
            }
        } catch (e: ContactJidCreateException) {
            LogManager.d(LOG_TAG, e.toString())
        }
    }

    private fun parseMessage(accountItem: AccountItem,
                             account: AccountJid,
                             user: ContactJid,
                             forwardedMessages: List<Forwarded>,
    ): List<MessageRealmObject> {
        val messageRealmObjects: MutableList<MessageRealmObject> = ArrayList()
        var lastOutgoingId: String? = null
        for (forwarded in forwardedMessages) {
            val message = parseMessage(accountItem, account, user, forwarded)
            if (message != null) {
                messageRealmObjects.add(message)
                if (!message.isIncoming) lastOutgoingId = message.primaryKey
            }
        }

        // mark messages before outgoing as read
        if (lastOutgoingId != null) {
            for (message in messageRealmObjects) {
                if (lastOutgoingId == message.primaryKey) break
                message.isRead = true
            }
        }
        return messageRealmObjects
    }

    private fun parseMessage(accountItem: AccountItem?,
                             account: AccountJid,
                             user: ContactJid,
                             forwarded: Forwarded,
    ): MessageRealmObject? {
        if (forwarded.forwardedStanza !is Message) return null
        val message = forwarded.forwardedStanza as Message
        if (message.hasIncomingInviteExtension()) {
            try {
                val inviteElement = message.getIncomingInviteExtension()
                var timestamp: Long = 0
                if (message.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                    val timeElement = message.getExtension<TimeElement>(TimeElement.ELEMENT, TimeElement.NAMESPACE)
                    timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.stamp).time
                }
                processIncomingInvite(inviteElement!!, accountItem!!.account, ContactJid.from(message.from), timestamp)
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
            }
            return null
        }
        val delayInformation = forwarded.delayInformation
        val messageDelay = DelayInformation.from(message)
        var body = message.body
        val otrMessage: AbstractMessage? = try {
            SerializationUtils.toMessage(body)
        } catch (e: IOException) {
            LogManager.exception(LOG_TAG, e)
            return null
        }
        var encrypted = false
        if (otrMessage != null) {
            if (otrMessage.messageType != AbstractMessage.MESSAGE_PLAINTEXT) {
                encrypted = true
                try {
                    // this transforming just decrypt message if have keys. No action as injectMessage or something else
                    body = OTRManager.getInstance().transformReceivingIfSessionExist(account, user, body)
                    if (OTRManager.getInstance().isEncrypted(body)) return null
                } catch (e: Exception) {
                    LogManager.exception(LOG_TAG, e)
                    return null
                }
            } else body = (otrMessage as PlainTextMessage).cleanText
        }

        // forward comment (to support previous forwarded xep)
        val forwardComment = ForwardManager.parseForwardComment(message)
        if (forwardComment != null) body = forwardComment

        // modify body with references
        val bodies = ReferencesManager.modifyBodyWithReferences(message, body)
        body = bodies.first
        val markupBody = bodies.second
        val incoming = message.from.asBareJid().equals(user.jid.asBareJid())
        val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message)

        val stanzaId =
                if (groupchatUser != null || message.hasGroupSystemMessage()) {
            UniqueIdsHelper.getStanzaIdBy(message, user.bareJid.toString())
                } else UniqueIdsHelper.getStanzaIdBy(message, account.bareJid.toString())

        val originId: String? = UniqueIdsHelper.getOriginId(message)

        val messageRealmObject =
                if (originId != null) MessageRealmObject.createMessageRealmObjectWithOriginId(account, user, originId)
                else MessageRealmObject.createMessageRealmObjectWithStanzaId(account, user, stanzaId)

        val timestamp = delayInformation.stamp.time

        messageRealmObject.apply {
            resource = user.jid.resourceOrNull
            text = body
            isIncoming = incoming
            markupText = markupBody
            delayTimestamp = messageDelay?.stamp?.time
            isRead = timestamp <= accountItem!!.startHistoryTimestamp
            if (incoming) {
                messageRealmObject.messageStatus = MessageStatus.NONE
            } else messageRealmObject.messageStatus = MessageStatus.DISPLAYED
            this.timestamp = timestamp
            this.stanzaId = stanzaId
            this.originId = UniqueIdsHelper.getOriginId(message)
            isEncrypted = encrypted
        }

        // attachments
        // FileManager.processFileMessage(messageRealmObject);
        val attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message)
        if (attachmentRealmObjects.size > 0) messageRealmObject.attachmentRealmObjects = attachmentRealmObjects

        // forwarded
        messageRealmObject.originalStanza = message.toXML().toString()
        messageRealmObject.originalFrom = message.from.toString()

        // groupchat
        if (groupchatUser != null) {
            GroupMemberManager.getInstance().saveGroupUser(groupchatUser, user.bareJid, timestamp)
            messageRealmObject.groupchatUserId = groupchatUser.id
        } else if (message.hasGroupSystemMessage()) messageRealmObject.isGroupchatSystem = true

        return messageRealmObject
    }

    /** SAVING  */
    private fun saveOrUpdateMessages(messages: Collection<MessageRealmObject>?,
                                     ui: Boolean = false,
    ){
        fun determineSaveOrUpdate(realm: Realm, message: MessageRealmObject): MessageRealmObject? {

            var originalMessage: Message? = null
            try {
                originalMessage = PacketParserUtils.parseStanza(message.originalStanza)
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
            }
            if (originalMessage == null) LogManager.e(NextMamManager::class.java, message.originalStanza)
            val chat = ChatManager.getInstance().getChat(message.account, message.user) ?: return null
            val localMessage = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .equalTo(MessageRealmObject.Fields.TEXT, message.text)
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .beginGroup()
                    .equalTo(MessageRealmObject.Fields.ORIGIN_ID, message.originId)
                    .or()
                    .equalTo(MessageRealmObject.Fields.ORIGIN_ID, message.stanzaId)
                    .or()
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, message.originId)
                    .or()
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, message.stanzaId)
                    .endGroup()
                    .findFirst()

            return if (localMessage == null) {

                // forwarded
                if (originalMessage != null) {
                    val forwardIdRealmObjects = chat.parseForwardedMessage(ui, originalMessage, message.primaryKey)
                    if (forwardIdRealmObjects != null && !forwardIdRealmObjects.isEmpty()) {
                        message.forwardedIds = forwardIdRealmObjects
                    }
                }

                // notify about new message
                chat.enableNotificationsIfNeed()
                val notify = (!message.isRead
                        && message.text != null && message.text.trim { it <= ' ' }.isNotEmpty()
                        && message.isIncoming
                        && chat.notifyAboutMessage())
                val visible = ChatManager.getInstance().isVisibleChat(chat)
                if (notify && !visible) NotificationManager.getInstance().onMessageNotification(message)
                message
            } else {
                LogManager.d(LOG_TAG, "Matching message found! Updating message")
                realm.executeTransaction { localMessage.stanzaId = message.stanzaId }

                localMessage
            }
        }

        fun saveOrUpdate() {
            var realm : Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                val messagesToSave: MutableList<MessageRealmObject> = ArrayList()
                if (messages != null && !messages.isEmpty()) {
                    messages.forEach { message ->
                        val newMessage = determineSaveOrUpdate(realm, message)
                        if (newMessage != null) messagesToSave.add(newMessage)
                    }
                }
                realm.executeTransaction { realm1 ->
                    realm1.copyToRealmOrUpdate(messagesToSave)
                }
                SyncManager.getInstance().onMessageSaved()

                Application.getInstance().runOnUiThread {
                    Application.getInstance().getUIListeners(OnNewMessageListener::class.java)
                            .forEach(OnNewMessageListener::onNewMessage)
                }
            } catch (e: Exception){
                LogManager.exception(LOG_TAG, e)
            } finally {
                if (Looper.myLooper() != Looper.getMainLooper() && realm != null) realm.close()
            }
        }

        if (ui){
            Application.getInstance().runOnUiThread { saveOrUpdate() }
        } else Application.getInstance().runInBackground { saveOrUpdate() }

    }

    private fun getNextArchivedId(queryResult: MamQueryResult): String? {
        return if (queryResult.forwardedMessages != null && queryResult.forwardedMessages.isNotEmpty()) {
            val lastForwardedStanza = queryResult.forwardedMessages[queryResult.forwardedMessages.size - 1].forwardedStanza
            val to = queryResult.mamFin.to.asBareJid().toString()
            val from = lastForwardedStanza.from.asBareJid().toString()
            if (lastForwardedStanza.hasGroupExtensionElement() || lastForwardedStanza.hasGroupSystemMessage()) {
                UniqueIdsHelper.getArchivedIdBy(lastForwardedStanza, from)
            } else UniqueIdsHelper.getArchivedIdBy(lastForwardedStanza, to)
        } else null
    }

    private fun getNextDate(queryResult: MamQueryResult): Date? {
        var date: Date? = null
        if (queryResult.forwardedMessages != null && queryResult.forwardedMessages.isNotEmpty()) {
            val forwarded = queryResult.forwardedMessages[queryResult.forwardedMessages.size - 1]
            val delayInformation = forwarded.delayInformation
            date = Date(delayInformation.stamp.time + 1)
        }
        return date
    }

    private fun findMissedMessages(realm: Realm,
                                   chat: AbstractChat,
    ) = realm.where(MessageRealmObject::class.java)
            .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
            .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
            .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
            .isNotNull(MessageRealmObject.Fields.STANZA_ID)
            .findAll()
            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)

    private fun getMessageForCloseMissedMessages(realm: Realm,
                                                 messageRealmObject: MessageRealmObject,
    ) = realm.where(MessageRealmObject::class.java)
            .equalTo(MessageRealmObject.Fields.ACCOUNT, messageRealmObject.account.toString())
            .equalTo(MessageRealmObject.Fields.USER, messageRealmObject.user.toString())
            .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
            .isNotNull(MessageRealmObject.Fields.STANZA_ID)
            .lessThan(MessageRealmObject.Fields.TIMESTAMP, messageRealmObject.timestamp)
            .findAll()
            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
            .first()

    private fun historyIsNotEnough(chat: AbstractChat,
    ): Boolean {
        var result = false
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .findAll()
                    .size < 30
        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
        return result
    }

    private fun getLastMessageArchivedId(account: AccountItem,
                                         realm: Realm,
    ) = realm.where(MessageRealmObject::class.java)
            .equalTo(MessageRealmObject.Fields.ACCOUNT, account.account.toString())
            .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
            .isNotNull(MessageRealmObject.Fields.STANZA_ID)
            .findAll()
            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
            .last()
            ?.stanzaId

    private fun hasMessage(chat: AbstractChat,
    ): Boolean {
        var result = false
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            val message = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                    .findAll()
                    .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
                    .first()
            result = message != null && message.isValid
        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
        return result
    }

    private fun getFirstMessageId(chat: AbstractChat,
    ): String? {
        var result: String? = ""
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                    .findAll()
                    .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
                    .first()
                    ?.stanzaId
        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
        return result
    }

    private fun updateLastMessageIdInChat(chat: AbstractChat,
    ) {
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            chat.lastMessageId = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                    .findFirst()
                    ?.stanzaId
        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
    }

    private fun List<Forwarded>.groupToChats(accountItem: AccountItem,
    ): Map<String, List<Forwarded>> = this.groupBy { forwarded -> 
        if (forwarded.forwardedStanza.from.asBareJid() == accountItem.account.fullJid.asBareJid()) {
            forwarded.forwardedStanza.to.asBareJid().toString()
        } else forwarded.forwardedStanza.from.asBareJid().toString() 
    }

    private fun createNewMamDataForm() =
            DataForm(DataForm.Type.submit).apply {
                addField(
                        FormField(FormField.FORM_TYPE).apply {
                            type = FormField.Type.hidden
                            addValue(MamElements.NAMESPACE)
                        }
                )
            }

}