package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.database.realmobjects.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageRepository {

    private static final String LOG_TAG = MessageRepository.class.getSimpleName();

    public static RealmResults<MessageItem> getChatMessages(AccountJid accountJid, UserJid userJid) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<MessageItem> results = realm
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageItem.Fields.USER, userJid.toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.TEXT)
                .findAll()
                .sort(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return results;
    }

    public static void removeAllAccountMessagesFromRealm(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(MessageItem.class).findAll().deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("messageRepository", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static void removeAccountMessagesFromRealm(final AccountJid account) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(MessageItem.class)
                            .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();

                    realm1.where(SyncInfo.class)
                            .equalTo(SyncInfo.FIELD_ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

}
