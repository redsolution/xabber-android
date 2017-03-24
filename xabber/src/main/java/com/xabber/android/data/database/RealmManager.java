package com.xabber.android.data.database;

import android.database.Cursor;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realm.AccountRealm;
import com.xabber.android.data.database.realm.DiscoveryInfoCache;
import com.xabber.android.data.database.sqlite.AccountTable;
import com.xabber.android.data.log.LogManager;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;
import io.realm.annotations.RealmModule;

public class RealmManager {
    private static final String REALM_DATABASE_NAME = "realm_database.realm";
    private static final int REALM_DATABASE_VERSION = 5;
    private static final String LOG_TAG = RealmManager.class.getSimpleName();
    private final RealmConfiguration realmConfiguration;

    private static RealmManager instance;

    private Realm realmUiThread;

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

    void deleteRealm() {
        Realm realm = getNewBackgroundRealm();
        Realm.deleteRealm(realm.getConfiguration());
        realm.close();
    }

    @RealmModule(classes = {DiscoveryInfoCache.class, AccountRealm.class})
    static class RealmDatabaseModule {
    }

    private RealmConfiguration createRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .name(REALM_DATABASE_NAME)
                .schemaVersion(REALM_DATABASE_VERSION)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        RealmSchema schema = realm.getSchema();

                        if (oldVersion == 2) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .setRequired(AccountRealm.Fields.ID, true);

                            oldVersion++;
                        }

                        if (oldVersion == 3) {
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField(AccountRealm.Fields.CLEAR_HISTORY_ON_EXIT, boolean.class);
                            schema.get(AccountRealm.class.getSimpleName())
                                    .addField(AccountRealm.Fields.MAM_DEFAULT_BEHAVIOR, String.class);

                            oldVersion++;
                        }

                        if (oldVersion == 4) {
                            schema.get(AccountRealm.class.getSimpleName()).
                                    addField(AccountRealm.Fields.LOAD_HISTORY_SETTINGS, String.class);

                            oldVersion++;
                        }
                    }
                })
                .modules(new RealmDatabaseModule())
                .build();
    }

    /**
     * Creates new realm instance for use from background thread.
     * Realm should be closed after use.
     *
     * @return new realm instance
     * @throws IllegalStateException if called from UI (main) thread
     */
    public Realm getNewBackgroundRealm() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Request background thread message realm from UI thread");
        }

        return getNewRealm();
    }

    /**
     * Creates new realm instance for use from any thread. Better to avoid this method.
     * Realm should be closed after use.
     *
     * @return new realm instance
     */
    public Realm getNewRealm() {
        return Realm.getInstance(realmConfiguration);
    }

    /**
     * Returns realm instance for use from UI (main) thread.
     * Do not close realm after use!
     *
     * @return realm instance for UI thread
     * @throws IllegalStateException if called from background thread
     */
    public Realm getRealmUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Request UI thread message realm from non UI thread");
        }

        if (realmUiThread == null) {
            realmUiThread = Realm.getInstance(realmConfiguration);
        }

        return realmUiThread;
    }


    void copyDataFromSqliteToRealm() {
        Realm realm = getNewBackgroundRealm();

        realm.beginTransaction();

        LogManager.i(LOG_TAG, "copying from SQLite to Realm");
        long counter = 0;
        Cursor cursor = AccountTable.getInstance().list();
        while (cursor.moveToNext()) {
            AccountRealm accountRealm = AccountTable.createAccountRealm(cursor);
            realm.copyToRealm(accountRealm);

            counter++;
        }
        cursor.close();
        LogManager.i(LOG_TAG, counter + " accounts copied to Realm");

        LogManager.i(LOG_TAG, "onSuccess. removing accounts from SQLite:");
        int removedAccounts = AccountTable.getInstance().removeAllAccounts();
        LogManager.i(LOG_TAG, removedAccounts + " accounts removed from SQLite");

        realm.commitTransaction();
        realm.close();
    }
}
