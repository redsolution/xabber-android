package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.OtrRealmObject;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmResults;

public class OtrRepository {

    public static NestedNestedMaps<String, Boolean> getFingerprintsFromRealm(){
        NestedNestedMaps<String, Boolean> fingerprints = new NestedNestedMaps<>();
        Realm realm = null;
        try{
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<OtrRealmObject> realmResults = realm
                    .where(OtrRealmObject.class)
                    .findAll();
            for (OtrRealmObject otrRealmObject : realmResults){
                fingerprints.put(otrRealmObject.getAccount(),
                        otrRealmObject.getUser(),
                        otrRealmObject.getFingerprint(),
                        otrRealmObject.isVerified());
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return fingerprints;
    }

    public static void saveOtrToRealm(final String account, final String user,
                                      final String fingerprint, final boolean verified){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealm(new OtrRealmObject(account, user, fingerprint, verified));
                });
            } catch (Exception e){
                LogManager.exception("OtrRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
