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
package com.xabber.android.data.notification;

import java.util.Date;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractEntityTable;

/**
 * Storage with notifications.
 * 
 * @author alexander.ivanov
 */
class NotificationTable extends AbstractEntityTable {

	private static final class Fields implements AbstractEntityTable.Fields {

		private Fields() {
		}

		/**
		 * Text message.
		 */
		public static final String TEXT = "text";

		/**
		 * Time when last message was received.
		 */
		public static final String TIMESTAMP = "timestamp";

		/**
		 * Count of not notified messages.
		 */
		public static final String COUNT = "count";

	}

	private static final String NAME = "notifications";
	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT,
			Fields.USER, Fields.TEXT, Fields.TIMESTAMP, Fields.COUNT };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static NotificationTable instance;

	static {
		instance = new NotificationTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static NotificationTable getInstance() {
		return instance;
	}

	private NotificationTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql;
		sql = "CREATE TABLE " + NAME + " (" + Fields.ACCOUNT + " TEXT,"
				+ Fields.USER + " TEXT," + Fields.TEXT + " TEXT,"
				+ Fields.TIMESTAMP + " INTEGER," + Fields.COUNT + " INTEGER);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_list ON " + NAME + " ("
				+ Fields.ACCOUNT + ", " + Fields.USER + " )";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 60:
			sql = "CREATE TABLE notifications (" + "account TEXT,"
					+ "user TEXT," + "text TEXT," + "timestamp INTEGER,"
					+ "count INTEGER);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX notifications_list ON notifications (account, user);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	void write(String account, String user, String text, Date timeStamp,
			int count) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", " + Fields.USER
						+ ", " + Fields.TEXT + ", " + Fields.TIMESTAMP + ", "
						+ Fields.COUNT + ") VALUES " + "(?, ?, ?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, user);
			writeStatement.bindString(3, text);
			writeStatement.bindLong(4, timeStamp.getTime());
			writeStatement.bindLong(5, count);
			writeStatement.execute();
		}
	}

	public void remove(String account, String user) {
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

	@Override
	protected String getListOrder() {
		return Fields.TIMESTAMP;
	}

	static String getText(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.TEXT));
	}

	static Date getTimeStamp(Cursor cursor) {
		return new Date(cursor.getLong(cursor.getColumnIndex(Fields.TIMESTAMP)));
	}

	static int getCount(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.COUNT));
	}

}