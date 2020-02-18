package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.OtrRealm;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmResults;

public class OtrRepository {

    public static NestedNestedMaps<String, Boolean> getFingerprintsFromRealm(){
        final NestedNestedMaps<String, Boolean> fingerprints = new NestedNestedMaps<>();

        Application.getInstance().runOnUiThread(() -> {
            RealmResults<OtrRealm> realmResults = Realm.getDefaultInstance()
                    .where(OtrRealm.class)
                    .findAll();
            for (OtrRealm otrRealm : realmResults){
                fingerprints.put(otrRealm.getAccount(),
                        otrRealm.getUser(),
                        otrRealm.getFingerprint(),
                        otrRealm.isVerified());
            }
        });

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
