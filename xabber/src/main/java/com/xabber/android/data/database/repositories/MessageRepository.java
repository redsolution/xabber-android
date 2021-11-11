package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageRepository {

    private static final String LOG_TAG = MessageRepository.class.getSimpleName();

    public static RealmResults<MessageRealmObject> getChatMessages(AccountJid accountJid, ContactJid contactJid) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<MessageRealmObject> results = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageRealmObject.Fields.USER, contactJid.toString())
                .equalTo(MessageRealmObject.Fields.FORWARDED, false)
                .isNotNull(MessageRealmObject.Fields.TEXT)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return results;
    }

    public static RealmResults<MessageRealmObject> getGroupMemberMessages(
            AccountJid accountJid, ContactJid contactJid, String groupMemberId
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException("Method must be invoked from UI thread or rewrited to return copies from background thread");
        }
        return DatabaseManager.getInstance().getDefaultRealmInstance()
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageRealmObject.Fields.USER, contactJid.toString())
                .equalTo(MessageRealmObject.Fields.GROUPCHAT_USER_ID, groupMemberId)
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.TEXT)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
    }

    public static RealmResults<MessageRealmObject> getGroupChatMessages(AccountJid accountJid, ContactJid contactJid) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<MessageRealmObject> results = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageRealmObject.Fields.USER, contactJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.TEXT)
                .isNull(MessageRealmObject.Fields.ACTION)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return results;
    }

    public static void removeAllAccountMessagesFromRealm(){
        //LogManager.i("MessageRepository()", "Removing all history for account " + accountItem.account)
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(MessageRealmObject.class).findAll().deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("messageRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void removeAccountMessagesFromRealm(final AccountJid account) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(MessageRealmObject.class)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, account.toString())
                        .findAll()
                        .deleteAllFromRealm());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    public static MessageRealmObject getMessageFromRealmByPrimaryKey(String primaryKey){
        if (Looper.getMainLooper() == Looper.myLooper())
            return DatabaseManager.getInstance().getDefaultRealmInstance()
                    .where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, primaryKey)
                    .findFirst();
        else {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            MessageRealmObject messageRealmObject = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, primaryKey)
                    .findFirst();
            MessageRealmObject result = null;
            if (messageRealmObject != null) result = realm.copyFromRealm(messageRealmObject);
            realm.close();
            return result;
        }
    }

    public static MessageRealmObject getMessageFromRealmByStanzaId(String stanzaId){
        if (Looper.getMainLooper() == Looper.myLooper())
            return DatabaseManager.getInstance().getDefaultRealmInstance()
                    .where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId)
                    .findFirst();
        else {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            MessageRealmObject messageRealmObject = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId)
                    .findFirst();
            MessageRealmObject result = null;
            if (messageRealmObject != null) result = realm.copyFromRealm(messageRealmObject);
            realm.close();
            return result;
        }
    }

    public static List<MessageRealmObject> getForwardedMessages(MessageRealmObject messageRealmObject) {
        if (!Arrays.asList(messageRealmObject.getForwardedIdsAsArray()).contains(null)) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                return DatabaseManager.getInstance().getDefaultRealmInstance()
                        .where(MessageRealmObject.class)
                        .in(MessageRealmObject.Fields.PRIMARY_KEY, messageRealmObject.getForwardedIdsAsArray())
                        .findAll()
                        .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
            }
            else {
                Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                List<MessageRealmObject> result = realm.copyFromRealm(
                        realm.where(MessageRealmObject.class)
                                .in(MessageRealmObject.Fields.PRIMARY_KEY, messageRealmObject.getForwardedIdsAsArray())
                                .findAll()
                                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING));
                realm.close();
                return result;
            }
        } else return null;
    }

}
