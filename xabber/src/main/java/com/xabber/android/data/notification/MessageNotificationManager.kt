package com.xabber.android.data.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.text.Html
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.OnLoadListener
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.NotificationChatRepository
import com.xabber.android.data.database.repositories.NotificationChatRepository.removeAllNotificationChatInRealm
import com.xabber.android.data.database.repositories.NotificationChatRepository.removeNotificationChatsByAccountInRealm
import com.xabber.android.data.database.repositories.NotificationChatRepository.removeNotificationChatsForAccountAndContactInRealm
import com.xabber.android.data.database.repositories.NotificationChatRepository.saveOrUpdateToRealm
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager.getMe
import com.xabber.android.data.extension.groups.GroupMemberManager.requestMe
import com.xabber.android.data.extension.groups.GroupPrivacyType
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.notification.Action.ActionType
import com.xabber.android.data.notification.MessageNotificationCreator.MESSAGE_BUNDLE_NOTIFICATION_ID
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.OnContactChangedListener
import com.xabber.android.ui.forEachOnUi
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils
import java.util.*

object MessageNotificationManager : OnLoadListener {

    private val context: Application = Application.getInstance()

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val creator = MessageNotificationCreator(context, notificationManager)

    private val nextChatNotificationId: Int
        get() = System.currentTimeMillis().toInt()

    private val chats: MutableList<Chat> = ArrayList()
    private val delayedActions = HashMap<Int, Action>()
    private var lastNotificationTime: Long = 0
    var isShowBanners = false

    val isTimeToNewFullNotification: Boolean
        get() = System.currentTimeMillis() > lastNotificationTime + 1000

    fun updateLastNotificationTime() {
        lastNotificationTime = System.currentTimeMillis()
    }

    /** LISTENER  */
    fun onNotificationAction(action: Action) {
        if (action.actionType != ActionType.cancel) {
            chats
                .find { it.notificationId == action.notificationID }
                ?.let { chat ->
                    val accountJid = chat.accountJid
                    val contactJid = chat.contactJid

                    performAction(FullAction(action, accountJid, contactJid))

                    // update notification
                    if (action.actionType == ActionType.reply) {
                        val groupMember =
                            if (chat.isGroupChat) {
                                getMe(
                                    (ChatManager.getInstance()
                                        .getChat(accountJid, contactJid) as GroupChat)
                                )
                            } else null
                        addMessage(chat, "", action.replyText, false, groupMember)
                        saveOrUpdateToRealm(chat)
                    }
                }
        }

        // cancel notification
        if (action.actionType != ActionType.reply) {
            notificationManager.cancel(action.notificationID)
            onNotificationCanceled(action.notificationID)
        }
    }

    fun onDelayedNotificationAction(action: Action) {
        notificationManager.cancel(action.notificationID)
        delayedActions[action.notificationID] = action
    }

    override fun onLoad() {
        Application.getInstance().runInBackground {
            NotificationChatRepository.getAllNotificationChatsFromRealm()
                .forEach { chatRealmObject ->
                    if (delayedActions.containsKey(chatRealmObject.notificationId)) {
                        delayedActions[chatRealmObject.notificationId]?.let { action ->
                            val accountJid = chatRealmObject.accountJid
                            val contactJid = chatRealmObject.contactJid

                            notificationManager.cancel(action.notificationID)
                            DelayedNotificationActionManager.getInstance().addAction(
                                FullAction(action, accountJid, contactJid)
                            )
                            removeNotificationChatsForAccountAndContactInRealm(
                                accountJid, contactJid
                            )
                        }
                    } else {
                        chats.add(chatRealmObject)
                    }
                }

            delayedActions.clear()

            chats.lastOrNull()?.messages?.lastOrNull()?.also {
                rebuildAllNotifications()
            }
        }
    }

    /** PUBLIC METHODS  */
    @Synchronized
    fun onNewMessage(
        messageRealmObject: MessageRealmObject, groupMember: GroupMemberRealmObject?
    ) {
        val accountJid = messageRealmObject.account
        val contactJid = messageRealmObject.user

        val abstractChat = ChatManager.getInstance().getChat(accountJid, contactJid)
        val isGroup = abstractChat is GroupChat

        val chatTitle =
            if (isGroup) {
                RosterManager.getInstance().getBestContact(accountJid, contactJid).name
            } else {
                ""
            }

        val privacyType =
            if (isGroup) {
                (abstractChat as GroupChat?)!!.privacyType
            } else {
                null
            }

        val chat =
            chats.find { it.equals(accountJid, contactJid) }
                ?: Chat(
                    accountJid = accountJid,
                    contactJid = contactJid,
                    notificationId = nextChatNotificationId,
                    chatTitle = chatTitle,
                    isGroupChat = isGroup,
                    privacyType = privacyType
                ).also { chats.add(it) }

        val sender =
            if (isGroup && groupMember != null) {
                groupMember.nickname
            } else {
                RosterManager.getInstance().getBestContact(accountJid, contactJid).name
            }

        addMessage(
            chat, sender, getNotificationText(messageRealmObject),
            true, groupMember, messageRealmObject.stanzaId
        )
        saveOrUpdateToRealm(chat)
    }

