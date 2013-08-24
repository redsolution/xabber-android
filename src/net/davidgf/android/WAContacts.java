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
package net.davidgf.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractAccountTable;
import java.util.*;

/**
 * Storage for whatsapp contacts
 * 
 * @author david.guillen
 */
public class WAContacts extends AbstractAccountTable {

	private static final class Fields implements AbstractAccountTable.Fields {

		private Fields() {
		}

		public static final String ACCOUNT = "account";
		public static final String USER_PHONE = "user_phone";
		public static final String USER_NAME = "user_name";
	}

	private static final String NAME = "wa_contacts";
	private static final String[] PROJECTION = new String[] { Fields.ACCOUNT, Fields.USER_PHONE, Fields.USER_NAME };
	static final boolean DEFAULT_EXPANDED = true;

	private final DatabaseManager databaseManager;
	private SQLiteStatement writeStatement;
	private final Object writeLock;

	private final static WAContacts instance;

	static {
		instance = new WAContacts(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static WAContacts getInstance() {
		return instance;
	}

	private WAContacts(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		writeStatement = null;
		writeLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + NAME + " (" 
				+ Fields.ACCOUNT + " TEXT,"
				+ Fields.USER_PHONE + " TEXT,"
				+ Fields.USER_NAME + " TEXT);";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		// No migration at this time
	}

	public void addContact(String account, String user, String name) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				create(db);
				writeStatement = db.compileStatement("INSERT OR REPLACE INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", " 
						+ Fields.USER_PHONE + ", "
						+ Fields.USER_NAME + ") VALUES (?, ?, ?);");
			}
			writeStatement.bindString(1, account);
			writeStatement.bindString(2, user);
			writeStatement.bindString(3, name);
			writeStatement.execute();
		}
	}
	
	public void removeContact(String account, String user) {
		synchronized (writeLock) {
			if (writeStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				create(db);
				writeStatement = db.compileStatement("DELETE FROM "
						+ NAME + " WHERE " + Fields.USER_PHONE + " = "
						+ "? AND " + Fields.ACCOUNT + " = ?;");
			}
			writeStatement.bindString(1, user);
			writeStatement.bindString(2, account);
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

	public Vector < Vector <String> > getContacts(String account) {
		Vector < Vector <String> > ret = new Vector < Vector <String> >();

		SQLiteDatabase db = databaseManager.getWritableDatabase();
		create(db);
		Cursor c = db.query(NAME, PROJECTION, "account = '" + account + "'", null, null, null, null);
 
		if (c.moveToFirst()) {
			do {
				String user = c.getString(1);
				String name = c.getString(2);
				Vector < String > e = new Vector <String>();
				e.add(user);
				e.add(name);
				ret.add(e);
			} while(c.moveToNext());
		}

		return ret;
	}

}

