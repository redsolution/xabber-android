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
import com.xabber.android.data.extension.chat_markers.ChatMarkerManager
import com.xabber.android.data.extension.chat_markers.ChatMarkersElements
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.references.ReferencesManager
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement
import com.xabber.android.data.extension.reliablemessagedelivery.hasTimeElement
import com.xabber.android.data.filedownload.DownloadManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.*
import com.xabber.android.data.notification.MessageNotificationManager
import com.xabber.android.data.notification.NotificationManager
import com.xabber.android.data.push.SyncManager
import com.xabber.android.data.xaccount.XMPPAuthManager
import com.xabber.android.ui.OnNewIncomingMessageListener
import com.xabber.android.ui.OnNewMessageListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.android.utils.StringUtils
import com.xabber.xmpp.groups.hasGroupSystemMessage
import com.xabber.xmpp.sid.UniqueIdsHelper
import com.xabber.xmpp.uuu.ChatStateExtension
import io.realm.Realm
import io.realm.RealmList
import net.java.otr4j.io.SerializationUtils
import net.java.otr4j.io.messages.AbstractMessage
import net.java.otr4j.io.messages.PlainTextMessage
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jxmpp.jid.parts.Resourcepart
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
                    if (!messagesList.isNullOrEmpty()){
                        var realm: Realm? = null
                        try {
                            realm = DatabaseManager.getInstance().defaultRealmInstance
                            realm.executeTransaction { realm1 -> realm1.copyToRealmOrUpdate(messagesList) }
                            notifySamUiListeners(OnNewMessageListener::class.java)
                            SyncManager.getInstance().onMessageSaved()
                            checkForAttachmentsAndDownload(messagesList)
                        } catch (e: Exception) {
                            LogManager.exception(this, e)
                        } finally {
                            if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close()
                        }
                    }
                }
    }

    private fun checkForAttachmentsAndDownload(messageRealmObjects: List<MessageRealmObject>) {
        if (SettingsManager.chatsAutoDownloadVoiceMessage()) {
            messageRealmObjects
                    .flatMap { message ->
                        message.attachmentRealmObjects.map { attach -> Pair(attach, message.account) } }
                    .filter { pair: Pair<AttachmentRealmObject, AccountJid> ->
                        pair.first.isVoice && pair.first.filePath == null }
                    .map { pair: Pair<AttachmentRealmObject, AccountJid> ->
                        DownloadManager.getInstance().downloadFile(pair.first, pair.second, Application.getInstance()) }
        }
    }

    fun parseMessage(
            accountJid: AccountJid,
            contactJid: ContactJid,
            messageStanza: Message,
            delayInformation: DelayInformation? = null,
            isCarbons: Boolean = false,
    ) {

        //todo parse carbons chat markers
        //todo parse chat states
        //todo parse headlines maybe
        //todo parse group invites
        
        if (messageStanza.hasExtension(ChatStateExtension.NAMESPACE)) return
        if (messageStanza.hasExtension(ChatMarkersElements.NAMESPACE)
                && !messageStanza.hasExtension(ChatMarkersElements.MarkableExtension.ELEMENT, ChatMarkersElements.NAMESPACE)){
                    return
        }

        if (messageStanza.type == Message.Type.error) return

        if (delayInformation != null && "Offline Storage" == delayInformation.reason) return

        if (messageStanza.type == Message.Type.headline
                && XMPPAuthManager.getInstance().isXabberServiceMessage(messageStanza.stanzaId)) {
            return
        }

        val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(messageStanza)
        val isGroupSystem = messageStanza.hasGroupSystemMessage()

        if (ChatManager.getInstance().getChat(accountJid, contactJid) == null){
            if (groupchatUser != null || isGroupSystem){
                ChatManager.getInstance().createGroupChat(accountJid, contactJid)
            } else ChatManager.getInstance().createRegularChat(accountJid, contactJid)
        } else if (groupchatUser != null || isGroupSystem){
            ChatManager.getInstance().convertRegularToGroup(accountJid, contactJid)
        }

        val chat = ChatManager.getInstance().getChat(accountJid, contactJid)

        val resource: Resourcepart? = messageStanza.from.resourceOrNull

        if (resource != null && resource != Resourcepart.EMPTY) {
            if (chat is GroupChat) {
                chat.resource = resource.toString()
            } else if (chat is RegularChat) chat.resource = resource
        }

        val thread = messageStanza.thread ?: null

        chat?.threadId = thread
        //todo get resourcepart and update abstractchat resource

        var body = messageStanza.getOptimalTextBody()
        val otrMessage: AbstractMessage? = try {
            SerializationUtils.toMessage(body)
        } catch (e: IOException) {
            LogManager.exception(this, e)
            return
        }
        var encrypted = false
        if (otrMessage != null) {
            if (otrMessage.messageType != AbstractMessage.MESSAGE_PLAINTEXT) {
                encrypted = true
                try {
                    // this transforming just decrypt message if have keys. No action as injectMessage or something else
                    body = OTRManager.getInstance().transformReceivingIfSessionExist(accountJid, contactJid, body)
                    if (OTRManager.getInstance().isEncrypted(body)) return
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    return
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
                if (groupchatUser != null || messageStanza.hasGroupSystemMessage()) {
                    UniqueIdsHelper.getStanzaIdBy(messageStanza, contactJid.bareJid.toString())
                } else UniqueIdsHelper.getStanzaIdBy(messageStanza, accountJid.bareJid.toString())

        val accountStartHistoryTimestamp = AccountManager.getInstance().getAccount(accountJid)?.startHistoryTimestamp

        // FileManager.processFileMessage(messageRealmObject);
        val attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(messageStanza)

        var timestamp: Date? = null
        if (messageStanza.hasTimeElement()){
            timestamp = StringUtils.parseReceivedReceiptTimestampString(
                    messageStanza.getExtension<TimeElement>(TimeElement.ELEMENT, TimeElement.NAMESPACE).timeStamp)
        }

        val id = UUID.randomUUID().toString();

        // groupchat
        if (groupchatUser != null) GroupMemberManager.getInstance().saveGroupUser(groupchatUser, contactJid.bareJid)

        if (attachmentRealmObjects.size > 0){
            val messageRealmObject = createMessageItem(
                    uid = id,
                    resource = resource,
                    text = body,
                    markupText = markupBody,
                    action = null,
                    timestamp = timestamp,
                    delayTimestamp = DelayInformation.from(messageStanza)?.stamp,
                    incoming = isIncoming,
                    notify = true,
                    encrypted = encrypted,
                    offline = MessageManager.isOfflineMessage(accountJid.fullJid.domain, messageStanza),
                    stanzaId = stanzaId,
                    originId = UniqueIdsHelper.getOriginId(messageStanza),
                    attachmentRealmObjects = attachmentRealmObjects,
                    originalStanza = messageStanza.toXML().toString(),
                    parentMessageId = null,
                    originalFrom = messageStanza.from.toString(),
                    isForwarded = false,
                    forwardIdRealmObjects = parseForwardedMessage(messageStanza, id, chat!!),
                    fromMAM = false,
                    groupchatUserId = groupchatUser?.id,
                    chat = chat,
            )

            saverBuffer.onNext(messageRealmObject ?: return)
        } else {
            val messageRealmObject = createMessageItem(uid = id, resource = resource, text = body, action = null,
                    markupText = markupBody,  timestamp = timestamp, encrypted = encrypted,  stanzaId = stanzaId,
                    delayTimestamp = DelayInformation.from(messageStanza)?.stamp, incoming = isIncoming, notify = true,
                    offline = MessageManager.isOfflineMessage(accountJid.fullJid.domain, messageStanza),
                    originId = UniqueIdsHelper.getOriginId(messageStanza), groupchatUserId = groupchatUser?.id,
                    originalStanza = messageStanza.toXML().toString(), parentMessageId = null, fromMAM = false,
                    originalFrom = messageStanza.from.toString(), isForwarded = false, attachmentRealmObjects = null,
                    forwardIdRealmObjects = parseForwardedMessage(messageStanza, id, chat!!), chat = chat,
                    isGroupchatSystem = isGroupSystem,
            )

            saverBuffer.onNext(messageRealmObject ?: return)

        }

        Application.getInstance().runOnUiThread {
            Application.getInstance().getUIListeners(OnNewIncomingMessageListener::class.java)
                    .map{ listener -> listener.onNewIncomingMessage(accountJid, contactJid) }
        }

        notifySamUiListeners(OnNewMessageListener::class.java)

    }

    fun parseForwardedMessage(packet: Stanza,
                              parentMessageId: String,
                              chat: AbstractChat,
    ): RealmList<ForwardIdRealmObject>? {
        var forwarded = ReferencesManager.getForwardedFromReferences(packet)
        if (forwarded.isEmpty()) forwarded = ForwardManager.getForwardedFromStanza(packet)
        if (forwarded.isEmpty()) return null
        val forwardedIds = RealmList<ForwardIdRealmObject>()
        for (forward in forwarded) {
            val stanza = forward.forwardedStanza
            val delayInformation = forward.delayInformation
            val timestamp = delayInformation.stamp
            if (stanza is Message) {
                forwardedIds.add(ForwardIdRealmObject(parseInnerMessage(stanza, timestamp, parentMessageId, chat)))
            }
        }
        return forwardedIds
    }

    private fun parseInnerMessage(message: Message,
                                  timestamp: Date?,
                                  parentMessageId: String?,
                                  chat: AbstractChat,
    ): String? {

        if (message.type == Message.Type.error) return null

        val fromJid = message.from
        val resource: Resourcepart? = fromJid?.resourceOrNull

        var text: String? = message.body ?: return null
        val encrypted = OTRManager.getInstance().isEncrypted(text)
        val attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message)
        val uid = UUID.randomUUID().toString()
        val forwardIdRealmObjects: RealmList<ForwardIdRealmObject>? = parseForwardedMessage(message, uid, chat)
        val originalStanza = message.toXML().toString()
        var originalFrom = ""
        if (fromJid != null) originalFrom = fromJid.toString()

        // groupchat
        var groupchatUserId: String? = null
        val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message)
        if (groupchatUser != null) {
            groupchatUserId = groupchatUser.id
            GroupMemberManager.getInstance().saveGroupUser(groupchatUser, message.from.asBareJid())
        }

        // forward comment (to support previous forwarded xep)
        val forwardComment = ForwardManager.parseForwardComment(message)
        if (forwardComment != null && forwardComment.isNotEmpty()) text = forwardComment

        // modify body with references
        val bodies = ReferencesManager.modifyBodyWithReferences(message, text)
        text = bodies.first
        val markupText = bodies.second
        val isGroupSystem = message.hasGroupSystemMessage()

        // create message with file-attachments
        val messageRealmObject =
                if (attachmentRealmObjects.size > 0){
                    createMessageItem(uid = uid, resource = resource, text = text, markupText = markupText,
                            action = null, timestamp = timestamp, delayTimestamp = DelayInformation.from(message)?.stamp,
                            incoming = true, notify = false, encrypted = encrypted, offline = false, isForwarded = true,
                            stanzaId = UniqueIdsHelper.getStanzaIdBy(message, chat.contactJid.bareJid.toString()),
                            originId = UniqueIdsHelper.getOriginId(message), chat = chat, fromMAM = true,
                            attachmentRealmObjects = attachmentRealmObjects, originalFrom = originalFrom,
                            originalStanza = message.toXML().toString(), parentMessageId = parentMessageId,
                            forwardIdRealmObjects = forwardIdRealmObjects, groupchatUserId = groupchatUserId,
                    )
        } else {
            createMessageItem(uid = uid, resource = resource, text = text, markupText = markupText, action = null,
                    timestamp = timestamp, delayTimestamp = DelayInformation.from(message)?.stamp, offline = false,
                    incoming = true, notify = false, isGroupchatSystem = isGroupSystem, chat = chat, fromMAM = true,
                    encrypted = encrypted,  originalFrom = originalFrom, isForwarded = true,
                    stanzaId = UniqueIdsHelper.getStanzaIdBy(message, chat.contactJid.bareJid.toString()),
                    originId = UniqueIdsHelper.getOriginId(message), originalStanza = message.toXML().toString(),
                    parentMessageId = parentMessageId, forwardIdRealmObjects = forwardIdRealmObjects,
                    groupchatUserId = groupchatUserId,
            )
        }
        if (messageRealmObject != null) saverBuffer.onNext(messageRealmObject)
        return uid
    }

    fun createMessageItem(uid: String? = UUID.randomUUID().toString(),
                          resource: Resourcepart?,
                          text: String?,
                          markupText: String?,
                          action: ChatAction?,
                          timestamp: Date?,
                          delayTimestamp: Date?,
                          incoming: Boolean,
                          notify: Boolean,
                          encrypted: Boolean,
                          offline: Boolean,
                          stanzaId: String?,
                          originId: String? = UUID.randomUUID().toString(),
                          attachmentRealmObjects: RealmList<AttachmentRealmObject?>? = null,
                          originalStanza: String?,
                          parentMessageId: String?,
                          originalFrom: String?,
                          isForwarded: Boolean,
                          forwardIdRealmObjects: RealmList<ForwardIdRealmObject>?,
                          fromMAM: Boolean = false,
                          groupchatUserId: String? = null,
                          isGroupchatSystem: Boolean = false,
                          chat: AbstractChat,
    ): MessageRealmObject? {


        // from message archivesaving:
//        delayTimestamp = delayInformation?.stamp?.time
//        if (timestamp != null && accountStartHistoryTimestamp != null){
//            isRead = timestamp <= accountStartHistoryTimestamp
//        }
//
//        if (isIncoming) {
//            messageRealmObject.messageStatus = MessageStatus.NONE
//        } else messageRealmObject.messageStatus = MessageStatus.DISPLAYED

        //
        var messageText = text
        var messageTimestamp = timestamp
        var isNotify = notify

        val visible = ChatManager.getInstance().isVisibleChat(chat)
        val read = !incoming
        if (messageText == null) messageText = " "
        if (messageTimestamp == null) messageTimestamp = Date()
        if (messageText.trim { it <= ' ' }.isEmpty()
                && (forwardIdRealmObjects == null || forwardIdRealmObjects.isEmpty())
                && (attachmentRealmObjects == null || attachmentRealmObjects.isEmpty())) {
            isNotify = false
        }
        if (isNotify || !incoming) chat.openChat()
        if (!incoming) isNotify = false
        val messageRealmObject =
                if (stanzaId != null) {
                    MessageRealmObject.createMessageRealmObjectWithStanzaId(chat.account, chat.contactJid, uid)
                } else MessageRealmObject.createMessageRealmObjectWithOriginId(chat.account, chat.contactJid, uid)

        messageRealmObject.apply {
            this.text = messageText
            this.timestamp = messageTimestamp.time
            this.isIncoming = incoming
            this.isRead = fromMAM || read || !isGroupchatSystem || action != null
            this.isEncrypted = encrypted
            this.isOffline = offline
            this.stanzaId = stanzaId
            this.originId = originId
            this.originalStanza = originalStanza
            this.originalFrom = originalFrom
            this.parentMessageId = parentMessageId
            this.isForwarded = isForwarded
            this.isGroupchatSystem = isGroupchatSystem
            this.resource = resource ?: Resourcepart.EMPTY

            this.messageStatus = if (incoming) MessageStatus.NONE else MessageStatus.NOT_SENT

            if (action != null) this.action = action.toString()
            if (markupText != null) this.markupText = markupText
            if (delayTimestamp != null) this.delayTimestamp = delayTimestamp.time
            if (attachmentRealmObjects != null) this.attachmentRealmObjects = attachmentRealmObjects
            if (forwardIdRealmObjects != null) this.forwardedIds = forwardIdRealmObjects
            if (groupchatUserId != null) this.groupchatUserId = groupchatUserId
        }

        // remove notifications if get outgoing message with 2 sec delay
        if (!incoming) MessageNotificationManager.getInstance().removeChatWithTimer(chat.account, chat.contactJid)

        // when getting new message, unarchive chat if chat not muted
        if (chat.notifyAboutMessage()) chat.isArchived = false

        // update last id in chat
        chat.lastMessageId = messageRealmObject.stanzaId

        // notification
        chat.enableNotificationsIfNeed()
        if (isNotify && chat.notifyAboutMessage() && !visible && !isGroupchatSystem) {
            NotificationManager.getInstance().onMessageNotification(messageRealmObject)
        }
        return if (action != null && (groupchatUserId != null || isGroupchatSystem)) null else messageRealmObject
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

}
