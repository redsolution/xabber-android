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
package com.xabber.android.data.database.sqlite;

import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionSettings;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.AccountRealm;
import com.xabber.android.data.entity.AccountJid;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import io.realm.Realm;

/**
 * Storage with account settings.
 *
 * @author alexander.ivanov
 */
public class AccountTable extends AbstractTable {

    private static final String NAME = "accounts";
    private static final String[] PROJECTION = new String[]{Fields._ID,
            Fields.PROTOCOL, Fields.CUSTOM, Fields.HOST, Fields.PORT,
            Fields.SERVER_NAME, Fields.USER_NAME, Fields.PASSWORD,
            Fields.RESOURCE, Fields.COLOR_INDEX, Fields.PRIORITY,
            Fields.STATUS_MODE, Fields.STATUS_TEXT, Fields.ENABLED,
            Fields.SASL_ENABLED, Fields.TLS_MODE, Fields.COMPRESSION,
            Fields.SYNCABLE, Fields.STORE_PASSWORD, Fields.PUBLIC_KEY,
            Fields.PRIVATE_KEY, Fields.LAST_SYNC, Fields.ARCHIVE_MODE,
            Fields.PROXY_TYPE, Fields.PROXY_HOST, Fields.PROXY_PORT,
            Fields.PROXY_USER, Fields.PROXY_PASSWORD};

    private static AccountTable instance;

    private final DatabaseManager databaseManager;

