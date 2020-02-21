package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.OtrRealm;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmResults;

public class OtrRepository {

    public static NestedNestedMaps<String, Boolean> getFingerprintsFromRealm(){
        NestedNestedMaps<String, Boolean> fingerprints = new NestedNestedMaps<>();
        Realm realm = null;
        try{
            realm = Realm.getDefaultInstance();
            RealmResults<OtrRealm> realmResults = realm
                    .where(OtrRealm.class)
                    .findAll();
            for (OtrRealm otrRealm : realmResults){
                fingerprints.put(otrRealm.getAccount(),
                        otrRealm.getUser(),
                        otrRealm.getFingerprint(),
                        otrRealm.isVerified());
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return fingerprints;
    }

    public static void saveOtrToRealm(final String account, final String user,
                                      final String fingerprint, final boolean verified){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealm(new OtrRealm(account, user, fingerprint, verified));
                });
            } catch (Exception e){
                LogManager.exception("OtrRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
