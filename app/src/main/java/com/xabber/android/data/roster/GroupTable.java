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
package com.xabber.android.data.roster;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractAccountTable;

/**
 * Storage with contact list group settings.
 * 
 * @author alexander.ivanov
 */
class GroupTable extends AbstractAccountTable {

	private static final class Fields implements AbstractAccountTable.Fields {

		private Fields() {
		}

		public static final String GROUP_NAME = "group_name";

		/**
		 * Whether group is expanded.
		 */
		public static final String EXPANDED = "expanded";

		/**
		 * Show offline contact mode.
		 */
		public static final String OFFLINE = "offline";

	}

	private static final String NAME = "groups";
	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT,
			Fields.GROUP_NAME, Fields.EXPANDED, Fields.OFFLINE };
	static final boolean DEFAULT_EXPANDED = true;

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static GroupTable instance;

	static {
		instance = new GroupTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static GroupTable getInstance() {
		return instance;
	}

	private GroupTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + NAME + " (" + Fields.ACCOUNT + " TEXT,"
				+ Fields.GROUP_NAME + " TEXT," + Fields.EXPANDED + " BOOLEAN,"
				+ Fields.OFFLINE + " INTEGER);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_group ON " + NAME + " ("
				+ Fields.ACCOUNT + ", " + Fields.GROUP_NAME + ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 7:
			sql = "CREATE TABLE groups (" + "_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "group_name TEXT,"
					+ "expanded BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX groups_group ON groups "
					+ "(account, group_name);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 13:
			sql = "ALTER TABLE groups ADD COLUMN " + "offline BOOLEAN;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE groups SET offline = 0;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 14:
			DatabaseManager.dropTable(db, "groups");
			sql = "CREATE TABLE groups (" + "account TEXT,"
					+ "group_name TEXT," + "expanded BOOLEAN,"
					+ "offline BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX groups_group ON groups "
					+ "(account, group_name);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 33:
			DatabaseManager.renameTable(db, "groups", "groups_");
			sql = "CREATE TABLE groups (" + "account TEXT,"
					+ "group_name TEXT," + "expanded BOOLEAN,"
					+ "offline INTEGER);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT INTO groups SELECT "
					+ "account, group_name, expanded, offline FROM groups_;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "groups_");
			sql = "CREATE UNIQUE INDEX groups_group ON groups "
					+ "(account, group_name);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	void write(String account, String group, boolean expanded,
			ShowOfflineMode showOfflineMode) {
		if (account == null || group == null)
			throw new IllegalArgumentException();
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", "
						+ Fields.GROUP_NAME + ", " + Fields.EXPANDED + ", "
						+ Fields.OFFLINE + ") VALUES (?, ?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, group);
			writeStatement.bindLong(3, expanded ? 1 : 0);
			if (showOfflineMode == ShowOfflineMode.never)
				writeStatement.bindLong(4, -1);
			else if (showOfflineMode == ShowOfflineMode.normal)
				writeStatement.bindLong(4, 0);
			else if (showOfflineMode == ShowOfflineMode.always)
				writeStatement.bindLong(4, 1);
			else
				throw new IllegalStateException();
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

	static String getGroup(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.GROUP_NAME));
	}

	static boolean isExpanded(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.EXPANDED)) != 0;
	}

	static ShowOfflineMode getShowOfflineMode(Cursor cursor) {
		int value = cursor.getInt(cursor.getColumnIndex(Fields.OFFLINE));
		if (value == -1)
			return ShowOfflineMode.never;
		else if (value == 0)
			return ShowOfflineMode.normal;
		else if (value == 1)
			return ShowOfflineMode.always;
		else
			throw new IllegalStateException();
	}

}