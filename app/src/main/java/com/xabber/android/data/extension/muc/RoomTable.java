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
package com.xabber.android.data.extension.muc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractAccountTable;

/**
 * Storage with settings for the chat rooms.
 * 
 * @author alexander.ivanov
 */
class RoomTable extends AbstractAccountTable {

	private static final class Fields implements AbstractAccountTable.Fields {

		private Fields() {
		}

		/**
		 * Bare jid of chat room.
		 */
		public static final String ROOM = "room";

		/**
		 * Nick in chat room.
		 */
		public static final String NICKNAME = "nickname";

		/**
		 * Password.
		 */
		public static final String PASSWORD = "password";

		/**
		 * Join on launch.
		 */
		public static final String NEED_JOIN = "need_join";

	}

	private static final String NAME = "rooms";
	private static final String[] PROJECTION = new String[] { Fields._ID,
			Fields.ACCOUNT, Fields.ROOM, Fields.NICKNAME, Fields.PASSWORD,
			Fields.NEED_JOIN, };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static RoomTable instance;

	static {
		instance = new RoomTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static RoomTable getInstance() {
		return instance;
	}

	private RoomTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql;
		sql = "CREATE TABLE " + NAME + " (" + Fields._ID
				+ " INTEGER PRIMARY KEY," + Fields.ACCOUNT + " TEXT,"
				+ Fields.ROOM + " TEXT," + Fields.NICKNAME + " TEXT,"
				+ Fields.PASSWORD + " TEXT," + Fields.NEED_JOIN + " INTEGER);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_list ON " + NAME + " ("
				+ Fields.ACCOUNT + ", " + Fields.ROOM + ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 24:
			sql = "CREATE TABLE rooms (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "room TEXT," + "nickname TEXT,"
					+ "password TEXT," + "timestamp INTEGER);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX rooms_list ON rooms (account, room);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 25:
			DatabaseManager.dropTable(db, "rooms");
			sql = "CREATE TABLE rooms (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "room TEXT," + "nickname TEXT,"
					+ "password TEXT," + "need_join INTEGER);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX rooms_list ON rooms (account, room);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	/**
	 * Adds or updates room.
	 * 
	 * @param account
	 * @param room
	 * @param nickname
	 * @param password
	 * @param join
	 */
	void write(String account, String room, String nickname, String password,
			boolean join) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db
						.compileStatement("INSERT OR REPLACE INTO " + NAME
								+ " (" + Fields.ACCOUNT + ", " + Fields.ROOM
								+ ", " + Fields.NICKNAME + ", "
								+ Fields.PASSWORD + ", " + Fields.NEED_JOIN
								+ ") VALUES (?, ?, ?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, room);
			writeStatement.bindString(3, nickname);
			writeStatement.bindString(4, password);
			writeStatement.bindLong(5, join ? 1 : 0);
			writeStatement.execute();
		}
	}

	/**
	 * Removes room.
	 * 
	 * @param account
	 * @param room
	 */
	void remove(String account, String room) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.ROOM + " = ?",
				new String[] { account, room });
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
		return Fields.NEED_JOIN;
	}

	static long getId(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(Fields._ID));
	}

	static String getRoom(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.ROOM));
	}

	static String getNickname(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.NICKNAME));
	}

	static String getPassword(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.PASSWORD));
	}

	static boolean needJoin(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(Fields.NEED_JOIN)) != 0;
	}

}