package com.xabber.android.data.database;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realm.DiscoveryInfoCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.annotations.RealmModule;

public class RealmManager {
    private static final String REALM_DATABASE_NAME = "realm_database.realm";
    private static final int REALM_DATABASE_VERSION = 1;
    private final RealmConfiguration realmConfiguration;

    private static RealmManager instance;

    public static RealmManager getInstance() {
        if (instance == null) {
            instance = new RealmManager();
        }

        return instance;
    }

    private RealmManager() {
        Realm.init(Application.getInstance());
        realmConfiguration = createRealmConfiguration();

        boolean success = Realm.compactRealm(realmConfiguration);
        System.out.println("Realm compact database file result: " + success);

    }

    public Realm getRealm() {
        return Realm.getInstance(realmConfiguration);
    }

    void deleteRealm() {
        Realm realm = getRealm();
        Realm.deleteRealm(realm.getConfiguration());
        realm.close();
    }

    @RealmModule(classes = {DiscoveryInfoCache.class})
    static class RealmDatabaseModule {
    }

    private RealmConfiguration createRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .name(REALM_DATABASE_NAME)
                .schemaVersion(REALM_DATABASE_VERSION)
                .modules(new RealmDatabaseModule())
                .build();
    }
}
