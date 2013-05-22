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
package com.xabber.android.data.extension.capability;

import java.util.Collection;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.xabber.android.data.AbstractTable;
import com.xabber.android.data.DatabaseManager;

/**
 * Storage with hashed entity capabilities.
 * 
 * @author alexander.ivanov
 */
class CapabilitiesTable extends AbstractTable {
	private static final class Fields implements BaseColumns {
		private Fields() {
		}

		/**
		 * Hash method.
		 */
		public static final String HASH = "hash";

		/**
		 * Node name.
		 */
		public static final String NODE = "node";

		/**
		 * Hashed capabilities.
		 */
		public static final String VERSION = "version";

		/**
		 * Client type.
		 */
		public static final String TYPE = "type_";

		/**
		 * Client name.
		 */
		public static final String NAME = "name";

		/**
		 * Comma separated list of client features.
		 */
		public static final String FEATURES = "features";
	}

	private static final String NAME = "capabilities";
	private static final String[] PROJECTION = new String[] { Fields.HASH,
			Fields.NODE, Fields.VERSION, Fields.TYPE, Fields.NAME,
			Fields.FEATURES };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static CapabilitiesTable instance;

	static {
		instance = new CapabilitiesTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static CapabilitiesTable getInstance() {
		return instance;
	}

	private CapabilitiesTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + NAME + " (" + Fields.HASH + " TEXT,"
				+ Fields.NODE + " TEXT," + Fields.VERSION + " TEXT,"
				+ Fields.TYPE + " TEXT," + Fields.NAME + " TEXT,"
				+ Fields.FEATURES + " TEXT);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_index ON " + NAME + " ("
				+ Fields.HASH + ", " + Fields.NODE + ", " + Fields.VERSION
				+ ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 51:
			sql = "CREATE TABLE capabilities (" + "hash TEXT," + "node TEXT,"
					+ "version TEXT," + "type_ TEXT," + "name TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX capabilities_index ON capabilities "
					+ "(hash, node, version);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 62:
			sql = "ALTER TABLE capabilities ADD COLUMN features TEXT;";
			DatabaseManager.execSQL(db, sql);
			sql = "DELETE FROM capabilities;";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	void write(String hash, String node, String version, String type,
			String name, Collection<String> features) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.HASH + ", " + Fields.NODE + ", "
						+ Fields.VERSION + ", " + Fields.TYPE + ", "
						+ Fields.NAME + ", " + Fields.FEATURES
						+ ") VALUES (?, ?, ?, ?, ?, ?);");
			}
			if (hash == null)
				writeStatement.bindNull(1);
			else
				writeStatement.bindString(1, hash);
			writeStatement.bindString(2, node);
			writeStatement.bindString(3, version);
			if (type == null)
				writeStatement.bindNull(4);
			else
				writeStatement.bindString(4, type);
			if (name == null)
				writeStatement.bindNull(5);
			else
				writeStatement.bindString(5, name);
			writeStatement.bindString(6,
					DatabaseManager.commaSeparatedFromCollection(features));
			writeStatement.execute();
		}
	}

	@Override
	protected String getTableName() {
		return NAME;
	}

	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	static String getHash(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.HASH));
	}

	static String getNode(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.NODE));
	}

	static String getVersion(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.VERSION));
	}

	static String getType(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.TYPE));
	}

	static String getName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.NAME));
	}

	static Collection<String> getFeatures(Cursor cursor) {
		return DatabaseManager.collectionFromCommaSeparated(cursor
				.getString(cursor.getColumnIndex(Fields.FEATURES)));
	}

}