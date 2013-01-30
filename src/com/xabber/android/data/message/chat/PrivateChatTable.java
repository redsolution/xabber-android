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
package com.xabber.android.data.message.chat;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractEntityTable;

/**
 * Storage for chats with disabled history.
 * 
 * @author alexander.ivanov
 * 
 */
class PrivateChatTable extends AbstractEntityTable {

	static final class Fields implements AbstractEntityTable.Fields {

		private Fields() {
		}

	}

	static final String NAME = "private_chats";
	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT,
			Fields.USER };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static PrivateChatTable instance;

	static {
		instance = new PrivateChatTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static PrivateChatTable getInstance() {
		return instance;
	}

	private PrivateChatTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + NAME + " (" + Fields.ACCOUNT + " TEXT,"
				+ Fields.USER + " TEXT);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + NAME + "_index ON " + NAME + " " + "("
				+ Fields.ACCOUNT + ", " + Fields.USER + ");";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 16:
			sql = "CREATE TABLE chats (" + "account TEXT," + "user TEXT,"
					+ "save BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX chats_index ON chats "
					+ "(account, user);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 18:
			sql = "ALTER TABLE chats ADD COLUMN " + "message TEXT;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE chats SET message = \"\";";
			DatabaseManager.execSQL(db, sql);
			break;
		case 19:
			sql = "UPDATE chats SET message = \"\";";
			DatabaseManager.execSQL(db, sql);
			break;
		case 20:
			DatabaseManager.dropTable(db, "chats");
			sql = "CREATE TABLE chats (" + "account TEXT," + "user TEXT,"
					+ "save_messages BOOLEAN, typed_message TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX chats_index ON chats "
					+ "(account, user);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 26:
			sql = "CREATE TABLE private_chats (" + "account TEXT,"
					+ "user TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE UNIQUE INDEX private_chats_index ON private_chats "
					+ "(account, user);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT INTO private_chats (account, user) "
					+ "SELECT account, user FROM chats WHERE NOT save_messages;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "chats");
			break;
		default:
			break;
		}
	}

	/**
	 * Sets that messages for specified chat must not to be saved.
	 */
	void write(String account, String user) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", " + Fields.USER
						+ ") VALUES (?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, user);
			writeStatement.execute();
		}
	}

	/**
	 * Sets that messages for specified chat can be saved.
	 */
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

}
