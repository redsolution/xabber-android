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
package com.xabber.android.data.account;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import android.content.ContentValues;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.xabber.android.data.AbstractTable;
import com.xabber.android.data.Application;
import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.androiddev.R;

/**
 * Storage with account settings.
 * 
 * @author alexander.ivanov
 */
class AccountTable extends AbstractTable {

	private static final class Fields implements BaseColumns {
		private Fields() {
		}

		public static final String ENABLED = "enabled";
		public static final String SERVER_NAME = "server_name";
		public static final String USER_NAME = "user_name";
		public static final String PASSWORD = "password";
		public static final String RESOURCE = "resource";
		public static final String PRIORITY = "priority";
		public static final String STATUS_MODE = "status_mode";
		public static final String STATUS_TEXT = "status_text";
		public static final String CUSTOM = "custom";
		public static final String HOST = "host";
		public static final String PORT = "port";
		public static final String SASL_ENABLED = "sasl_enabled";
		public static final String TLS_MODE = "required_tls";
		public static final String COMPRESSION = "compression";
		public static final String COLOR_INDEX = "color_index";
		public static final String PROTOCOL = "protocol";
		public static final String SYNCABLE = "syncable";
		public static final String STORE_PASSWORD = "store_password";
		public static final String PUBLIC_KEY = "public_key";
		public static final String PRIVATE_KEY = "private_key";
		public static final String LAST_SYNC = "last_sync";
		public static final String ARCHIVE_MODE = "archive_mode";
		public static final String PROXY_TYPE = "proxy_type";
		public static final String PROXY_HOST = "proxy_host";
		public static final String PROXY_PORT = "proxy_port";
		public static final String PROXY_USER = "proxy_user";
		public static final String PROXY_PASSWORD = "proxy_password";
	}

	private static final String NAME = "accounts";
	private static final String[] PROJECTION = new String[] { Fields._ID,
			Fields.PROTOCOL, Fields.CUSTOM, Fields.HOST, Fields.PORT,
			Fields.SERVER_NAME, Fields.USER_NAME, Fields.PASSWORD,
			Fields.RESOURCE, Fields.COLOR_INDEX, Fields.PRIORITY,
			Fields.STATUS_MODE, Fields.STATUS_TEXT, Fields.ENABLED,
			Fields.SASL_ENABLED, Fields.TLS_MODE, Fields.COMPRESSION,
			Fields.SYNCABLE, Fields.STORE_PASSWORD, Fields.PUBLIC_KEY,
			Fields.PRIVATE_KEY, Fields.LAST_SYNC, Fields.ARCHIVE_MODE,
			Fields.PROXY_TYPE, Fields.PROXY_HOST, Fields.PROXY_PORT,
			Fields.PROXY_USER, Fields.PROXY_PASSWORD };

	private final DatabaseManager databaseManager;

	private final static AccountTable instance;

	static {
		instance = new AccountTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static AccountTable getInstance() {
		return instance;
	}

	private AccountTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
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
			long count = db.compileStatement("SELECT COUNT(*) FROM accounts;")
					.simpleQueryForLong();
			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					Application.getInstance().getBaseContext()).edit();
			if (count < 2)
				editor.putBoolean(
						Application.getInstance().getString(
								R.string.contacts_show_accounts_key), false);
			else
				editor.putBoolean(
						Application.getInstance().getString(
								R.string.contacts_enable_show_accounts_key),
						false);
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
			if ("all".equals(value))
				archiveMode = ArchiveMode.available;
			else if ("unread".equals(value))
				archiveMode = ArchiveMode.unreadOnly;
			else
				archiveMode = ArchiveMode.dontStore;
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

