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
package com.xabber.android.data.extension.avatar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.xabber.android.data.AbstractTable;
import com.xabber.android.data.DatabaseManager;

/**
 * Storage with avatar hashes for the users.
 * 
 * @author alexander.ivanov
 */
class AvatarTable extends AbstractTable {

	private static final class Fields implements BaseColumns {
		private Fields() {
		}

		public static final String USER = "user";
		public static final String HASH = "hash";
	}

	private static final String NAME = "avatars";
	private static final String[] PROJECTION = new String[] { Fields.USER,
			Fields.HASH, };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static AvatarTable instance;

	static {
		instance = new AvatarTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static AvatarTable getInstance() {
		return instance;
	}

	private AvatarTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + NAME + " (" + Fields.USER
				+ " TEXT PRIMARY KEY," + Fields.HASH + " TEXT);";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		switch (toVersion) {
		case 2:
			String sql = "CREATE TABLE avatars (" + "user TEXT PRIMARY KEY,"
					+ "hash TEXT);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	/**
	 * Saves avatar's hash for user.
	 * 
	 * @param bareAddress
	 * @param hash
	 */
	void write(String bareAddress, String hash) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.USER + ", " + Fields.HASH
						+ ") VALUES (?, ?);");
			}
			writeStatement.bindString(1, bareAddress);
			if (hash == null)
				writeStatement.bindNull(2);
			else
				writeStatement.bindString(2, hash);
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

	static String getUser(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.USER));
	}

	static String getHash(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.HASH));
	}

}