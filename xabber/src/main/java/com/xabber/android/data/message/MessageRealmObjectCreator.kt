package com.xabber.android.data.message

import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.mam.NextMamManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.references.ReferencesManager
import com.xabber.android.data.log.LogManager
import com.xabber.xmpp.groups.hasGroupSystemMessage
import com.xabber.xmpp.sid.UniqueIdsHelper
import net.java.otr4j.io.SerializationUtils
import net.java.otr4j.io.messages.AbstractMessage
import net.java.otr4j.io.messages.PlainTextMessage
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.delay.packet.DelayInformation
import java.io.IOException

//fun parseMessage(accountJid: AccountJid,
//                 contactJid: ContactJid,
//                 stanza: Stanza,
//): MessageRealmObject {
//    if (stanza !is Message) throw ClassCastException("Incoming stanza does not contain any message!")
//
//    val delayInformation = stanza.delayInformation
//    val messageDelay = DelayInformation.from(stanza)
//
//    var body = stanza.body
//    val otrMessage: AbstractMessage? = try {
//        SerializationUtils.toMessage(body)
//    } catch (e: IOException) {
//        LogManager.exception(, e)
//        return null
//    }
//    var encrypted = false
//    if (otrMessage != null) {
//        if (otrMessage.messageType != AbstractMessage.MESSAGE_PLAINTEXT) {
//            encrypted = true
//            try {
//                // this transforming just decrypt message if have keys. No action as injectMessage or something else
//                body = OTRManager.getInstance().transformReceivingIfSessionExist(accountJid, contactJid, body)
//                if (OTRManager.getInstance().isEncrypted(body)) return null
//            } catch (e: Exception) {
//                LogManager.exception(NextMamManager.LOG_TAG, e)
//                return null
//            }
//        } else body = (otrMessage as PlainTextMessage).cleanText
//    }
//
//    // forward comment (to support previous forwarded xep)
//    val forwardComment = ForwardManager.parseForwardComment(stanza)
//    if (forwardComment != null) body = forwardComment
//
//    // modify body with references
//    val bodies = ReferencesManager.modifyBodyWithReferences(stanza, body)
//    body = bodies.first
//    val markupBody = bodies.second
//    val isIncoming = stanza.from.asBareJid().equals(contactJid.jid.asBareJid())
//    val groupchatUser = ReferencesManager.getGroupchatUserFromReferences(stanza)
//
//    val stanzaId =
//            if (groupchatUser != null || stanza.hasGroupSystemMessage()) {
//                UniqueIdsHelper.getStanzaIdBy(stanza, contactJid.bareJid.toString())
//            } else UniqueIdsHelper.getStanzaIdBy(stanza, accountJid.bareJid.toString())
//
//    val originId: String? = UniqueIdsHelper.getOriginId(stanza)
//
//    val messageRealmObject =
//            if (originId != null) MessageRealmObject.createMessageRealmObjectWithOriginId(accountJid, contactJid, originId)
//            else MessageRealmObject.createMessageRealmObjectWithStanzaId(accountJid, contactJid, stanzaId)
//
//    val timestamp = delayInformation.stamp.time
//
//    messageRealmObject.apply {
//        resource = user.jid.resourceOrNull
//        text = body
//        this.isIncoming = isIncoming
//        markupText = markupBody
//        delayTimestamp = messageDelay?.stamp?.time
//        isRead = timestamp <= accountItem!!.startHistoryTimestamp
//        if (isIncoming) {
//            messageRealmObject.messageStatus = MessageStatus.NONE
//        } else messageRealmObject.messageStatus = MessageStatus.DISPLAYED
//        this.timestamp = timestamp
//        this.stanzaId = stanzaId
//        this.originId = UniqueIdsHelper.getOriginId(stanza)
//        isEncrypted = encrypted
//    }
//
//    // attachments
//    // FileManager.processFileMessage(messageRealmObject);
//    val attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(stanza)
//    if (attachmentRealmObjects.size > 0) messageRealmObject.attachmentRealmObjects = attachmentRealmObjects
//
//    // forwarded
//    messageRealmObject.originalStanza = stanza.toXML().toString()
//    messageRealmObject.originalFrom = stanza.from.toString()
//
//    // groupchat
//    if (groupchatUser != null) {
//        GroupMemberManager.getInstance().saveGroupUser(groupchatUser, contactJid.bareJid, timestamp)
//        messageRealmObject.groupchatUserId = groupchatUser.id
//    } else if (stanza.hasGroupSystemMessage()) messageRealmObject.isGroupchatSystem = true
//
//    return messageRealmObject
//
//}