package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.RecentSearchRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class RecentSearchRealmObjectRepository {

    private static final String LOG_TAG = RecentSearchRealmObjectRepository.class.getSimpleName();

    public static void itemWasSearched(AccountJid accountJid, ContactJid contactJid) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RecentSearchRealmObject result = realm1
                            .where(RecentSearchRealmObject.class)
                            .equalTo(RecentSearchRealmObject.Fields.ACCOUNT_JID,
                                    accountJid.toString())
                            .equalTo(RecentSearchRealmObject.Fields.CONTACT_JID,
                                    contactJid.getBareJid().toString())
                            .findFirst();
                    if (result != null && result.getTimestamp() != null
                            && result.getTimestamp() != 0) {
                        result.setTimestamp(System.currentTimeMillis());
                        realm1.copyToRealmOrUpdate(result);
                    } else {
                        result = new RecentSearchRealmObject();
                        result.setAccountJid(accountJid.toString());
                        result.setContactJid(contactJid.getBareJid().toString());
                        result.setTimestamp(System.currentTimeMillis());
                        realm1.copyToRealmOrUpdate(result);
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null)
                    realm.close();
            }
        });
    }

    public static ArrayList<RecentSearchRealmObject> getAllRecentSearchRealmObjects() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<RecentSearchRealmObject> realmObjects = realm
                .where(RecentSearchRealmObject.class)
                .isNotNull(RecentSearchRealmObject.Fields.TIMESTAMP)
                .findAll()
                .sort(RecentSearchRealmObject.Fields.TIMESTAMP, Sort.DESCENDING);
        ArrayList<RecentSearchRealmObject> result = new ArrayList<>(realmObjects);
        if (Looper.getMainLooper() != Looper.myLooper())
            realm.close();
        return result;
    }

    public static void clearAllRecentSearched() {
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 -> {
                realm1.where(RecentSearchRealmObject.class)
                        .findAll()
                        .deleteAllFromRealm();
            });
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.getMainLooper() != Looper.myLooper())
                realm.close();
        }
    }
}
