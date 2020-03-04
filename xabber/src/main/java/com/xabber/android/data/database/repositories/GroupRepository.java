package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupRealmObject;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.GroupConfiguration;
import com.xabber.android.data.roster.ShowOfflineMode;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupRepository {

    public static NestedMap<GroupConfiguration> getGroupConfigurationsFromRealm(){
        NestedMap<GroupConfiguration> groupConfigurationNestedMap = new NestedMap<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<GroupRealmObject> groupRealmObjectResults = realm
                    .where(GroupRealmObject.class)
                    .findAll();
            for (GroupRealmObject groupRealmObject : groupRealmObjectResults){
                GroupConfiguration groupConfiguration = new GroupConfiguration();
                groupConfiguration.setExpanded(groupRealmObject.isExpanded());
                groupConfiguration.setShowOfflineMode(groupRealmObject.getShowOfflineMode());
                groupConfigurationNestedMap.put(groupRealmObject.getAccount(), groupRealmObject.getGroupName(), groupConfiguration);
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }


        return groupConfigurationNestedMap;
    }

    public static void saveGroupToRealm(final String account, final String group,
                                        final boolean expanded, final ShowOfflineMode showOfflineMode){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(new GroupRealmObject(account, group, expanded, showOfflineMode));
                });
            } catch (Exception e){
                LogManager.exception("GroupRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
