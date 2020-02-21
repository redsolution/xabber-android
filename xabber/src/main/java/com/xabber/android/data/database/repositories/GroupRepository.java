package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.GroupRealm;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.GroupConfiguration;
import com.xabber.android.data.roster.ShowOfflineMode;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupRepository {

    public static NestedMap<GroupConfiguration> getGroupConfigurationsFromRealm(){
        LogManager.d("GroupRepo", "getGroupConf");
        final NestedMap<GroupConfiguration> groupConfigurationNestedMap = new NestedMap<>();

        Application.getInstance().runOnUiThread(() -> {
            RealmResults<GroupRealm> groupRealmResults = Realm.getDefaultInstance()
                    .where(GroupRealm.class)
                    .findAll();
            for (GroupRealm groupRealm : groupRealmResults){
                GroupConfiguration groupConfiguration = new GroupConfiguration();
                groupConfiguration.setExpanded(groupRealm.isExpanded());
                groupConfiguration.setShowOfflineMode(groupRealm.getShowOfflineMode());
                groupConfigurationNestedMap.put(groupRealm.getAccount(), groupRealm.getGroupName(), groupConfiguration);
            }
        });

        return groupConfigurationNestedMap;
    }

    public static void saveGroupToRealm(final String account, final String group,
                                        final boolean expanded, final ShowOfflineMode showOfflineMode){
        LogManager.d("GroupRepo", "savegrouptorealm");
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(new GroupRealm(account, group, expanded, showOfflineMode));
                });
            } catch (Exception e){
                LogManager.exception("GroupRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
