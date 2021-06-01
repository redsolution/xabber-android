package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.SavedStatus;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.StatusRealmObject;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

public class StatusRepository {

    public static Collection<SavedStatus> getAllSavedStatusesFromRealm(){
        Collection<SavedStatus> savedStatusCollection = new ArrayList<>();
        Realm realm = null;
        try{
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();

            RealmResults<StatusRealmObject> realObjectsList = realm
                    .where(StatusRealmObject.class)
                    .findAll();

            for (StatusRealmObject statusRealmObject : realObjectsList){
                savedStatusCollection.add(new SavedStatus(StatusMode.fromString(statusRealmObject.getStatusMode())
                        , statusRealmObject.getStatusText()));
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return savedStatusCollection;
    }

    public static void saveStatusToRealm(SavedStatus savedStatus){

        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    StatusRealmObject statusRealmObject = new StatusRealmObject(savedStatus.getStatusMode().toString(),
                            savedStatus.getStatusText());
                    realm1.copyToRealm(statusRealmObject);
                });
            } catch (Exception e){
                LogManager.exception("StatusRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void deleteSavedStatusFromRealm(SavedStatus savedStatus){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(StatusRealmObject.class)
                            .equalTo(StatusRealmObject.Fields.STATUS_MODE, savedStatus.getStatusMode().toString())
                            .equalTo(StatusRealmObject.Fields.STATUS_TEXT, savedStatus.getStatusText())
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("StatusRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void clearAllSavedStatusesInRealm(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(StatusRealmObject.class)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("StatusRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
