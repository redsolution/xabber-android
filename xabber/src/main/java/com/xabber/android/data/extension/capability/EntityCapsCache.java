package com.xabber.android.data.extension.capability;

import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.DiscoveryInfoCache;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import io.realm.Realm;


class EntityCapsCache implements EntityCapsPersistentCache {

    private static final String LOG_TAG = EntityCapsCache.class.getSimpleName();

    @Override
    public void addDiscoverInfoByNodePersistent(final String nodeVer, final DiscoverInfo info) {
        final long startTime = System.currentTimeMillis();
        if (nodeVer == null || info == null) {
            return;
        }

        // TODO: 13.03.18 ANR - WRITE
        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();

        DiscoveryInfoCache discoveryInfoCache = new DiscoveryInfoCache(nodeVer, info);
        realm.copyToRealmOrUpdate(discoveryInfoCache);

        realm.commitTransaction();
        realm.close();
        LogManager.d("REALM", Thread.currentThread().getName()
                + " save discover info: " + (System.currentTimeMillis() - startTime));

    }

    @Override
    public DiscoverInfo lookup(String nodeVer) {
        Realm realm = RealmManager.getInstance().getNewRealm();

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
        final long startTime = System.currentTimeMillis();
        // TODO: 13.03.18 ANR - WRITE
        Realm realm = RealmManager.getInstance().getNewRealm();

        realm.beginTransaction();
        realm.where(DiscoveryInfoCache.class)
                .findAll()
                .deleteAllFromRealm();
        realm.commitTransaction();

        realm.close();
        LogManager.d("REALM", Thread.currentThread().getName()
                + " delete discover cache: " + (System.currentTimeMillis() - startTime));
    }
}
