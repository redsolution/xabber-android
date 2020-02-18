package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.SavedStatus;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.database.realmobjects.StatusRealm;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

public class StatusRepository {

    public static Collection<SavedStatus> getAllSavedStatusesFromRealm(){
        final Collection<SavedStatus> savedStatusCollection = new ArrayList<>();

        Application.getInstance().runOnUiThread(() -> {
            RealmResults<StatusRealm> realObjectsList = Realm.getDefaultInstance()
                    .where(StatusRealm.class)
                    .findAll();

            for (StatusRealm statusRealm : realObjectsList){

                savedStatusCollection.add(new SavedStatus(StatusMode.fromString(statusRealm.getStatusMode())
                        , statusRealm.getStatusText()));
            }
        });

        return savedStatusCollection;
    }

    public static void saveStatusToRealm(SavedStatus savedStatus){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    StatusRealm statusRealm = new StatusRealm(savedStatus.getStatusMode().toString(),
                            savedStatus.getStatusText());
                    realm1.copyToRealm(statusRealm);
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
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(StatusRealm.class)
                            .equalTo(StatusRealm.Fields.STATUS_MODE, savedStatus.getStatusMode().toString())
                            .equalTo(StatusRealm.Fields.STATUS_TEXT, savedStatus.getStatusText())
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
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(StatusRealm.class)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("StatusRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