	/**
	 * Adds or updates account.
	 * 
	 * @return Assigned id.
	 */
	long write(Long id, AccountProtocol protocol, boolean custom, String host,
			int port, String serverName, String userName, String resource,
			boolean storePassword, String password, int colorIndex,
			int priority, StatusMode statusMode, String statusText,
			boolean enabled, boolean saslEnabled, TLSMode tlsMode,
			boolean compression, ProxyType proxyType, String proxyHost,
			int proxyPort, String proxyUser, String proxyPassword,
			boolean syncable, KeyPair keyPair, Date lastSync,
			ArchiveMode archiveMode) {
		ContentValues values = new ContentValues();
		values.put(Fields.PROTOCOL, protocol.name());
		values.put(Fields.CUSTOM, custom ? 1 : 0);
		values.put(Fields.HOST, host);
		values.put(Fields.PORT, port);
		values.put(Fields.SERVER_NAME, serverName);
		values.put(Fields.USER_NAME, userName);
		if (!storePassword)
			password = AccountItem.UNDEFINED_PASSWORD;
		values.put(Fields.PASSWORD, password);
		values.put(Fields.RESOURCE, resource);
		values.put(Fields.COLOR_INDEX, colorIndex);
		values.put(Fields.PRIORITY, priority);
		values.put(Fields.STATUS_MODE, statusMode.ordinal());
		values.put(Fields.STATUS_TEXT, statusText);
		values.put(Fields.ENABLED, enabled ? 1 : 0);
		values.put(Fields.SASL_ENABLED, saslEnabled ? 1 : 0);
		values.put(Fields.TLS_MODE, tlsMode.ordinal());
		values.put(Fields.COMPRESSION, compression ? 1 : 0);
		values.put(Fields.PROXY_TYPE, proxyType.ordinal());
		values.put(Fields.PROXY_HOST, proxyHost);
		values.put(Fields.PROXY_PORT, proxyPort);
		values.put(Fields.PROXY_USER, proxyUser);
		values.put(Fields.PROXY_PASSWORD, proxyPassword);
		values.put(Fields.SYNCABLE, syncable ? 1 : 0);
		values.put(Fields.STORE_PASSWORD, storePassword ? 1 : 0);
		if (keyPair == null) {
			values.putNull(Fields.PUBLIC_KEY);
			values.putNull(Fields.PRIVATE_KEY);
		} else {
			PublicKey publicKey = keyPair.getPublic();
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
					publicKey.getEncoded());
			PrivateKey privateKey = keyPair.getPrivate();
			PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
					privateKey.getEncoded());
			byte[] publicKeyBytes = x509EncodedKeySpec.getEncoded();
			byte[] privateKeyBytes = pkcs8EncodedKeySpec.getEncoded();
			values.put(Fields.PUBLIC_KEY, publicKeyBytes);
			values.put(Fields.PRIVATE_KEY, privateKeyBytes);
		}
		if (lastSync == null)
			values.putNull(Fields.LAST_SYNC);
		else
			values.put(Fields.LAST_SYNC, lastSync.getTime());
		values.put(Fields.ARCHIVE_MODE, archiveMode.ordinal());
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		if (id == null)
			return db.insert(NAME, Fields.USER_NAME, values);
		db.update(NAME, values, Fields._ID + " = ?",
				new String[] { String.valueOf(id) });
		return id;
	}

	/**
	 * Delete record from database.
	 * 
	 * @param accountItem
	 * @return <b>True</b> if record was removed.
	 */
	void remove(String account, long id) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields._ID + " = ?",
				new String[] { String.valueOf(id) });
		databaseManager.removeAccount(account);
	}

	@Override
	public void clear() {
		// Don't remove accounts on clear request.
	}

	void wipe() {
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

	static long getId(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(Fields._ID));
	}

	static AccountProtocol getProtocol(Cursor cursor) {
		return AccountProtocol.valueOf(cursor.getString(cursor
				.getColumnIndex(Fields.PROTOCOL)));
	}

	static String getHost(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.HOST));
	}

	static boolean isCustom(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.CUSTOM)) != 0;
	}

	static int getPort(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.PORT));
	}

	static String getServerName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.SERVER_NAME));
	}

	static String getUserName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.USER_NAME));
	}

	static String getPassword(Cursor cursor) {
		if (!isStorePassword(cursor))
			return AccountItem.UNDEFINED_PASSWORD;
		return cursor.getString(cursor.getColumnIndex(Fields.PASSWORD));
	}

	static String getResource(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.RESOURCE));
	}

	static int getColorIndex(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.COLOR_INDEX));
	}

	static int getPriority(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.PRIORITY));
	}

	static StatusMode getStatusMode(Cursor cursor) {
		int statusModeIndex = cursor.getInt(cursor
				.getColumnIndex(Fields.STATUS_MODE));
		return StatusMode.values()[statusModeIndex];
	}

	static String getStatusText(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.STATUS_TEXT));
	}

	static boolean isEnabled(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.ENABLED)) != 0;
	}

	static boolean isSaslEnabled(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.SASL_ENABLED)) != 0;
	}

	public static TLSMode getTLSMode(Cursor cursor) {
		int tlsModeIndex = cursor
				.getInt(cursor.getColumnIndex(Fields.TLS_MODE));
		return TLSMode.values()[tlsModeIndex];
	}

	static boolean isCompression(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.COMPRESSION)) != 0;
	}

	static boolean isSyncable(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.SYNCABLE)) != 0;
	}

	static boolean isStorePassword(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.STORE_PASSWORD)) != 0;
	}

	static Date getLastSync(Cursor cursor) {
		if (cursor.isNull(cursor.getColumnIndex(Fields.LAST_SYNC)))
			return null;
		else
			return new Date(cursor.getLong(cursor
					.getColumnIndex(Fields.LAST_SYNC)));
	}

	static ArchiveMode getArchiveMode(Cursor cursor) {
		int index = cursor.getInt(cursor.getColumnIndex(Fields.ARCHIVE_MODE));
		return ArchiveMode.values()[index];
	}

	static ProxyType getProxyType(Cursor cursor) {
		int index = cursor.getInt(cursor.getColumnIndex(Fields.PROXY_TYPE));
		return ProxyType.values()[index];
	}

	static String getProxyHost(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.PROXY_HOST));
	}

	static int getProxyPort(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.PROXY_PORT));
	}

	static String getProxyUser(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.PROXY_USER));
	}

	static String getProxyPassword(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.PROXY_PASSWORD));
	}

	static KeyPair getKeyPair(Cursor cursor) {
		byte[] publicKeyBytes = cursor.getBlob(cursor
				.getColumnIndex(Fields.PUBLIC_KEY));
		byte[] privateKeyBytes = cursor.getBlob(cursor
				.getColumnIndex(Fields.PRIVATE_KEY));
		if (privateKeyBytes == null || publicKeyBytes == null)
			return null;
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				publicKeyBytes);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
				privateKeyBytes);
		PublicKey publicKey;
		PrivateKey privateKey;
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("DSA");
			publicKey = keyFactory.generatePublic(publicKeySpec);
			privateKey = keyFactory.generatePrivate(privateKeySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		return new KeyPair(publicKey, privateKey);
	}

}
