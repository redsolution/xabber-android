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
package com.xabber.android.data.extension.otr;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractEntityTable;

/**
 * Storage with OTR finger prints and trusted level.
 * 
 * @author alexander.ivanov
 */
class OTRTable extends AbstractEntityTable {

	private static final class Fields implements AbstractEntityTable.Fields {

		private Fields() {
		}

		public static final String USER = "user";
		public static final String FINGERPRINT = "fingerprint";
		public static final String VERIFIED = "verified";

	}

	private static final String NAME = "otr";
	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT,
			Fields.USER, Fields.FINGERPRINT, Fields.VERIFIED, };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static OTRTable instance;

	static {
		instance = new OTRTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static OTRTable getInstance() {
		return instance;
	}

	private OTRTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql;
		sql = "CREATE TABLE " + NAME + " (" + Fields.ACCOUNT + " TEXT,"
				+ Fields.USER + " TEXT," + Fields.FINGERPRINT + " TEXT,"
				+ Fields.VERIFIED + " INTEGER);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_list ON " + NAME + " ("
				+ Fields.ACCOUNT + ", " + Fields.USER + ", "
				+ Fields.FINGERPRINT + ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 54:
			sql = "CREATE TABLE otr (" + "account TEXT," + "user TEXT,"
					+ "fingerprint TEXT," + "verified INTEGER);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX otr_list ON otr (account, user);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 56:
			sql = "DROP INDEX otr_list;";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX otr_list ON otr (account, user, fingerprint);";
			DatabaseManager.execSQL(db, sql);
		default:
			break;
		}
	}

	void write(String account, String user, String fingerprint, boolean verified) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", " + Fields.USER
						+ ", " + Fields.FINGERPRINT + ", " + Fields.VERIFIED
						+ ") VALUES (?, ?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, user);
			writeStatement.bindString(3, fingerprint);
			writeStatement.bindLong(4, verified ? 1 : 0);
			writeStatement.execute();
		}
	}

	void remove(String account, String user) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.USER + " = ?",
				new String[] { account, user });
	}

	@Override
	protected String getTableName() {
		return NAME;
	}

	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	static String getFingerprint(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.FINGERPRINT));
	}

	static boolean isVerified(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(Fields.VERIFIED)) != 0;
	}

}