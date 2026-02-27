package com.xabber.android.data.database.repositories;

import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.DeviceRealmObject;
import com.xabber.android.data.database.realmobjects.SyncStateRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;

public class SyncStateRepository {

    public static final String LOG_TAG = SyncStateRepository.class.getSimpleName();

    public static void removeSyncState(String accountJid) {
        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> {
                    realm1.where(SyncStateRealmObject.class)
                            .equalTo(SyncStateRealmObject.Fields.JID, accountJid)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }
}
