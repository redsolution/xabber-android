package com.xabber.android.data.message

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.chat_markers.ChatMarkersElements
import com.xabber.android.data.extension.delivery.getTimeElement
import com.xabber.android.data.extension.delivery.hasTimeElement
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.references.ReferencesManager
import com.xabber.android.data.filedownload.DownloadManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.notification.MessageNotificationManager
import com.xabber.android.data.notification.NotificationManager
import com.xabber.android.data.push.SyncManager
import com.xabber.android.data.xaccount.XMPPAuthManager
import com.xabber.android.ui.OnNewIncomingMessageListener
import com.xabber.android.ui.OnNewMessageListener
import com.xabber.android.ui.forEachOnUi
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.chat_state.ChatStateExtension
import com.xabber.xmpp.groups.hasGroupExtensionElement
import com.xabber.xmpp.groups.hasGroupSystemMessage
import com.xabber.xmpp.groups.invite.incoming.getIncomingInviteExtension
import com.xabber.xmpp.groups.invite.incoming.hasIncomingInviteExtension
import com.xabber.xmpp.sid.UniqueIdsHelper
import io.realm.Realm
import io.realm.RealmList
import net.java.otr4j.io.SerializationUtils
import net.java.otr4j.io.messages.AbstractMessage
import net.java.otr4j.io.messages.PlainTextMessage
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.util.XmppDateTime
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

object MessageHandler {

    private val saverBuffer: PublishSubject<MessageRealmObject> = PublishSubject.create()

    init {
        saverBuffer
            .buffer(250, TimeUnit.MILLISECONDS)
            .onBackpressureBuffer()
            .observeOn(Schedulers.io())
            .subscribe { messagesList ->
                if (!messagesList.isNullOrEmpty()) {
                    var realm: Realm? = null
                    try {
                        realm = DatabaseManager.getInstance().defaultRealmInstance
                        realm.executeTransaction { realm1 -> realm1.copyToRealmOrUpdate(messagesList) }
                        messagesList
                            .groupBy { ChatManager.getInstance().getChat(it.account, it.user) }
                            .filter { it.key != null }
                            .map { repairArchiveMessagesStatuses(it.key!!) }
                        SyncManager.getInstance().onMessageSaved()
                        checkForAttachmentsAndDownload(messagesList)
                        notifySamUiListeners(OnNewMessageListener::class.java)
                    } catch (e: Exception) {
                        LogManager.exception(this, e)
                    } finally {
                        if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close()
                    }
                }
            }
    }

