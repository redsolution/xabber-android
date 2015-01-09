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
 * Table with custom values associated with chat.
 * 
 * @author alexander.ivanov
 * 
 */
abstract class AbstractChatPropertyTable<T> extends AbstractEntityTable {

	static final class Fields implements AbstractEntityTable.Fields {

		private Fields() {
		}

		public static final String VALUE = "value";

	}

	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT,
			Fields.USER, Fields.VALUE };

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	AbstractChatPropertyTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	/**
	 * @return SQL type of the value field.
	 */
	abstract String getValueType();

	/**
	 * Hook to bind value as 3 argument to the statement.
	 * 
	 * @param writeStatement
	 * @param value
	 */
	abstract void bindValue(SQLiteStatement writeStatement, T value);

	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + getTableName() + " (" + Fields.ACCOUNT
				+ " TEXT," + Fields.USER + " TEXT," + Fields.VALUE + " "
				+ getValueType() + ");";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + getTableName() + "_index ON "
				+ getTableName() + " " + "(" + Fields.ACCOUNT + ", "
				+ Fields.USER + ");";
		DatabaseManager.execSQL(db, sql);
	}

	/**
	 * Initial migrate to create table.
	 * 
	 * @param db
	 * @param toVersion
	 * @param tableName
	 * @param valueType
	 */
	void initialMigrate(SQLiteDatabase db, String tableName, String valueType) {
		String sql;
		sql = "CREATE TABLE " + tableName + " (" + "account TEXT,"
				+ "user TEXT," + "value " + valueType + ");";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE UNIQUE INDEX " + tableName + "_index ON " + tableName
				+ " (account, user);";
		DatabaseManager.execSQL(db, sql);
	}

	void write(String account, String user, T value) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ getTableName() + " (" + Fields.ACCOUNT + ", "
						+ Fields.USER + ", " + Fields.VALUE
						+ ") VALUES (?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, user);
			bindValue(writeStatement, value);
			writeStatement.execute();
		}
	}

}
