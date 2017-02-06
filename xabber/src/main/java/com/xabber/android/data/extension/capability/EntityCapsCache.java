package com.xabber.android.data.extension.capability;

import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.DiscoveryInfoCache;

import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import io.realm.Realm;


class EntityCapsCache implements EntityCapsPersistentCache {

    private static final String LOG_TAG = EntityCapsCache.class.getSimpleName();

    @Override
    public void addDiscoverInfoByNodePersistent(final String nodeVer, final DiscoverInfo info) {
        if (nodeVer == null || info == null) {
            return;
        }

        Realm realm = RealmManager.getInstance().getRealm();
        realm.beginTransaction();

        DiscoveryInfoCache discoveryInfoCache = new DiscoveryInfoCache(nodeVer, info);
        realm.copyToRealmOrUpdate(discoveryInfoCache);

        realm.commitTransaction();
        realm.close();
    }

    @Override
    public DiscoverInfo lookup(String nodeVer) {
        Realm realm = RealmManager.getInstance().getRealm();

        DiscoveryInfoCache discoveryInfoCache = realm.where(DiscoveryInfoCache.class)
                .equalTo(DiscoveryInfoCache.Fields.NODE_VER, nodeVer)
                .findFirst();

        DiscoverInfo discoverInfo = null;

        if (discoveryInfoCache != null) {
            discoverInfo = realm.copyFromRealm(discoveryInfoCache).getDiscoveryInfo();
        }

        realm.close();

        return discoverInfo;
    }

    @Override
    public void emptyCache() {
        Realm realm = RealmManager.getInstance().getRealm();

        realm.beginTransaction();
        realm.where(DiscoveryInfoCache.class)
                .findAll()
                .deleteAllFromRealm();
        realm.commitTransaction();

        realm.close();
    }
}
