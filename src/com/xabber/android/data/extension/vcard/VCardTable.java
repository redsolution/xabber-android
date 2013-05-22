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
package com.xabber.android.data.extension.vcard;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.xabber.android.data.AbstractTable;
import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.roster.StructuredName;

/**
 * Storage with useful vcard fields.
 * 
 * @author alexander.ivanov
 */
class VCardTable extends AbstractTable {

	private static final class Fields implements BaseColumns {

		private Fields() {
		}

		public static final String USER = "user";
		public static final String NICK_NAME = "nick_name";
		public static final String FORMATTED_NAME = "formatted_name";
		public static final String FIRST_NAME = "first_name";
		public static final String MIDDLE_NAME = "middle_name";
		public static final String LAST_NAME = "last_name";

	}

	private static final String NAME = "vcards";
	private static final String[] PROJECTION = new String[] { Fields.USER,
			Fields.NICK_NAME, Fields.FORMATTED_NAME, Fields.FIRST_NAME,
			Fields.MIDDLE_NAME, Fields.LAST_NAME };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static VCardTable instance;

	static {
		instance = new VCardTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static VCardTable getInstance() {
		return instance;
	}

	private VCardTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + NAME + " (" + Fields.USER
				+ " TEXT PRIMARY KEY," + Fields.NICK_NAME + " TEXT,"
				+ Fields.FORMATTED_NAME + " TEXT," + Fields.FIRST_NAME
				+ " TEXT," + Fields.MIDDLE_NAME + " TEXT," + Fields.LAST_NAME
				+ " TEXT);";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 11:
			sql = "CREATE TABLE vcards (" + "user TEXT PRIMARY KEY,"
					+ "nick_name TEXT," + "formatted_name TEXT);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 44:
			sql = "UPDATE vcards SET nick_name = \"\" WHERE nick_name IS NULL;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE vcards SET formatted_name = \"\" WHERE formatted_name IS NULL;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 49:
			DatabaseManager.dropTable(db, "vcards");
			sql = "CREATE TABLE vcards (" + "user TEXT PRIMARY KEY,"
					+ "nick_name TEXT," + "formatted_name TEXT,"
					+ "first_name TEXT," + "middle_name TEXT,"
					+ "last_name TEXT);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	void write(String bareAddress, StructuredName name) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.USER + ", " + Fields.NICK_NAME
						+ ", " + Fields.FORMATTED_NAME + ", "
						+ Fields.FIRST_NAME + ", " + Fields.MIDDLE_NAME + ", "
						+ Fields.LAST_NAME + ") VALUES (?, ?, ?, ?, ?, ?);");
			}
			writeStatement.bindString(1, bareAddress);
			writeStatement.bindString(2, name.getNickName());
			writeStatement.bindString(3, name.getFormattedName());
			writeStatement.bindString(4, name.getFirstName());
			writeStatement.bindString(5, name.getMiddleName());
			writeStatement.bindString(6, name.getLastName());
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

	static String getNickName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.NICK_NAME));
	}

	static String getFormattedName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.FORMATTED_NAME));
	}

	static String getFirstName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.FIRST_NAME));
	}

	static String getMiddleName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.MIDDLE_NAME));
	}

	static String getLastName(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.LAST_NAME));
	}

}