    private fun repairArchiveMessagesStatuses(chat: AbstractChat) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                realm.executeTransaction { realmTransaction ->
                    val messages = realmTransaction
                        .where(MessageRealmObject::class.java)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                        .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                        .sort(MessageRealmObject.Fields.TIMESTAMP)
                        .findAll()

                    if (messages.last()?.isIncoming == true) {
                        messages.dropLastWhile { it.isIncoming }.forEach { it.apply { isRead = true } }
                        realmTransaction.copyToRealmOrUpdate(messages)
                    } else realmTransaction.copyToRealmOrUpdate(messages.map { it.apply { isRead = true } })
                }
            }
        }
    }

    fun parseMessage(
        accountJid: AccountJid,
        contactJid: ContactJid,
        messageStanza: Message,
        delayInformation: DelayInformation? = null,
    ): MessageRealmObject? {

        val timestamp = when {
            messageStanza.hasTimeElement() -> XmppDateTime.parseDate(messageStanza.getTimeElement().timeStamp).time
            delayInformation != null -> delayInformation.stamp.time
            else -> Date().time
        }

        if (messageStanza.hasIncomingInviteExtension()
            && messageStanza.from.asBareJid().toString() != accountJid.bareJid.toString()
        ) {
            GroupInviteManager.processIncomingInvite(
                messageStanza.getIncomingInviteExtension()!!, accountJid, contactJid, timestamp
            )

            return null
        }

        if (messageStanza.getOptimalTextBody() == null && messageStanza.hasExtension(ChatStateExtension.NAMESPACE)) {
            return null
        }

        if (messageStanza.hasExtension(ChatMarkersElements.NAMESPACE) &&
            !messageStanza.hasExtension(ChatMarkersElements.MarkableExtension.ELEMENT, ChatMarkersElements.NAMESPACE)
        ) {
            return null
        }

        if (messageStanza.type != Message.Type.chat) return null

        if (delayInformation != null && "Offline Storage" == delayInformation.reason) return null

        if (messageStanza.type == Message.Type.headline
            && XMPPAuthManager.getInstance().isXabberServiceMessage(messageStanza.stanzaId)
        ) {
            return null
        }

        if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
            && messageStanza.hasGroupExtensionElement()
        ) {
            return null
        }


        val groupMember = (ReferencesManager.getGroupchatUserFromReferences(messageStanza))?.let {
            GroupMemberManager.saveOrUpdateMemberFromMessage(it, accountJid, contactJid)
        }
        val isGroupSystem = messageStanza.hasGroupSystemMessage()

        val chat = ChatManager.getInstance().getChat(accountJid, contactJid)
            ?: ChatManager.getInstance().createRegularChat(accountJid, contactJid)

        val resource: Resourcepart? = messageStanza.from.resourceOrNull

        if (resource != null && resource != Resourcepart.EMPTY) chat?.resource = resource

        if (messageStanza.thread != null) chat?.threadId = messageStanza.thread

        var body = messageStanza.getOptimalTextBody()
        val otrMessage: AbstractMessage? = try {
            SerializationUtils.toMessage(body)
        } catch (e: IOException) {
            LogManager.exception(this, e)
            return null
        }
        var encrypted = false
        if (otrMessage != null) {
            if (otrMessage.messageType != AbstractMessage.MESSAGE_PLAINTEXT) {
                encrypted = true
                try {
                    // this transforming just decrypt message if have keys. No action as injectMessage or something else
                    body = OTRManager.getInstance().transformReceivingIfSessionExist(accountJid, contactJid, body)
                    if (OTRManager.getInstance().isEncrypted(body)) return null
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    return null
                }
            } else body = (otrMessage as PlainTextMessage).cleanText
        }

        // forward comment (to support previous forwarded xep)
        val forwardComment = ForwardManager.parseForwardComment(messageStanza)
        if (forwardComment != null) body = forwardComment

        // modify body with references
        val bodies = ReferencesManager.modifyBodyWithReferences(messageStanza, body)
        body = bodies.first
        val markupBody = bodies.second

        val isIncoming = messageStanza.from.asBareJid().equals(contactJid.jid.asBareJid())

        val stanzaId =
            if (groupMember != null || isGroupSystem) {
                UniqueIdsHelper.getStanzaIdBy(messageStanza, contactJid.bareJid.toString())
            } else UniqueIdsHelper.getStanzaIdBy(messageStanza, accountJid.bareJid.toString())

        val accountStartHistoryTimestamp =
            AccountManager.getInstance().getAccount(accountJid)?.startHistoryTimestamp?.time

        //FileManager.processFileMessage(messageRealmObject);
        val attachmentRealmObjects = try {
            HttpFileUploadManager.parseFileMessage(messageStanza)
        } catch (e: Exception) {
            null
        }

        val id = UUID.randomUUID().toString()

        val forwardIdRealmObjects = parseForwardedMessage(messageStanza, id, chat!!)

        val originId = UniqueIdsHelper.getOriginId(messageStanza)

        val isMe = groupMember?.isMe ?: false

        val messageRealmObject = if (originId != null) {
            MessageRealmObject.createMessageRealmObjectWithOriginId(accountJid, contactJid, originId)
        } else MessageRealmObject.createMessageRealmObjectWithStanzaId(accountJid, contactJid, stanzaId)

        messageRealmObject.apply {
            this.text = body ?: ""
            this.isRead = !isIncoming || isGroupSystem
                    || delayInformation != null && (timestamp <= accountStartHistoryTimestamp ?: 0)
            this.isEncrypted = encrypted
            this.isOffline = MessageManager.isOfflineMessage(accountJid.fullJid.domain, messageStanza)
            this.timestamp = timestamp
            this.isIncoming = isIncoming && !isMe
            this.stanzaId = stanzaId
            this.originId = originId
            this.originalStanza = messageStanza.toXML().toString()
            this.originalFrom = messageStanza.from.toString()
            this.isForwarded = false
            this.isGroupchatSystem = isGroupSystem
            this.resource = resource ?: Resourcepart.EMPTY
            this.messageStatus = if (isIncoming && !isMe) MessageStatus.NONE else MessageStatus.DISPLAYED
            this.markupText = markupBody
            this.delayTimestamp = DelayInformation.from(messageStanza)?.stamp?.time
            (attachmentRealmObjects)?.let { this.attachmentRealmObjects = it }
            this.forwardedIds = forwardIdRealmObjects
            this.groupchatUserId = groupMember?.memberId
        }

        saverBuffer.onNext(messageRealmObject ?: return null)

        // remove notifications if get outgoing message with 2 sec delay
        if (!isIncoming) MessageNotificationManager.getInstance().removeChatWithTimer(chat.account, chat.contactJid)

        // when getting new message, unarchive chat if chat not muted
        if (chat.notifyAboutMessage()) chat.isArchived = false

        // update last id in chat
        chat.lastMessageId = messageRealmObject.stanzaId

        // notification
        var isNotify = isIncoming && !isGroupSystem

        if (body?.trim()?.isEmpty() == true
            && (forwardIdRealmObjects == null || forwardIdRealmObjects.isEmpty())
            && (attachmentRealmObjects == null || attachmentRealmObjects.isEmpty())
        ) {
            isNotify = false
        }
        if (isNotify || !isIncoming) chat.openChat()

        chat.enableNotificationsIfNeed()

        if (isNotify && chat.notifyAboutMessage() && !isGroupSystem && delayInformation == null) {
            Application.getInstance().getUIListeners(OnNewIncomingMessageListener::class.java)
                .forEachOnUi { listener ->
                    listener.onNewIncomingMessage(accountJid, contactJid, messageRealmObject, true)
                }
            if (!ChatManager.getInstance().isVisibleChat(chat)) {
                if (groupMember != null) {
                    NotificationManager.getInstance().onMessageNotification(messageRealmObject, groupMember)
                } else NotificationManager.getInstance().onMessageNotification(messageRealmObject)
            }
        } else {
            Application.getInstance().getUIListeners(OnNewIncomingMessageListener::class.java)
                .forEachOnUi { listener ->
                    listener.onNewIncomingMessage(accountJid, contactJid, messageRealmObject, false)
                }
        }

        return messageRealmObject
    }

    private fun checkForAttachmentsAndDownload(messageRealmObjects: List<MessageRealmObject>) {
        if (SettingsManager.chatsAutoDownloadVoiceMessage()) {
            messageRealmObjects
                .flatMap { message ->
                    message.attachmentRealmObjects.map { attach -> Pair(attach, message.account) }
                }
                .filter { pair: Pair<AttachmentRealmObject, AccountJid> ->
                    pair.first.isVoice && pair.first.filePath == null
                }
                .map { pair: Pair<AttachmentRealmObject, AccountJid> ->
                    DownloadManager.getInstance().downloadFile(pair.first, pair.second, Application.getInstance())
                }
        }
    }

    private fun parseForwardedMessage(
        packet: Stanza,
        parentMessageId: String,
        chat: AbstractChat,
    ): RealmList<ForwardIdRealmObject>? {
        var forwarded = ReferencesManager.getForwardedFromReferences(packet)
        if (forwarded.isEmpty()) forwarded = ForwardManager.getForwardedFromStanza(packet)
        if (forwarded.isEmpty()) return null

        val forwardedIds = RealmList<ForwardIdRealmObject>()
        for (forward in forwarded) {
            val stanza = forward.forwardedStanza
            val timestamp = forward.delayInformation.stamp
            if (stanza is Message) {
                forwardedIds.add(ForwardIdRealmObject(parseInnerMessage(stanza, timestamp, parentMessageId, chat)))
            }
        }
        return forwardedIds
    }

    private fun parseInnerMessage(
        message: Message,
        timestamp: Date?,
        parentMessageId: String?,
        chat: AbstractChat,
    ): String? {

        if (message.type == Message.Type.error) return null

        var text: String? = message.body ?: return null

        // groupchat
        var groupchatUserId: String? = null
        val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message)
        if (groupchatUser != null) {
            groupchatUserId = groupchatUser.id
            if (message.from == null) {
                LogManager.e(this, "Got possible rewrite, todo implement handling")
                return null
            }
            GroupMemberManager.saveOrUpdateMemberFromMessage(groupchatUser, chat.account, ContactJid.from(message.from))
        }

        // forward comment (to support previous forwarded xep)
        val forwardComment = ForwardManager.parseForwardComment(message)
        if (forwardComment != null && forwardComment.isNotEmpty()) text = forwardComment

        // modify body with references
        val bodies = ReferencesManager.modifyBodyWithReferences(message, text)
        text = bodies.first

        val originId = UniqueIdsHelper.getOriginId(message)
        val stanzaId =
            UniqueIdsHelper.getStanzaIdBy(message, chat.contactJid.bareJid.toString()) ?: UUID.randomUUID().toString()

        val messageRealmObject = if (originId != null) {
            MessageRealmObject.createMessageRealmObjectWithOriginId(chat.account, chat.contactJid, originId)
        } else MessageRealmObject.createMessageRealmObjectWithStanzaId(chat.account, chat.contactJid, stanzaId)

        messageRealmObject.apply {
            this.text = text ?: ""
            this.timestamp = timestamp?.time ?: Date().time
            this.isIncoming = true
            this.isRead = true
            this.isEncrypted = OTRManager.getInstance().isEncrypted(text)
            this.isOffline = false
            this.stanzaId = stanzaId
            this.originId = originId
            this.originalStanza = message.toXML().toString()
            this.originalFrom = message.from?.toString()
            this.parentMessageId = parentMessageId
            this.isForwarded = true
            this.isGroupchatSystem = isGroupchatSystem
            this.resource = message.from?.resourceOrNull ?: Resourcepart.EMPTY
            this.messageStatus = MessageStatus.NONE
            this.action = null
            this.markupText = bodies.second
            this.delayTimestamp = DelayInformation.from(message)?.stamp?.time
            this.attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message) ?: null
            this.groupchatUserId = groupchatUserId
        }

        if (messageRealmObject != null) saverBuffer.onNext(messageRealmObject)

        return messageRealmObject.primaryKey
    }

}

