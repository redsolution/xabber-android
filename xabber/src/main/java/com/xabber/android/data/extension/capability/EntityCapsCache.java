package com.xabber.android.data.extension.capability;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.DiscoveryInfoRealmObject;
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

        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    DiscoveryInfoRealmObject discoveryInfoRealmObject = new DiscoveryInfoRealmObject(nodeVer, info);
                    realm1.copyToRealmOrUpdate(discoveryInfoRealmObject);
                });
            } catch (Exception e) {
                LogManager.exception("EntityCapsCache", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    @Override
    public DiscoverInfo lookup(String nodeVer) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        DiscoveryInfoRealmObject discoveryInfoRealmObject = realm
                .where(DiscoveryInfoRealmObject.class)
                .equalTo(DiscoveryInfoRealmObject.Fields.NODE_VER, nodeVer)
                .findFirst();

        DiscoverInfo discoverInfo = null;

        if (discoveryInfoRealmObject != null) {
            discoverInfo = discoveryInfoRealmObject.getDiscoveryInfo();
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return discoverInfo;
    }

    @Override
    public void emptyCache() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(DiscoveryInfoRealmObject.class)
                        .findAll()
                        .deleteAllFromRealm());
            } catch (Exception e) {
                LogManager.exception("EntityCapsCache", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
