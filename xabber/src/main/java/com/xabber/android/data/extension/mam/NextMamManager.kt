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
import org.jivesoftware.smackx.mam.MamManager.MamPrefsResult
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

object NextMamManager : OnRosterReceivedListener, OnPacketListener {

    private val LOG_TAG = NextMamManager::class.java.simpleName
    const val NAMESPACE = "urn:xmpp:mam:tmp"

    private val supportedByAccount: MutableMap<AccountJid, Boolean> = ConcurrentHashMap()
    private var isRequested = false
    private val lock = Any()
    private val waitingRequests: MutableMap<String, ContactJid> = HashMap()
    private val rosterItemIterators: MutableMap<AccountItem?, Iterator<RosterContact>?> = ConcurrentHashMap()

    override fun onRosterReceived(accountItem: AccountItem) {
        LogManager.d(LOG_TAG, "onRosterReceivedStarted")
        updateIsSupported(accountItem)
        //updatePreferencesFromServer(accountItem);
        //LogManager.d("AccountRosterListener", "finished updating preferences");
        val realm = DatabaseManager.getInstance().defaultRealmInstance
        accountItem.startHistoryTimestamp = getLastMessageTimestamp(accountItem, realm)
        if (accountItem.startHistoryTimestamp == 0L) {
            initializeStartTimestamp(realm, accountItem)
            loadMostRecentMessages(realm, accountItem)
            startLoadingLastMessageInAllChats(accountItem)
        } else {
            val lastArchivedId = getLastMessageArchivedId(accountItem, realm)
            if (lastArchivedId != null) {
                val historyCompleted = loadAllNewMessages(realm, accountItem, lastArchivedId)
                if (!historyCompleted) {
                    startLoadingLastMessageInAllChats(accountItem)
                } else startLoadingLastMessageInMissedChats(realm, accountItem)
            } else startLoadingLastMessageInAllChats(accountItem)
        }
        updatePreferencesFromServer(accountItem)
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
            val firstMessage = getFirstMessage(chat, realm)
            if (firstMessage == null) loadLastMessage(realm, accountItem, chat)
            synchronized(lock) {
                isRequested = if (isRequested) return@runInBackgroundNetworkUserRequest else true
            }

            // load prev page if history is not enough
            if (historyIsNotEnough(realm, chat) && !chat.historyIsFull()) {

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
            if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
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

        // if history is empty - load last message
        val firstMessage = getFirstMessage(chat, realm)

        if (firstMessage == null) loadLastMessage(realm, accountItem, chat)

        var complete = false
        while (!complete) {
            complete = loadNextHistory(realm, accountItem, chat)
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close()
    }

    fun onRequestUpdatePreferences(accountJid: AccountJid) {
        val accountItem = AccountManager.getInstance().getAccount(accountJid)
        if (accountItem == null || !isSupported(accountJid)) return
        Application.getInstance().runInBackgroundNetworkUserRequest { requestUpdatePreferences(accountItem) }
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
                                parseAndSaveMessageFromMamResult(realm, connection.account, packetExtension.forwarded)
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
    private fun startLoadingLastMessageInMissedChats(realm: Realm,
                                                     accountItem: AccountItem,
    ) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) return

        val contactsWithoutHistory: MutableCollection<RosterContact> = ArrayList()

        RosterManager.getInstance().getAccountRosterContacts(accountItem.account).forEach { contact ->
            var chat = ChatManager.getInstance().getChat(contact.account, contact.contactJid)

            if (chat == null) chat = ChatManager.getInstance().createRegularChat(contact.account, contact.contactJid)

            if (getFirstMessage(chat, realm) == null && !chat!!.isHistoryRequestedAtStart) {
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

    private fun loadLastMessage(realm: Realm,
                                accountItem: AccountItem,
                                chat: AbstractChat,
    ) {
        LogManager.d(LOG_TAG, "load last messages in chat: " + chat.contactJid)
        val queryResult = requestLastMessage(accountItem, chat)
        if (queryResult != null) {
            val messages: List<Forwarded> = ArrayList(queryResult.forwardedMessages)
            saveOrUpdateMessages(realm, parseMessage(accountItem, chat.account, chat.contactJid, messages))
        }
        updateLastMessageId(chat, realm)
    }

    private fun loadMostRecentMessages(realm: Realm,
                                       accountItem: AccountItem,
    ) {
        if (accountItem.loadHistorySettings != LoadHistorySettings.all || !isSupported(accountItem.account)) {
            return
        }
        LogManager.d(LOG_TAG, "load new messages")
        val messages: MutableList<Forwarded> = ArrayList()
        val queryResult = requestRecentMessages(accountItem)
        if (queryResult != null) messages.addAll(queryResult.forwardedMessages)
        if (messages.isNotEmpty()) {
            val parsedMessages: MutableList<MessageRealmObject> = ArrayList()
            val chatsNeedUpdateLastMessageId: MutableList<AbstractChat?> = ArrayList()
            val messagesByChat: HashMap<String, ArrayList<Forwarded>> = sortNewMessagesByChats(messages, accountItem)
            for ((key, list) in messagesByChat) {
                try {
                    var chat = ChatManager.getInstance().getChat(accountItem.account, ContactJid.from(key))

                    if (chat == null) {
                        chat = ChatManager.getInstance().createRegularChat(accountItem.account, ContactJid.from(key))
                    }

                    val oldSize = parsedMessages.size
                    parsedMessages.addAll(parseNewMessagesInChat(list, chat, accountItem))

                    if (parsedMessages.size - oldSize > 0) chatsNeedUpdateLastMessageId.add(chat)
                } catch (e: ContactJidCreateException) {
                    LogManager.d(LOG_TAG, e.toString())
                }
            }
            saveOrUpdateMessages(realm, parsedMessages)
            chatsNeedUpdateLastMessageId.forEach { chat ->
                run {
                    updateLastMessageId(chat, realm)
                    chat!!.setHistoryRequestedWithoutRealm(true)
                    ChatManager.getInstance().saveOrUpdateChatDataToRealm(chat)
                }
            }
        }
    }

    private fun loadAllNewMessages(realm: Realm,
                                   accountItem: AccountItem,
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
            val chatsNeedUpdateLastMessageId: MutableList<AbstractChat?> = ArrayList()
            val messagesByChat: HashMap<String, ArrayList<Forwarded>> = sortNewMessagesByChats(messages, accountItem)

            // parse message lists
            for ((key, list) in messagesByChat) {
                try {
                    val chat = ChatManager.getInstance().getChat(accountItem.account, ContactJid.from(key))
                            ?: ChatManager.getInstance().createRegularChat(accountItem.account, ContactJid.from(key))

                    parsedMessages.addAll(parseNewMessagesInChat(list, chat, accountItem))
                    if (parsedMessages.size - parsedMessages.size > 0) chatsNeedUpdateLastMessageId.add(chat)
                } catch (e: ContactJidCreateException) {
                    LogManager.d(LOG_TAG, e.toString())
                }
            }

            // save messages to Realm
            saveOrUpdateMessages(realm, parsedMessages)
            chatsNeedUpdateLastMessageId.forEach { chat -> updateLastMessageId(chat, realm) }
        }
        return complete
    }

    private fun parseNewMessagesInChat(chatMessages: ArrayList<Forwarded>,
                                       chat: AbstractChat?,
                                       accountItem: AccountItem,
    ): List<MessageRealmObject> {
        Collections.sort(chatMessages, archiveMessageTimeComparator)
        return ArrayList(parseMessage(accountItem, accountItem.account, chat!!.contactJid,
                chatMessages))
    }

    private fun loadNextHistory(realm: Realm,
                                accountItem: AccountItem,
                                chat: AbstractChat,
    ): Boolean {
        LogManager.d(LOG_TAG, "load next history in chat: " + chat.contactJid)
        val firstMessage = getFirstMessage(chat, realm)
        if (firstMessage != null) {
            val queryResult = requestMessagesBeforeId(accountItem, chat, firstMessage.stanzaId)
            if (queryResult != null) {
                val messages: List<Forwarded> = ArrayList(queryResult.forwardedMessages)
                if (messages.isNotEmpty()) {
                    val savedMessages = saveOrUpdateMessages(
                            realm, parseMessage(accountItem, chat.account, chat.contactJid, messages))
                    if (savedMessages.isNotEmpty()) {
                        realm.beginTransaction()
                        realm.commitTransaction()
                        return false
                    }
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
                val savedMessages = saveOrUpdateMessages(
                        realm, parseMessage(accountItem, chat.account, chat.contactJid, messages))
                if (savedMessages.isNotEmpty()) {
                    realm.beginTransaction()
                    realm.commitTransaction()
                }
            } else {
                realm.beginTransaction()
                realm.commitTransaction()
            }
        }
    }

    /** Request most recent message from all history and save it timestamp to startHistoryTimestamp
     * If message is null save current time to startHistoryTimestamp  */
    private fun initializeStartTimestamp(realm: Realm, accountItem: AccountItem) {
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
            parseAndSaveMessageFromMamResult(realm, accountItem.account, forwarded)
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

    private fun updatePreferencesFromServer(accountItem: AccountItem) {
        val prefsResult = requestPreferencesFromServer(accountItem)
        if (prefsResult != null) {
            val behavior = prefsResult.mamPrefs.default
            AccountManager.getInstance().setMamDefaultBehaviour(accountItem.account, behavior)
        }
    }

    /** T extends MamManager.MamQueryResult or T extends MamManager.MamPrefsResult  */
    internal abstract class MamRequest<T> {
        abstract fun execute(manager: MamManager): T
    }

    /** T extends MamManager.MamQueryResult or T extends MamManager.MamPrefsResult  */
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
            @Throws(Exception::class)
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
                val dataForm = newMamForm
                addWithJid(chat.contactJid.jid, dataForm)
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
                val dataForm = newMamForm
                addWithStanzaId(stanzaId, dataForm)
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

    /** Request update archiving preferences on server  */
    private fun requestUpdatePreferences(accountItem: AccountItem) {
        requestToMessageArchive(accountItem, object : MamRequest<MamPrefsResult>() {
            override fun execute(manager: MamManager): MamPrefsResult {
                return manager.updateArchivingPreferences(null, null, accountItem.mamDefaultBehaviour)
            }
        })
    }

    /** Request archiving preferences from server  */
    private fun requestPreferencesFromServer(accountItem: AccountItem): MamPrefsResult? {
        return requestToMessageArchive(accountItem, object : MamRequest<MamPrefsResult>() {
            override fun execute(manager: MamManager): MamPrefsResult {
                return manager.retrieveArchivingPreferences()
            }
        })
    }

    /** PARSING  */
    private fun parseAndSaveMessageFromMamResult(realm: Realm,
                                                 account: AccountJid,
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
                saveOrUpdateMessages(realm, listOf(messageRealmObject), true)
                updateLastMessageId(chat, realm)
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

        if (stanzaId != null) messageRealmObject.stanzaId = stanzaId
        val timestamp = delayInformation.stamp.time
        messageRealmObject.resource = user.jid.resourceOrNull
        messageRealmObject.text = body
        if (markupBody != null) messageRealmObject.markupText = markupBody
        messageRealmObject.timestamp = timestamp
        if (messageDelay != null) messageRealmObject.delayTimestamp = messageDelay.stamp.time
        messageRealmObject.isIncoming = incoming
        messageRealmObject.originId = UniqueIdsHelper.getOriginId(message)
        messageRealmObject.isRead = timestamp <= accountItem!!.startHistoryTimestamp
        if (incoming) {
            messageRealmObject.messageStatus = MessageStatus.NONE
        } else messageRealmObject.messageStatus = MessageStatus.DISPLAYED
        messageRealmObject.isEncrypted = encrypted

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
    private fun saveOrUpdateMessages(realm: Realm,
                                     messages: Collection<MessageRealmObject>?,
                                     ui: Boolean = false,
    ): List<MessageRealmObject> {
        val messagesToSave: MutableList<MessageRealmObject> = ArrayList()
        realm.refresh()
        if (messages != null && !messages.isEmpty()) {
            for (message in messages) {
                val newMessage = determineSaveOrUpdate(realm, message, ui)
                if (newMessage != null) messagesToSave.add(newMessage)
            }
        }
        realm.beginTransaction()
        realm.copyToRealmOrUpdate(messagesToSave)
        realm.commitTransaction()
        SyncManager.getInstance().onMessageSaved()
        for (listener in Application.getInstance().getUIListeners(OnNewMessageListener::class.java)) {
            listener.onNewMessage()
        }
        return messagesToSave
    }

    private fun determineSaveOrUpdate(realm: Realm, message: MessageRealmObject, ui: Boolean): MessageRealmObject? {
        var originalMessage: Message? = null
        try {
            originalMessage = PacketParserUtils.parseStanza(message.originalStanza)
        } catch (e: Exception) {
            LogManager.exception(LOG_TAG, e)
            LogManager.e(LOG_TAG, message.originalStanza)
        }
        val chat = ChatManager.getInstance().getChat(message.account, message.user)
                ?: return null
        val localMessage = findSameLocalMessage(realm, chat, message)
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
                    && message.text != null && !message.text.trim { it <= ' ' }.isEmpty()
                    && message.isIncoming
                    && chat.notifyAboutMessage())
            val visible = ChatManager.getInstance().isVisibleChat(chat)
            if (notify && !visible) NotificationManager.getInstance().onMessageNotification(message)
            message
        } else {
            LogManager.d(LOG_TAG, "Matching message found! Updating message")
            realm.beginTransaction()
            localMessage.stanzaId = message.stanzaId
            realm.commitTransaction()
            localMessage
        }
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

    private fun findMissedMessages(realm: Realm, chat: AbstractChat): List<MessageRealmObject>? {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
        return if (results != null && !results.isEmpty()) {
            ArrayList(results)
        } else null
    }

    private fun getMessageForCloseMissedMessages(realm: Realm, messageRealmObject: MessageRealmObject): MessageRealmObject? {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, messageRealmObject.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, messageRealmObject.user.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .lessThan(MessageRealmObject.Fields.TIMESTAMP, messageRealmObject.timestamp)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
        return if (results != null && !results.isEmpty()) {
            results.first()
        } else null
    }

    private fun historyIsNotEnough(realm: Realm, chat: AbstractChat): Boolean {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .findAll()
        return results.size < 30
    }

    private fun getLastMessageArchivedId(account: AccountItem, realm: Realm): String? {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, account.account.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
        return if (results != null && !results.isEmpty()) {
            results.last()!!.stanzaId
        } else null
    }

    private fun getFirstMessage(chat: AbstractChat?, realm: Realm): MessageRealmObject? {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat!!.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
        return if (results != null && !results.isEmpty()) {
            results.first()
        } else null
    }

    private fun getLastMessageTimestamp(account: AccountItem, realm: Realm): Long {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, account.account.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
        return if (results != null && !results.isEmpty()) {
            val lastMessage = results.last()
            lastMessage!!.timestamp
        } else 0
    }

    private fun updateLastMessageId(chat: AbstractChat?, realm: Realm) {
        val results = realm.where(MessageRealmObject::class.java)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, chat!!.account.toString())
                .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
        if (results != null && !results.isEmpty()) {
            val lastMessage = results.last()
            chat.lastMessageId = lastMessage!!.stanzaId
        }
    }

    private fun findSameLocalMessage(realm: Realm, chat: AbstractChat, message: MessageRealmObject): MessageRealmObject? {
        return realm.where(MessageRealmObject::class.java)
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
    }

    private val archiveMessageTimeComparator = Comparator { o1: Forwarded, o2: Forwarded ->
        val time1 = o1.delayInformation.stamp.time
        val time2 = o2.delayInformation.stamp.time
        time1.compareTo(time2)
    }

    private fun sortNewMessagesByChats(messages: List<Forwarded>,
                                       accountItem: AccountItem,
    ): HashMap<String, ArrayList<Forwarded>> {
        val sortedMapOfChats = HashMap<String, ArrayList<Forwarded>>()
        for (forwarded in messages) {
            val stanza = forwarded.forwardedStanza
            var user: Jid = stanza.from.asBareJid()

            if (user.equals(accountItem.account.fullJid.asBareJid())) user = stanza.to.asBareJid()

            if (!sortedMapOfChats.containsKey(user.toString())) sortedMapOfChats[user.toString()] = ArrayList()

            val list = sortedMapOfChats[user.toString()]
            list?.add(forwarded)
        }
        return sortedMapOfChats
    }

    /** UTILS  */
    private val newMamForm: DataForm
        get() {
            val formField = FormField(FormField.FORM_TYPE)
            formField.type = FormField.Type.hidden
            formField.addValue(MamElements.NAMESPACE)
            val form = DataForm(DataForm.Type.submit)
            form.addField(formField)
            return form
        }

    private fun addWithJid(withJid: Jid?, dataForm: DataForm) {
        if (withJid == null) return
        val formField = FormField("with")
        formField.addValue(withJid.toString())
        dataForm.addField(formField)
    }

    private fun addWithStanzaId(stanzaId: String?, dataForm: DataForm) {
        if (stanzaId == null) return
        val formField = FormField("{urn:xmpp:sid:0}stanza-id")
        formField.addValue(stanzaId)
        dataForm.addField(formField)
    }

}