//    /**
//     * Creates new action.
//     *
//     * @param resource can be `null`.
//     * @param text     can be `null`.
//     */
//    fun newActionMessage(resource: Resourcepart,
//                         text: String,
//                         action: ChatAction,
//                         abstractChat: AbstractChat,
//    ) {
//        createAndSaveNewMessage(uid = UUID.randomUUID().toString(),
//                resource = resource,
//                text = text,
//                markupText = null,
//                action = action,
//                timestamp = null,
//                delayTimestamp = null,
//                incoming = true,
//                notify = false,
//                encrypted = false,
//                offline = false,
//                stanzaId = null,
//                originId = null,
//                originalStanza = null,
//                parentMessageId = null,
//                originalFrom = null,
//                isForwarded = false,
//                forwardIdRealmObjects = null,
//                fromMAM = false,
//                groupchatUserId = null,
//                isGroupchatSystem = false,
//                chat = abstractChat)
//    }

//    /**
//     * Creates new action with the same timestamp as the last message,
//     * as not to disturb the order of chatList elements.
//     *
//     * @param resource can be `null`.
//     * @param text     can be `null`.
//     */
//    fun newSilentActionMessage(resource: Resourcepart?, text: String?, action: ChatAction?, chat: AbstractChat) {
//        val lastMessageTimestamp: Long? = getLastTimestampFromBackground(chat)
//        var silentTimestamp: Date? = null
//        if (lastMessageTimestamp != null) {
//            silentTimestamp = Date(lastMessageTimestamp + 1)
//        }
//        createAndSaveNewMessage(true, UUID.randomUUID().toString(), resource, text, null,
//                action, silentTimestamp, null, true, false, false, false,
//                null, null, null, null, null, false, null,
//                false, null, false)
//    }
//
//    private fun getLastTimestampFromBackground(chat: AbstractChat): Long? {
//        val timestamp: Long
//        val bgRealm = DatabaseManager.getInstance().defaultRealmInstance
//        val lastMessage: MessageRealmObject? =
//                bgRealm.where(MessageRealmObject::class.java)
//                        .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
//                        .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
//                        .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
//                        .isNotNull(MessageRealmObject.Fields.TEXT)
//                        .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
//                        .findAll()
//                        .last(null)
//        if (lastMessage != null && lastMessage.timestamp != null) {
//            timestamp = lastMessage.timestamp
//        } else if (lastActionTimestamp != null) {
//            timestamp = lastActionTimestamp
//        } else {
//            return null
//        }
//        if (Looper.myLooper() != Looper.getMainLooper()) bgRealm.close()
//        return timestamp
//    }

