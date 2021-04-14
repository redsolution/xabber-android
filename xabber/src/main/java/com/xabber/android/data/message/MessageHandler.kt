package com.xabber.android.data.message

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.references.ReferencesManager
import com.xabber.android.data.filedownload.DownloadManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.push.SyncManager
import com.xabber.android.ui.OnNewMessageListener
import com.xabber.xmpp.groups.hasGroupSystemMessage
import com.xabber.xmpp.sid.UniqueIdsHelper
import io.realm.Realm
import io.realm.Realm.Transaction.OnSuccess
import net.java.otr4j.io.SerializationUtils
import net.java.otr4j.io.messages.AbstractMessage
import net.java.otr4j.io.messages.PlainTextMessage
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.packet.DelayInformation
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.io.IOException
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
                            realm.executeTransactionAsync(
                                    Realm.Transaction { realm1: Realm -> realm1.copyToRealmOrUpdate(messagesList) },
                                    OnSuccess {
                                        Application.getInstance().runOnUiThread {
                                            Application.getInstance()
                                                    .getUIListeners(OnNewMessageListener::class.java)
                                                    .map(OnNewMessageListener::onNewMessage)
                                        }
                                        SyncManager.getInstance().onMessageSaved()
                                        checkForAttachmentsAndDownload(messagesList)
                                    }
                            )
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

    fun parseMessage(accountJid: AccountJid,
                     contactJid: ContactJid,
                     messageStanza: Message,
                     delayInformation: DelayInformation? = null
    ): MessageRealmObject? {

        var body = messageStanza.body
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
        val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(messageStanza)

        val stanzaId =
                if (groupchatUser != null || messageStanza.hasGroupSystemMessage()) {
                    UniqueIdsHelper.getStanzaIdBy(messageStanza, contactJid.bareJid.toString())
                } else UniqueIdsHelper.getStanzaIdBy(messageStanza, accountJid.bareJid.toString())

        val originId: String? = UniqueIdsHelper.getOriginId(messageStanza)

        val messageRealmObject =
                if (originId != null) MessageRealmObject.createMessageRealmObjectWithOriginId(accountJid, contactJid, originId)
                else MessageRealmObject.createMessageRealmObjectWithStanzaId(accountJid, contactJid, stanzaId)

        val timestamp = delayInformation?.stamp?.time
        val accountStartHistoryTimestamp = AccountManager.getInstance().getAccount(accountJid)?.startHistoryTimestamp

        messageRealmObject.apply {
            resource = user.jid.resourceOrNull
            text = body
            this.isIncoming = isIncoming
            markupText = markupBody
            delayTimestamp = delayInformation?.stamp?.time
            if (timestamp != null && accountStartHistoryTimestamp != null){
                isRead = timestamp <= accountStartHistoryTimestamp
            }

            if (isIncoming) {
                messageRealmObject.messageStatus = MessageStatus.NONE
            } else messageRealmObject.messageStatus = MessageStatus.DISPLAYED
            this.timestamp = timestamp
            this.stanzaId = stanzaId
            this.originId = UniqueIdsHelper.getOriginId(messageStanza)
            isEncrypted = encrypted
        }

        // attachments
        // FileManager.processFileMessage(messageRealmObject);
        val attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(messageStanza)
        if (attachmentRealmObjects.size > 0) messageRealmObject.attachmentRealmObjects = attachmentRealmObjects

        // forwarded
        messageRealmObject.originalStanza = messageStanza.toXML().toString()
        messageRealmObject.originalFrom = messageStanza.from.toString()

        // groupchat
        if (groupchatUser != null) {
            GroupMemberManager.getInstance().saveGroupUser(groupchatUser, contactJid.bareJid)
            messageRealmObject.groupchatUserId = groupchatUser.id
        } else if (messageStanza.hasGroupSystemMessage()) messageRealmObject.isGroupchatSystem = true

        return messageRealmObject
    }

    fun saveOrUpdateMessage(messageRealmObject: MessageRealmObject) = saverBuffer.onNext(messageRealmObject)







}