    @Synchronized
    fun onNewMessage(messageRealmObject: MessageRealmObject) {
        onNewMessage(messageRealmObject, null)
    }

    fun removeChatWithTimer(account: AccountJid, user: ContactJid) {
        chats
            .find { it.equals(account, user) }
            ?.startRemoveTimer()
    }

    @Synchronized
    fun removeChat(account: AccountJid, user: ContactJid) {
        chats
            .find { it.equals(account, user) }
            ?.let { chat ->
                chats.remove(chat)
                removeNotification(chat)
                removeNotificationChatsForAccountAndContactInRealm(account, user)
            }
    }

    @Synchronized
    fun removeChat(notificationId: Int) {
        chats
            .find { it.notificationId == notificationId }
            ?.let { chat ->
                chats.remove(chat)
                removeNotification(chat)
                removeNotificationChatsForAccountAndContactInRealm(chat.accountJid, chat.contactJid)
            }
    }

    fun removeNotificationForMessage(
        accountJid: AccountJid, stanzaId: String
    ) {
        chats.find { chat ->
            chat.accountJid == accountJid
                    && chat.messages.any { message -> message.id == stanzaId }
        }?.let { chat ->
            notificationManager.cancel(chat.notificationId)
            chat.messages.removeAll { it.id == stanzaId }
            rebuildAllNotifications()
        }
    }

//    fun replaceNotificationTextForMessage(
//        accountJid: AccountJid, stanzaId: String, newText: String
//    ) {
//        chats.find { chat ->
//            chat.accountJid == accountJid
//                    && chat.messages.any { message -> message.id == stanzaId }
//        }?.let { chat ->
//            notificationManager.cancel(chat.notificationId)
//            chat.messages.find { it.id == stanzaId }.messageText = newText
//            rebuildAllNotifications()
//        }
//    }

    fun removeNotificationsForAccount(account: AccountJid) {
        val chatsToRemove: MutableList<Chat> = ArrayList()
        val it = chats.iterator()
        while (it.hasNext()) {
            val chat = it.next()
            if (chat.accountJid == account) {
                chatsToRemove.add(chat)
                it.remove()
            }
        }
        removeNotifications(chatsToRemove)
        removeNotificationChatsByAccountInRealm(account)
    }

    fun removeAllMessageNotifications() {
        val chatsToRemove: MutableList<Chat> = ArrayList()
        val it = chats.iterator()
        while (it.hasNext()) {
            val chat = it.next()
            chatsToRemove.add(chat)
            it.remove()
        }
        removeNotifications(chatsToRemove)
        removeAllNotificationChatInRealm()
    }

