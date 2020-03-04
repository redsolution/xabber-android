package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.realmobjects.SyncInfoRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageRepository {

    private static final String LOG_TAG = MessageRepository.class.getSimpleName();

    public static RealmResults<MessageRealmObject> getChatMessages(AccountJid accountJid, UserJid userJid) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<MessageRealmObject> results = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageRealmObject.Fields.USER, userJid.toString())
                .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageRealmObject.Fields.TEXT)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return results;
    }

    public static void removeAllAccountMessagesFromRealm(){
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
                realm.executeTransaction(realm1 -> {
                    realm1.where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();

                    realm1.where(SyncInfoRealmObject.class)
                            .equalTo(SyncInfoRealmObject.FIELD_ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
