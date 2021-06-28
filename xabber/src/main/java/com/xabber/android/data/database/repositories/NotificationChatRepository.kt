package com.xabber.android.data.database.repositories

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.NotificationChatRealmObject
import com.xabber.android.data.database.realmobjects.NotificationMessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.notification.MessageNotificationManager
import io.realm.Realm
import io.realm.RealmList

object NotificationChatRepository {

    private val LOG_TAG = this::class.java.simpleName

    fun saveOrUpdateToRealm(chat: MessageNotificationManager.Chat) {
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm?.executeTransaction { realm1: Realm ->
                    realm1.copyToRealmOrUpdate(NotificationChatRealmObject(chat.id).apply {
                        account = chat.accountJid
                        user = chat.contactJid
                        chatTitle = chat.chatTitle.toString()
                        notificationID = chat.notificationId
                        isGroupChat = chat.isGroupChat
                        privacyType = chat.privacyType
                        messages = RealmList<NotificationMessageRealmObject>().apply {
                            synchronized(chat.messages) {
                                addAll(chat.messages.map { chatMessage -> chatMessage.toRealmMessage() })
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
            } finally {
                realm?.close()
            }
        }
    }

    fun getAllNotificationChatsFromRealm(): List<MessageNotificationManager.Chat> {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw IllegalThreadStateException("This function must be called on background thread!")
        }
        val realm = DatabaseManager.getInstance().defaultRealmInstance
        val results = realm.where(NotificationChatRealmObject::class.java)
            .findAll()
            .map { realmObject ->
                MessageNotificationManager.getInstance().Chat(
                    realmObject.id, realmObject.account, realmObject.user, realmObject.notificationID,
                    realmObject.chatTitle, realmObject.isGroupChat, realmObject.privacyType
                ).apply {
                    messages.addAll(realmObject.messages.map { message ->
                        MessageNotificationManager.getInstance().Message(
                            message.id,
                            message.author,
                            message.text,
                            message.timestamp,
                            GroupMemberManager.getGroupMemberById(
                                realmObject.account, realmObject.user, message.memberId
                            )
                        )
                    })
                }
            }

        realm.close()
        return results
    }

    fun removeAllNotificationChatInRealm() {
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm?.executeTransaction { realm1: Realm ->
                    realm1.where(NotificationChatRealmObject::class.java)
                        .findAll()
                        .forEach {
                            it.messages.deleteAllFromRealm()
                            it.deleteFromRealm()
                        }
                }
            } catch (e: java.lang.Exception) {
                LogManager.exception(LOG_TAG, e)
            } finally {
                realm?.close()
            }
        }
    }

    fun removeNotificationChatsByAccountInRealm(accountJid: AccountJid) {
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm?.executeTransaction { realm1: Realm ->
                    realm1.where(NotificationChatRealmObject::class.java)
                        .equalTo(NotificationChatRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .findAll()
                        .forEach {
                            it.messages.deleteAllFromRealm()
                            it.deleteFromRealm()
                        }
                }
            } catch (e: java.lang.Exception) {
                LogManager.exception("MessageNotificationManager", e)
            } finally {
                realm?.close()
            }
        }
    }

    fun removeNotificationChatsForAccountAndContactInRealm(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm?.executeTransaction { realm1: Realm ->
                    realm1.where(NotificationChatRealmObject::class.java)
                        .equalTo(NotificationChatRealmObject.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(NotificationChatRealmObject.Fields.USER, contactJid.toString())
                        .findAll()
                        .forEach {
                            it.messages.deleteAllFromRealm()
                            it.deleteFromRealm()
                        }
                }
            } catch (e: java.lang.Exception) {
                LogManager.exception("MessageNotificationManager", e)
            } finally {
                realm?.close()
            }
        }
    }

    private fun MessageNotificationManager.Message.toRealmMessage() =
        NotificationMessageRealmObject(
            this.id, this.author.toString(), this.messageText.toString(),
            this.timestamp, this.groupMember.memberId
        )

}