    fun rebuildAllNotifications() {
        chats.removeAll { it.messages.isEmpty() }
        notificationManager.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (chat in chats) {
                creator.createNotification(chat, true)
            }
            if (chats.size > 1) {
                creator.createBundleNotification(chats, true)
            }
        } else {
            if (chats.size > 1) {
                creator.createBundleNotification(chats, true)
            } else if (chats.size > 0) {
                creator.createNotification(chats[0], true)
            }
        }
    }

    /** PRIVATE METHODS  */
    private fun onNotificationCanceled(notificationId: Int) {
        if (notificationId == MESSAGE_BUNDLE_NOTIFICATION_ID) {
            removeAllMessageNotifications()
        } else {
            removeChat(notificationId)
        }
    }

    fun performAction(action: FullAction) {
        val accountJid = action.accountJid
        val contactJid = action.contactJid

        fun callUiUpdate() {
            Application.getInstance().getUIListeners(OnContactChangedListener::class.java)
                .forEachOnUi {
                    it.onContactsChanged(ArrayList())
                }
        }

        when (action.actionType) {
            ActionType.read -> {
                ChatManager.getInstance().getChat(accountJid, contactJid)?.let { chat ->
                    AccountManager.getInstance().stopGracePeriod(chat.account)
                    chat.markAsReadAll(true)
                    callUiUpdate()
                }
            }
            ActionType.snooze -> {
                ChatManager.getInstance().getChat(accountJid, contactJid)?.let { chat ->
                    chat.setNotificationState(
                        NotificationState(
                            NotificationState.NotificationMode.snooze2h,
                            System.currentTimeMillis() / 1000L
                        ),
                        true
                    )
                    callUiUpdate()
                }
            }
            ActionType.reply -> MessageManager.getInstance().sendMessage(
                accountJid, contactJid, action.replyText.toString()
            )
            else -> {
                /* ignore */
            }
        }
    }

    private fun addMessage(
        chat: Chat, author: CharSequence, messageText: CharSequence,
        alert: Boolean, groupMember: GroupMemberRealmObject?,
        messagePrimaryKey: String = UUID.randomUUID().toString()
    ) {
        chat.addMessage(
            Message(messagePrimaryKey, author, System.currentTimeMillis(), groupMember, messageText)
        )
        chat.stopRemoveTimer()
        addNotification(chat, alert)
    }

    private fun addNotification(chat: Chat, alert: Boolean) {
        if (chat.isGroupChat &&
            getMe(
                (ChatManager.getInstance()
                    .getChat(chat.accountJid, chat.contactJid) as GroupChat?)!!
            ) == null
        ) {
            requestMe(chat.accountJid, chat.contactJid)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (chats.size > 1) {
                creator.createBundleNotification(chats, true)
            }
            if (isShowBanners) {
                creator.createNotification(chat, alert)
            } else {
                creator.createNotificationWithoutBannerJustSound()
            }
        } else {
            if (chats.size > 1) {
                if (chats.size == 2) {
                    notificationManager.cancel(chats[0].notificationId)
                    notificationManager.cancel(chats[1].notificationId)
                }
                if (isShowBanners) {
                    creator.createNotification(chat, alert)
                } else {
                    creator.createNotificationWithoutBannerJustSound()
                }
            } else if (chats.size > 0) {
                creator.createNotification(chats[0], true)
            }
        }
    }

    private fun removeNotification(chat: Chat) {
        removeNotifications(listOf(chat))
    }

    private fun removeNotifications(chatsToRemove: List<Chat>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (chats.size > 1) {
                creator.createBundleNotification(chats, true)
            }
            chatsToRemove.forEach {
                notificationManager.cancel(it.notificationId)
            }
            if (chats.size == 0) {
                notificationManager.cancel(MESSAGE_BUNDLE_NOTIFICATION_ID)
            }
        } else {
            when {
                chats.size > 1 -> {
                    creator.createBundleNotification(chats, false)
                }
                chats.size > 0 -> {
                    notificationManager.cancel(MESSAGE_BUNDLE_NOTIFICATION_ID)
                    creator.createNotification(chats[0], false)
                }
                else -> {
                    chatsToRemove.forEach {
                        notificationManager.cancel(it.notificationId)
                    }
                }
            }
        }
    }

    private fun getNotificationText(message: MessageRealmObject): String {
        val forwardedCount = message.forwardedIds?.size ?: 0
        val attachmentsCount = message.attachmentRealmObjects?.size ?: 0

        fun String.tryToDecode() =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                try {
                    Html.fromHtml(Utils.getDecodedSpannable(this).toString())
                } catch (e: Exception) {
                    Html.fromHtml(this)
                }
            } else this

        return when {
            message.action != null -> message.action

            forwardedCount > 0 ->
                if (message.text.isNullOrEmpty()) {
                    String.format(
                        context.resources.getQuantityString(
                            R.plurals.forwarded_messages_count,
                            forwardedCount
                        ), forwardedCount
                    )
                } else {
                    message.text.tryToDecode().toString()
                }

            attachmentsCount > 0 ->
                if (message.text.isNullOrEmpty()) {
                    StringUtils.getColoredAttachmentDisplayName(
                        context, message.attachmentRealmObjects, -1
                    )
                } else {
                    message.text.tryToDecode().toString()
                }
            else -> message.text.tryToDecode().toString()
        }
    }

    /** INTERNAL CLASSES  */
    class Chat(
        val id: String = UUID.randomUUID().toString(),
        val accountJid: AccountJid,
        val contactJid: ContactJid,
        val notificationId: Int,
        val chatTitle: CharSequence,
        val isGroupChat: Boolean,
        val privacyType: GroupPrivacyType?,
    ) {

        val messages: MutableList<Message> = ArrayList()
        private var removeTimer: Handler? = null

        fun addMessage(message: Message) {
            messages.add(message)
        }

        val lastMessageTimestamp: Long
            get() = messages.lastOrNull()?.timestamp ?: 0

        fun getLastMessage() = messages.lastOrNull()

        fun equals(account: AccountJid, user: ContactJid): Boolean {
            return accountJid == account && contactJid == user
        }

        fun startRemoveTimer() {
            Application.getInstance().runOnUiThread {
                stopRemoveTimer()
                removeTimer = Handler()
                removeTimer?.postDelayed(
                    {
                        Application.getInstance().runOnUiThread { removeChat(notificationId) }
                    },
                    500
                )
            }
        }

        fun stopRemoveTimer() {
            removeTimer?.removeCallbacksAndMessages(null)
        }
    }

    data class Message(
        val id: String = UUID.randomUUID().toString(),
        val author: CharSequence,
        val timestamp: Long,
        val groupMember: GroupMemberRealmObject?,
        private val _messageText: CharSequence,
    ) {

        val messageText: CharSequence
            get() {
                return try {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        Utils.getDecodedSpannable(_messageText.toString())
                    } else {
                        _messageText.toString()
                    }
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    _messageText.toString()
                }
            }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannelUtils.createMessageChannel(
                notificationManager,
                NotificationChannelUtils.ChannelType.privateChat,
                null, null, null
            )

            NotificationChannelUtils.createMessageChannel(
                notificationManager,
                NotificationChannelUtils.ChannelType.groupChat,
                null, null, null
            )

            NotificationChannelUtils.createMessageChannel(
                notificationManager,
                NotificationChannelUtils.ChannelType.attention,
                null, null, null
            )
        }
    }

}