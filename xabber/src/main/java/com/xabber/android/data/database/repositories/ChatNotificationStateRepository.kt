package com.xabber.android.data.database.repositories

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.ChatNotificationStateRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.NotificationState
import io.realm.Realm
import java.util.*


//todo MAY BE USELESS
object ChatNotificationStateRepository {

    private val LOG_TAG = this.javaClass.simpleName.toString()

    @JvmStatic
    fun saveChatNotificationState(accountJid: AccountJid, contactJid: ContactJid,
                                  notificationState: NotificationState) : String {
        val uuid = UUID.randomUUID().toString()
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm.executeTransaction {
                    it.insertOrUpdate(ChatNotificationStateRealmObject(uuid).apply {
                        setAccountJid(accountJid)
                        setContactJid(contactJid)
                        mode = notificationState.mode
                        timestamp = notificationState.timestamp
                    })
                }
            } catch (e: Exception){
                LogManager.exception(LOG_TAG, e)
            } finally {
                realm?.close()
            }
        }
        return uuid
    }

    @JvmStatic
    fun removeChatNotificationState(accountJid: AccountJid, contactJid: ContactJid){
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm.executeTransaction { realm1 ->
                    realm1.where(ChatNotificationStateRealmObject::class.java)
                            .equalTo(ChatNotificationStateRealmObject.Fields.ACCOUNT_JID, accountJid.bareJid.toString())
                            .equalTo(ChatNotificationStateRealmObject.Fields.CONTACT_JIO, contactJid.bareJid.toString())
                            .findAll()
                            .deleteAllFromRealm()
                }
            } catch (e: Exception){
                LogManager.exception(LOG_TAG, e)
            } finally {
                realm?.close()
            }
        }
    }

    @JvmStatic
    fun removeChatNotificationState(uuid: String){
        Application.getInstance().runInBackground {
            var realm: Realm? = null
            try {
                realm = DatabaseManager.getInstance().defaultRealmInstance
                realm.executeTransaction { realm1 ->
                    realm1.where(ChatNotificationStateRealmObject::class.java)
                            .equalTo(ChatNotificationStateRealmObject.Fields.ID, uuid)
                            .findAll()
                            .deleteAllFromRealm()
                }
            } catch (e: Exception){
                LogManager.exception(LOG_TAG, e)
            } finally {
                realm?.close()
            }
        }
    }

    @JvmStatic
    fun getChatNotificationState(uniqueId: String): NotificationState? {
        var mode: NotificationState.NotificationMode? = null
        var timestamp: Long? = null

        var realm: Realm? = null
        try{
            realm = DatabaseManager.getInstance().defaultRealmInstance

            val cnsro = realm.where(ChatNotificationStateRealmObject::class.java)
                    .equalTo(ChatNotificationStateRealmObject.Fields.ID, uniqueId)
                    .findFirst()

            mode = cnsro?.mode
            timestamp = cnsro?.timestamp

        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper()){
                realm?.close()
            }
        }

        return if (timestamp != null) NotificationState(mode, timestamp) else null
    }

    @JvmStatic
    fun getChatNotificationState(accountJid: AccountJid, contactJid: ContactJid): NotificationState? {
        var mode: NotificationState.NotificationMode? = null
        var timestamp: Long? = null

        var realm: Realm? = null
        try{
            realm = DatabaseManager.getInstance().defaultRealmInstance

            val cnsro = realm.where(ChatNotificationStateRealmObject::class.java)
                    .equalTo(ChatNotificationStateRealmObject.Fields.ACCOUNT_JID, accountJid.bareJid.toString())
                    .equalTo(ChatNotificationStateRealmObject.Fields.CONTACT_JIO, contactJid.bareJid.toString())
                    .findFirst()

            mode = cnsro?.mode
            timestamp = cnsro?.timestamp

        } catch (e: Exception){
            LogManager.exception(LOG_TAG, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper()){
                realm?.close()
            }
        }

        return if (timestamp != null) NotificationState(mode, timestamp) else null
    }

}