    public static AccountTable getInstance() {
        if (instance == null) {
            instance = new AccountTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private AccountTable(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public static long getId(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields._ID));
    }

    private static String getHost(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.HOST));
    }

    private static boolean isCustom(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.CUSTOM)) != 0;
    }

    private static int getPort(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.PORT));
    }

    private static String getServerName(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.SERVER_NAME));
    }

    private static String getUserName(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.USER_NAME));
    }

    private static String getPassword(Cursor cursor) {
        if (!isStorePassword(cursor)) {
            return AccountItem.UNDEFINED_PASSWORD;
        }
        return cursor.getString(cursor.getColumnIndex(Fields.PASSWORD));
    }

    private static String getResource(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.RESOURCE));
    }

    private static int getColorIndex(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.COLOR_INDEX));
    }

    private static int getPriority(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.PRIORITY));
    }

    private static StatusMode getStatusMode(Cursor cursor) {
        int statusModeIndex = cursor.getInt(cursor.getColumnIndex(Fields.STATUS_MODE));
        return StatusMode.values()[statusModeIndex];
    }

    private static String getStatusText(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.STATUS_TEXT));
    }

    private static boolean isEnabled(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.ENABLED)) != 0;
    }

    private static boolean isSaslEnabled(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.SASL_ENABLED)) != 0;
    }

    private static TLSMode getTLSMode(Cursor cursor) {
        int tlsModeIndex = cursor.getInt(cursor.getColumnIndex(Fields.TLS_MODE));
        return TLSMode.values()[tlsModeIndex];
    }

    private static boolean isCompression(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.COMPRESSION)) != 0;
    }

    private static boolean isSyncable(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.SYNCABLE)) != 0;
    }

    private static boolean isStorePassword(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.STORE_PASSWORD)) != 0;
    }

    private static Date getLastSync(Cursor cursor) {
        if (cursor.isNull(cursor.getColumnIndex(Fields.LAST_SYNC))) {
            return null;
        } else {
            return new Date(cursor.getLong(cursor.getColumnIndex(Fields.LAST_SYNC)));
        }
    }

    private static ArchiveMode getArchiveMode(Cursor cursor) {
        int index = cursor.getInt(cursor.getColumnIndex(Fields.ARCHIVE_MODE));
        return ArchiveMode.values()[index];
    }

    private static ProxyType getProxyType(Cursor cursor) {
        int index = cursor.getInt(cursor.getColumnIndex(Fields.PROXY_TYPE));
        return ProxyType.values()[index];
    }

    private static String getProxyHost(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.PROXY_HOST));
    }

    private static int getProxyPort(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.PROXY_PORT));
    }

    private static String getProxyUser(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.PROXY_USER));
    }

    private static String getProxyPassword(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.PROXY_PASSWORD));
    }

    private static KeyPair getKeyPair(Cursor cursor) {
        byte[] publicKeyBytes = cursor.getBlob(cursor.getColumnIndex(Fields.PUBLIC_KEY));
        byte[] privateKeyBytes = cursor.getBlob(cursor.getColumnIndex(Fields.PRIVATE_KEY));
        if (privateKeyBytes == null || publicKeyBytes == null) {
            return null;
        }
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PublicKey publicKey;
        PrivateKey privateKey;
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return new KeyPair(publicKey, privateKey);
    }

    @Override
    public void create(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + NAME + " (" + Fields._ID
                + " INTEGER PRIMARY KEY," + Fields.PROTOCOL + " TEXT,"
                + Fields.CUSTOM + " INTEGER," + Fields.HOST + " TEXT,"
                + Fields.PORT + " INTEGER," + Fields.SERVER_NAME + " TEXT,"
                + Fields.USER_NAME + " TEXT," + Fields.PASSWORD + " TEXT,"
                + Fields.RESOURCE + " TEXT," + Fields.COLOR_INDEX + " INTEGER,"
                + Fields.PRIORITY + " INTEGER," + Fields.STATUS_MODE
                + " INTEGER," + Fields.STATUS_TEXT + " TEXT," + Fields.ENABLED
                + " INTEGER," + Fields.SASL_ENABLED + " INTEGER,"
                + Fields.TLS_MODE + " INTEGER," + Fields.COMPRESSION
                + " INTEGER," + Fields.SYNCABLE + " INTEGER,"
                + Fields.STORE_PASSWORD + " INTEGER," + Fields.PUBLIC_KEY
                + " BLOB," + Fields.PRIVATE_KEY + " BLOB," + Fields.LAST_SYNC
                + " INTEGER," + Fields.ARCHIVE_MODE + " INTEGER,"
                + Fields.PROXY_TYPE + " INTEGER," + Fields.PROXY_HOST
                + " TEXT," + Fields.PROXY_PORT + " INTEGER,"
                + Fields.PROXY_USER + " TEXT," + Fields.PROXY_PASSWORD
                + " TEXT);";
        DatabaseManager.execSQL(db, sql);
    }

    @Override
    public void migrate(SQLiteDatabase db, int toVersion) {
        super.migrate(db, toVersion);
        String sql;
        switch (toVersion) {
            case 3:
                DatabaseManager.renameTable(db, "accounts", "accounts_");
                sql = "CREATE TABLE accounts (" + "_id INTEGER PRIMARY KEY,"
                        + "host TEXT," + "port INTEGER," + "server_name TEXT,"
                        + "user_name TEXT," + "password TEXT," + "resource TEXT,"
                        + "color_index INTEGER," + "priority INTEGER,"
                        + "status_mode INTEGER," + "status_text TEXT);";
                DatabaseManager.execSQL(db, sql);
                sql = "INSERT INTO accounts SELECT "
                        + "_id, host, port, server_name, user_name, password, resource, "
                        + "_id, 0, " + StatusMode.available.ordinal()
                        + ", '' FROM accounts_;";
                DatabaseManager.execSQL(db, sql);
                DatabaseManager.dropTable(db, "accounts_");
                break;
            case 9:
                sql = "ALTER TABLE accounts ADD COLUMN enabled INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET enabled = 1;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 21:
                sql = "ALTER TABLE accounts ADD COLUMN required_tls INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET required_tls = 0;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 22:
                sql = "ALTER TABLE accounts ADD COLUMN compression INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET compression = 0;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 30:
                sql = "ALTER TABLE accounts ADD COLUMN share_location INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN accept_location INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET share_location = 0, accept_location = 1;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 31:
                sql = "UPDATE accounts SET accept_location = 0;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 34:
                long count = db.compileStatement("SELECT COUNT(*) FROM accounts;").simpleQueryForLong();
                Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        Application.getInstance().getBaseContext()).edit();
                if (count < 2) {
                    editor.putBoolean(Application.getInstance().getString(
                            R.string.contacts_show_accounts_key), false);
                } else {
                    editor.putBoolean(Application.getInstance().getString(
                            R.string.contacts_enable_show_accounts_key), false);
                }
                editor.commit();
                break;
            case 36:
                sql = "ALTER TABLE accounts ADD COLUMN custom INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET custom = 1;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 37:
                sql = "ALTER TABLE accounts ADD COLUMN sasl_enabled INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET sasl_enabled = 1;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 43:
                DatabaseManager.renameTable(db, "accounts", "accounts_");
                sql = "CREATE TABLE accounts (" + "_id INTEGER PRIMARY KEY,"
                        + "custom INTEGER," + "host TEXT," + "port INTEGER,"
                        + "server_name TEXT," + "user_name TEXT,"
                        + "password TEXT," + "resource TEXT,"
                        + "color_index INTEGER," + "priority INTEGER,"
                        + "status_mode INTEGER," + "status_text TEXT,"
                        + "enabled INTEGER," + "sasl_enabled INTEGER,"
                        + "required_tls INTEGER," + "compression INTEGER);";
                DatabaseManager.execSQL(db, sql);
                String fields = "custom, host, port, server_name, user_name, password, "
                        + "resource, color_index, priority, status_mode, status_text, "
                        + "enabled, sasl_enabled, required_tls, compression";
                sql = "INSERT INTO accounts (" + fields + ") " + "SELECT " + fields
                        + " FROM accounts_;";
                DatabaseManager.execSQL(db, sql);
                DatabaseManager.dropTable(db, "accounts_");
                break;
            case 46:
                sql = "ALTER TABLE accounts ADD COLUMN protocol TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET protocol = 'xmpp';";
                DatabaseManager.execSQL(db, sql);
                break;
            case 48:
                sql = "ALTER TABLE accounts ADD COLUMN syncable INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET syncable = 0;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 50:
                sql = "ALTER TABLE accounts ADD COLUMN store_password INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET store_password = 1;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 53:
                sql = "UPDATE accounts SET protocol = 'gtalk' WHERE host = 'talk.google.com';";
                DatabaseManager.execSQL(db, sql);
                break;
            case 55:
                sql = "ALTER TABLE accounts ADD COLUMN public_key BLOB;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN private_key BLOB;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 59:
                sql = "ALTER TABLE accounts ADD COLUMN last_sync INTEGER;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 61:
                sql = "ALTER TABLE accounts ADD COLUMN archive_mode INTEGER;";
                DatabaseManager.execSQL(db, sql);
                ArchiveMode archiveMode;
                String value = PreferenceManager.getDefaultSharedPreferences(
                        Application.getInstance().getBaseContext()).getString(
                        "chats_history", "all");
                switch (value) {
                    case "all":
                        archiveMode = ArchiveMode.available;
                        break;
                    case "unread":
                        archiveMode = ArchiveMode.unreadOnly;
                        break;
                    default:
                        archiveMode = ArchiveMode.dontStore;
                        break;
                }
                sql = "UPDATE accounts SET archive_mode = " + archiveMode.ordinal()
                        + ";";
                DatabaseManager.execSQL(db, sql);
                break;
            case 66:
                sql = "ALTER TABLE accounts ADD COLUMN proxy_type INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN proxy_host TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN proxy_port INTEGER;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN proxy_user TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE accounts ADD COLUMN proxy_password TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE accounts SET proxy_type = "
                        + ProxyType.none.ordinal() + ", "
                        + "proxy_host = \"localhost\", " + "proxy_port = 8080, "
                        + "proxy_user = \"\", " + "proxy_password = \"\" "
                        + "WHERE proxy_type IS NULL;";
                DatabaseManager.execSQL(db, sql);
                break;
            default:
                break;
        }
    }

    public static AccountRealm createAccountRealm(Cursor cursor) {
        AccountRealm accountRealm = new AccountRealm();

        accountRealm.setCustom(AccountTable.isCustom(cursor));
        accountRealm.setHost(AccountTable.getHost(cursor));
        accountRealm.setPort(AccountTable.getPort(cursor));
        accountRealm.setServerName(AccountTable.getServerName(cursor));
        accountRealm.setUserName(AccountTable.getUserName(cursor));
        accountRealm.setResource(AccountTable.getResource(cursor));
        accountRealm.setStorePassword(AccountTable.isStorePassword(cursor));
        accountRealm.setPassword(AccountTable.getPassword(cursor));
        accountRealm.setColorIndex(AccountTable.getColorIndex(cursor));
        accountRealm.setPriority(AccountTable.getPriority(cursor));
        accountRealm.setStatusMode(AccountTable.getStatusMode(cursor));
        accountRealm.setStatusText(AccountTable.getStatusText(cursor));
        accountRealm.setEnabled(AccountTable.isEnabled(cursor));
        accountRealm.setSaslEnabled(AccountTable.isSaslEnabled(cursor));
        accountRealm.setTlsMode(AccountTable.getTLSMode(cursor));
        accountRealm.setCompression(AccountTable.isCompression(cursor));
        accountRealm.setProxyType(AccountTable.getProxyType(cursor));
        accountRealm.setProxyHost(AccountTable.getProxyHost(cursor));
        accountRealm.setProxyPort(AccountTable.getProxyPort(cursor));
        accountRealm.setProxyUser(AccountTable.getProxyUser(cursor));
        accountRealm.setProxyPassword(AccountTable.getProxyPassword(cursor));
        accountRealm.setSyncable(AccountTable.isSyncable(cursor));
        accountRealm.setKeyPair(AccountTable.getKeyPair(cursor));
        accountRealm.setLastSync(AccountTable.getLastSync(cursor));
        accountRealm.setArchiveMode(AccountTable.getArchiveMode(cursor));

        return accountRealm;
    }

    private void saveAccountRealm(String id, AccountItem accountItem) {
        AccountRealm accountRealm = new AccountRealm(id);

        ConnectionSettings connectionSettings = accountItem.getConnectionSettings();

        accountRealm.setCustom(connectionSettings.isCustomHostAndPort());
        accountRealm.setHost(connectionSettings.getHost());
        accountRealm.setPort(connectionSettings.getPort());
        accountRealm.setServerName(connectionSettings.getServerName().getDomain().toString());
        accountRealm.setUserName(connectionSettings.getUserName().toString());

        String password = connectionSettings.getPassword();
        if (!accountItem.isStorePassword()) {
            password = AccountItem.UNDEFINED_PASSWORD;
        }
        accountRealm.setPassword(password);

        accountRealm.setToken(connectionSettings.getToken());
        accountRealm.setOrder(accountItem.getOrder());
        accountRealm.setSyncNotAllowed(accountItem.isSyncNotAllowed());
        accountRealm.setXabberAutoLoginEnabled(accountItem.isXabberAutoLoginEnabled());
        accountRealm.setTimestamp(accountItem.getTimestamp());
        accountRealm.setResource(connectionSettings.getResource().toString());
        accountRealm.setColorIndex(accountItem.getColorIndex());
        accountRealm.setPriority(accountItem.getPriority());
        accountRealm.setStatusMode(accountItem.getRawStatusMode());
        accountRealm.setStatusText(accountItem.getStatusText());
        accountRealm.setEnabled(accountItem.isEnabled());
        accountRealm.setSaslEnabled(connectionSettings.isSaslEnabled());
        accountRealm.setTlsMode(connectionSettings.getTlsMode());
        accountRealm.setCompression(connectionSettings.useCompression());
        accountRealm.setProxyType(connectionSettings.getProxyType());
        accountRealm.setProxyHost(connectionSettings.getProxyHost());
        accountRealm.setProxyPort(connectionSettings.getProxyPort());
        accountRealm.setProxyUser(connectionSettings.getProxyUser());
        accountRealm.setProxyPassword(connectionSettings.getProxyPassword());
        accountRealm.setSyncable(accountItem.isSyncable());
        accountRealm.setStorePassword(accountItem.isStorePassword());
        accountRealm.setKeyPair(accountItem.getKeyPair());
        accountRealm.setLastSync(accountItem.getLastSync());
        accountRealm.setArchiveMode(accountItem.getArchiveMode());
        accountRealm.setClearHistoryOnExit(accountItem.isClearHistoryOnExit());
        accountRealm.setMamDefaultBehavior(accountItem.getMamDefaultBehaviour());
        accountRealm.setLoadHistorySettings(accountItem.getLoadHistorySettings());
        accountRealm.setSuccessfulConnectionHappened(accountItem.isSuccessfulConnectionHappened());

        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(accountRealm);
        realm.commitTransaction();
        realm.close();
    }

    public void write(String id, AccountItem accountItem) {
        saveAccountRealm(id, accountItem);
    }

    /**
     * Delete record from database.
     *
     * @return <b>True</b> if record was removed.
     */
    public void remove(AccountJid account, String id) {
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        realm.beginTransaction();
        AccountRealm accountRealm = realm
                .where(AccountRealm.class)
                .equalTo(AccountRealm.Fields.ID, id)
                .findFirst();

        if (accountRealm != null) {
            accountRealm.deleteFromRealm();
        }
        realm.commitTransaction();

        realm.close();

        SQLiteDatabase db = databaseManager.getWritableDatabase();
        db.delete(NAME, Fields._ID + " = ?", new String[]{ String.valueOf(id) });
        databaseManager.removeAccount(account);
    }

    public int removeAllAccounts() {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        return db.delete(NAME, null, null);
    }

    @Override
    public void clear() {
        // Don't remove accounts on clear request.
    }

    public void wipe() {
        super.clear();
    }

    @Override
    protected String getTableName() {
        return NAME;
    }

    @Override
    protected String[] getProjection() {
        return PROJECTION;
    }

    private static final class Fields implements BaseColumns {
        static final String ENABLED = "enabled";
        static final String SERVER_NAME = "server_name";
        static final String USER_NAME = "user_name";
        static final String PASSWORD = "password";
        static final String RESOURCE = "resource";
        static final String PRIORITY = "priority";
        static final String STATUS_MODE = "status_mode";
        static final String STATUS_TEXT = "status_text";
        static final String CUSTOM = "custom";
        static final String HOST = "host";
        static final String PORT = "port";
        static final String SASL_ENABLED = "sasl_enabled";
        static final String TLS_MODE = "required_tls";
        static final String COMPRESSION = "compression";
        static final String COLOR_INDEX = "color_index";
        static final String PROTOCOL = "protocol";
        static final String SYNCABLE = "syncable";
        static final String STORE_PASSWORD = "store_password";
        static final String PUBLIC_KEY = "public_key";
        static final String PRIVATE_KEY = "private_key";
        static final String LAST_SYNC = "last_sync";
        static final String ARCHIVE_MODE = "archive_mode";
        static final String PROXY_TYPE = "proxy_type";
        static final String PROXY_HOST = "proxy_host";
        static final String PROXY_PORT = "proxy_port";
        static final String PROXY_USER = "proxy_user";
        static final String PROXY_PASSWORD = "proxy_password";

        private Fields() {
        }
    }

}
