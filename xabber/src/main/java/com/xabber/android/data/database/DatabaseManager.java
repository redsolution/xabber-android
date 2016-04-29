/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.database;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnMigrationListener;
import com.xabber.android.data.database.realm.BlockedContactsForAccount;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.database.sqlite.AbstractAccountTable;
import com.xabber.android.data.database.sqlite.DatabaseTable;
import com.xabber.android.data.database.sqlite.MessageTable;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.mam.SyncInfo;

import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Helps to open, create, and upgrade the database file.
 * <p/>
 * All requests to database / file system MUST be called from background thread.
 *
 * @author alexander.ivanov
 */
public class DatabaseManager extends SQLiteOpenHelper implements
        OnLoadListener, OnClearListener {

    private static final String DATABASE_NAME = "xabber.db";
    private static final String REALM_DATABASE_NAME = "xabber.realm";
    private static final int DATABASE_VERSION = 70;
    private static final int REALM_DATABASE_VERSION = 6;

    private static final SQLiteException DOWNGRAD_EXCEPTION = new SQLiteException(
            "Database file was deleted");
    private final static DatabaseManager instance;

    static {
        instance = new DatabaseManager();
        Application.getInstance().addManager(instance);
    }

    private final ArrayList<DatabaseTable> registeredTables;

    private DatabaseManager() {
        super(Application.getInstance(), DATABASE_NAME, null, DATABASE_VERSION);
        registeredTables = new ArrayList<>();
        configureRealm();
    }

    private void configureRealm() {
        RealmConfiguration config = new RealmConfiguration.Builder(Application.getInstance())
                .name(REALM_DATABASE_NAME)
                .schemaVersion(REALM_DATABASE_VERSION)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        RealmSchema schema = realm.getSchema();

                        if (oldVersion == 1) {
                            schema.create(SyncInfo.class.getSimpleName())
                                    .addField(SyncInfo.FIELD_ACCOUNT, String.class, FieldAttribute.INDEXED)
                                    .addField(SyncInfo.FIELD_USER, String.class, FieldAttribute.INDEXED)
                                    .addField(SyncInfo.FIELD_FIRST_MAM_MESSAGE_MAM_ID, String.class)
                                    .addField(SyncInfo.FIELD_FIRST_MAM_MESSAGE_STANZA_ID, String.class)
                                    .addField(SyncInfo.FIELD_LAST_MESSAGE_MAM_ID, String.class)
                                    .addField(SyncInfo.FIELD_REMOTE_HISTORY_COMPLETELY_LOADED, boolean.class);

                            oldVersion++;
                        }

                        if (oldVersion == 2) {
                            schema.create(MessageItem.class.getSimpleName())
                                    .addField(MessageItem.Fields.UNIQUE_ID, String.class, FieldAttribute.PRIMARY_KEY)
                                    .addField(MessageItem.Fields.ACCOUNT, String.class, FieldAttribute.INDEXED)
                                    .addField(MessageItem.Fields.USER, String.class, FieldAttribute.INDEXED)
                                    .addField(MessageItem.Fields.RESOURCE, String.class)
                                    .addField(MessageItem.Fields.ACTION, String.class)
                                    .addField(MessageItem.Fields.TEXT, String.class)
                                    .addField(MessageItem.Fields.TIMESTAMP, Long.class, FieldAttribute.INDEXED)
                                    .addField(MessageItem.Fields.DELAY_TIMESTAMP, Long.class)
                                    .addField(MessageItem.Fields.STANZA_ID, String.class)
                                    .addField(MessageItem.Fields.INCOMING, boolean.class)
                                    .addField(MessageItem.Fields.UNENCRYPTED, boolean.class)
                                    .addField(MessageItem.Fields.SENT, boolean.class)
                                    .addField(MessageItem.Fields.READ, boolean.class)
                                    .addField(MessageItem.Fields.DELIVERED, boolean.class)
                                    .addField(MessageItem.Fields.OFFLINE, boolean.class)
                                    .addField(MessageItem.Fields.ERROR, boolean.class)
                                    .addField(MessageItem.Fields.IS_RECEIVED_FROM_MAM, boolean.class)
                                    .addField(MessageItem.Fields.FILE_PATH, String.class)
                                    .addField(MessageItem.Fields.FILE_SIZE, Long.class);

                            oldVersion++;
                        }

                        if (oldVersion == 3) {
                            schema.get(MessageItem.class.getSimpleName())
                                    .addIndex(MessageItem.Fields.SENT);
                            oldVersion++;
                        }

                        if (oldVersion == 4) {
                            schema.get(MessageItem.class.getSimpleName())
                                    .addField(MessageItem.Fields.FORWARDED, boolean.class);
                            oldVersion++;
                        }

                        if (oldVersion == 5) {
                            schema.get(MessageItem.class.getSimpleName())
                                    .addField(MessageItem.Fields.ACKNOWLEDGED, boolean.class);
                            oldVersion++;
                        }
                    }
                })
                .build();
        Realm.setDefaultConfiguration(config);
    }

    private void copyDataFromSqliteToRealm() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getDefaultInstance();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        LogManager.i("DatabaseManager", "copying from sqlite to Reaml");
                        long counter = 0;
                        Cursor cursor = MessageTable.getInstance().getAllMessages();
                        while (cursor.moveToNext()) {
                            try {
                                MessageItem messageItem = MessageTable.createMessageItem(cursor);
                                realm.copyToRealm(messageItem);
                            } catch (XmppStringprepException | UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }

                            counter++;
                        }
                        cursor.close();
                        LogManager.i("DatabaseManager", counter + " messages copied to Realm");
                    }
                }, new Realm.Transaction.Callback() {

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        LogManager.i("DatabaseManager", "onSuccess. removing messages from sqlite:");
                        int removedMessages = MessageTable.getInstance().removeAllMessages();
                        LogManager.i("DatabaseManager", removedMessages + " messages removed from sqlite");
                    }

                    @Override
                    public void onError(Exception e) {
                        super.onError(e);
                        LogManager.exception(this, e);
                        LogManager.i("DatabaseManager", "onError " + e.getMessage());
                    }
                });
                realm.close();

            }
        });


    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    /**
     * Builds IN statement for specified collection of values.
     *
     * @param <T>
     * @param column
     * @param values
     * @return "column IN (value1, ... valueN)" or
     * "(column IS NULL AND column IS NOT NULL)" if ids is empty.
     */
    public static <T> String in(String column, Collection<T> values) {
        if (values.isEmpty())
            return new StringBuilder("(").append(column)
                    .append(" IS NULL AND ").append(column)
                    .append(" IS NOT NULL)").toString();
        StringBuilder builder = new StringBuilder(column);
        builder.append(" IN (");
        Iterator<T> iterator = values.iterator();
        while (iterator.hasNext()) {
            T value = iterator.next();
            if (value instanceof String)
                builder.append(DatabaseUtils.sqlEscapeString((String) value));
            else
                builder.append(value.toString());
            if (iterator.hasNext())
                builder.append(",");
        }
        builder.append(")");
        return builder.toString();
    }

    public static void execSQL(SQLiteDatabase db, String sql) {
        LogManager.iString(DatabaseManager.class.getName(), sql);
        db.execSQL(sql);
    }

    public static void dropTable(SQLiteDatabase db, String table) {
        execSQL(db, "DROP TABLE IF EXISTS " + table + ";");
    }

    public static void renameTable(SQLiteDatabase db, String table,
                                   String newTable) {
        execSQL(db, "ALTER TABLE " + table + " RENAME TO " + newTable + ";");
    }

    public static String commaSeparatedFromCollection(Collection<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (String value : strings) {
            if (builder.length() > 0)
                builder.append(",");
            builder.append(value.replace("\\", "\\\\").replace(",", "\\,"));
        }
        return builder.toString();
    }

    public static Collection<String> collectionFromCommaSeparated(String value) {
        Collection<String> collection = new ArrayList<String>();
        boolean escape = false;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char chr = value.charAt(index);
            if (!escape) {
                if (chr == '\\') {
                    escape = true;
                    continue;
                } else if (chr == ',') {
                    collection.add(builder.toString());
                    builder = new StringBuilder();
                    continue;
                }
            }
            escape = false;
            builder.append(chr);
        }
        collection.add(builder.toString());
        return Collections.unmodifiableCollection(collection);
    }

    /**
     * Register new table.
     *
     * @param table
     */
    public void addTable(DatabaseTable table) {
        registeredTables.add(table);
    }

    @Override
    public void onLoad() {
        try {
            getWritableDatabase(); // Force onCreate or onUpgrade
            copyDataFromSqliteToRealm();
        } catch (SQLiteException e) {
            if (e == DOWNGRAD_EXCEPTION) {
                // Downgrade occured
            } else {
                throw e;
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (DatabaseTable table : registeredTables)
            table.create(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion > newVersion) {
            LogManager.i(this, "Downgrading database from version "
                    + oldVersion + " to " + newVersion);
            File file = new File(db.getPath());
            file.delete();
            LogManager.i(this, "Database file was deleted");
            throw DOWNGRAD_EXCEPTION;
            // This will interrupt getWritableDatabase() call from
            // DatabaseManager's constructor.
        } else {
            LogManager.i(this, "Upgrading database from version " + oldVersion
                    + " to " + newVersion);
            while (oldVersion < newVersion) {
                oldVersion += 1;
                LogManager.i(this, "Migrate to version " + oldVersion);
                migrate(db, oldVersion);
                for (DatabaseTable table : registeredTables)
                    table.migrate(db, oldVersion);
                for (OnMigrationListener listener : Application.getInstance()
                        .getManagers(OnMigrationListener.class))
                    listener.onMigrate(oldVersion);
            }
        }
    }

    /**
     * Called on database migration.
     *
     * @param db
     * @param toVersion
     */
    private void migrate(SQLiteDatabase db, int toVersion) {
        switch (toVersion) {
            case 42:
                dropTable(db, "geolocs");
                dropTable(db, "locations");
                break;
            default:
                break;
        }
    }

    @Override
    public void onClear() {
        for (DatabaseTable table : registeredTables) {
            table.clear();
        }

        Realm realm = Realm.getDefaultInstance();
        Realm.deleteRealm(realm.getConfiguration());
        realm.close();
    }

    public void removeAccount(final String account) {
        // TODO: replace with constraint.
        for (DatabaseTable table : registeredTables) {
            if (table instanceof AbstractAccountTable) {
                ((AbstractAccountTable) table).removeAccount(account);
            }
        }

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(MessageItem.class).equalTo(MessageItem.Fields.ACCOUNT, account).findAll().deleteAllFromRealm();
                realm.where(BlockedContactsForAccount.class).equalTo(BlockedContactsForAccount.Fields.ACCOUNT, account).findAll().deleteAllFromRealm();
            }
        }, null);
        realm.close();
    }

}
