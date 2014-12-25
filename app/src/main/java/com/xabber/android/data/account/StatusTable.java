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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.xabber.android.data.AbstractTable;
import com.xabber.android.data.DatabaseManager;

/**
 * Storage with preset statuses.
 * 
 * @author alexander.ivanov
 */
class StatusTable extends AbstractTable {

	private static final class Fields implements BaseColumns {
		private Fields() {
		}

		public static final String STATUS_MODE = "status_mode";
		public static final String STATUS_TEXT = "status_text";
	}

	private static final String NAME = "statuses";
	private static final String[] PROJECTION = new String[] {
			Fields.STATUS_MODE, Fields.STATUS_TEXT };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static StatusTable instance;

	static {
		instance = new StatusTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static StatusTable getInstance() {
		return instance;
	}

	private StatusTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql;
		sql = "CREATE TABLE " + NAME + " (" + Fields.STATUS_MODE + " INTEGER,"
				+ Fields.STATUS_TEXT + " TEXT);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_index ON " + NAME + " ("
				+ Fields.STATUS_MODE + ", " + Fields.STATUS_TEXT + ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 12:
			sql = "CREATE TABLE statuses (" + "status_mode INTEGER,"
					+ "status_text TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX statuses_index ON statuses "
					+ "(status_mode, status_text);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 35:
			sql = "ALTER TABLE statuses ADD COLUMN share_location INTEGER;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE statuses SET share_location = 0;";
			DatabaseManager.execSQL(db, sql);
			sql = "DROP INDEX statuses_index;";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX statuses_index ON statuses "
					+ "(status_mode, status_text, share_location);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 38:
			sql = "DROP INDEX statuses_index;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.renameTable(db, "statuses", "old_statuses");
			sql = "CREATE TABLE statuses (" + "status_mode INTEGER,"
					+ "status_text TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX statuses_index ON statuses "
					+ "(status_mode, status_text);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT OR REPLACE INTO statuses (status_mode, status_text) "
					+ "SELECT status_mode, status_text FROM old_statuses;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "old_statuses");
			break;
		default:
			break;
		}
	}

	void write(StatusMode statusMode, String statusText) {
		if (statusText == null)
			statusText = "";
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.STATUS_MODE + ", "
						+ Fields.STATUS_TEXT + ") VALUES (?, ?);");
			}
			writeStatement.bindLong(1, statusMode.ordinal());
			writeStatement.bindString(2, statusText);
			writeStatement.execute();
		}
	}

	void remove(StatusMode statusMode, String statusText) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields.STATUS_MODE + " = ? AND " + Fields.STATUS_TEXT
				+ " = ?", new String[] { String.valueOf(statusMode.ordinal()),
				statusText });
	}

	@Override
	protected String getTableName() {
		return NAME;
	}

	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	static StatusMode getStatusMode(Cursor cursor) {
		return StatusMode.values()[cursor.getInt(cursor
				.getColumnIndex(Fields.STATUS_MODE))];
	}

	static String getStatusText(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.STATUS_TEXT));
	}

}