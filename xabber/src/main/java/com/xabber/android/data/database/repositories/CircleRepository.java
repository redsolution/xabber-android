package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.CircleRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.CircleConfiguration;
import com.xabber.android.data.roster.ShowOfflineMode;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class CircleRepository {

    public static NestedMap<CircleConfiguration> getGroupConfigurationsFromRealm(){
        NestedMap<CircleConfiguration> circleConfigurationNestedMap = new NestedMap<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<CircleRealmObject> circleRealmObjectResults = realm
                    .where(CircleRealmObject.class)
                    .findAll();
            for (CircleRealmObject circleRealmObject : circleRealmObjectResults){
                CircleConfiguration circleConfiguration = new CircleConfiguration();
                circleConfiguration.setExpanded(circleRealmObject.isExpanded());
                circleConfiguration.setShowOfflineMode(circleRealmObject.getShowOfflineMode());
                circleConfigurationNestedMap.put( circleRealmObject.getContacts().size() != 0 ? circleRealmObject.getContacts().get(0).getAccountJid() : "empty",
                        circleRealmObject.getCircleName(), circleConfiguration);
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }


        return circleConfigurationNestedMap;
    }

    public static void saveGroupToRealm(final String account, final String group,
                                        final boolean expanded, final ShowOfflineMode showOfflineMode){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<ContactRealmObject> contactRealmObjectRealmResults = realm1
                            .where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, account)
                            .findAll();
                    RealmList<ContactRealmObject> contactRealmObjectRealmList = new RealmList<>();
                    contactRealmObjectRealmList.addAll(contactRealmObjectRealmResults);
                    realm1.copyToRealmOrUpdate(new CircleRealmObject(contactRealmObjectRealmList, group, expanded, showOfflineMode));
                });
            } catch (Exception e){
                LogManager.exception("GroupRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
