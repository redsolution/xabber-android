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
package com.xabber.android.data.message;

import java.util.Collection;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.entity.AbstractEntityTable;

/**
 * Storage with messages.
 * 
 * @author alexander.ivanov
 */
class MessageTable extends AbstractEntityTable {

	private static final class Fields implements AbstractEntityTable.Fields {

		private Fields() {
		}

		/**
		 * Message archive collection tag.
		 */
		public static final String TAG = "tag";

		/**
		 * User's resource or nick in chat room.
		 */
		public static final String RESOURCE = "resource";

		/**
		 * Text message.
		 */
		public static final String TEXT = "text";

		/**
		 * Message action.
		 * <ul>
		 * <li>Must be empty string for usual text message.</li>
		 * <li>Must be one of names in MessageAction.</li>
		 * </ul>
		 * 
		 * {@link #TEXT} can contains some description on this action.
		 */
		public static final String ACTION = "action";

		/**
		 * Time when this message was created locally.
		 */
		public static final String TIMESTAMP = "timestamp";

		/**
		 * Receive and send delay.
		 */
		public static final String DELAY_TIMESTAMP = "delay_timestamp";

		/**
		 * Whether message is incoming.
		 */
		public static final String INCOMING = "incoming";

		/**
		 * Whether incoming message was read.
		 */
		public static final String READ = "read";

		/**
		 * Whether this outgoing message was sent.
		 */
		public static final String SENT = "sent";

		/**
		 * Whether this outgoing message was not received.
		 */
		public static final String ERROR = "error";

	}

	private static final String NAME = "messages";
	private static final String[] PROJECTION = new String[] { Fields._ID,
			Fields.ACCOUNT, Fields.USER, Fields.RESOURCE, Fields.TEXT,
			Fields.ACTION, Fields.TIMESTAMP, Fields.DELAY_TIMESTAMP,
			Fields.INCOMING, Fields.READ, Fields.SENT, Fields.ERROR, Fields.TAG };

	private final DatabaseManager databaseManager;
	private SQLiteStatement insertNewMessageStatement;
	private final Object insertNewMessageLock;

	private final static MessageTable instance;

	static {
		instance = new MessageTable(DatabaseManager.getInstance());
		DatabaseManager.getInstance().addTable(instance);
	}

	public static MessageTable getInstance() {
		return instance;
	}

	private MessageTable(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		insertNewMessageStatement = null;
		insertNewMessageLock = new Object();
	}

	@Override
	public void create(SQLiteDatabase db) {
		String sql;
		sql = "CREATE TABLE " + NAME + " (" + Fields._ID
				+ " INTEGER PRIMARY KEY," + Fields.ACCOUNT + " TEXT,"
				+ Fields.USER + " TEXT," + Fields.RESOURCE + " TEXT,"
				+ Fields.TEXT + " TEXT," + Fields.ACTION + " TEXT,"
				+ Fields.TIMESTAMP + " INTEGER," + Fields.DELAY_TIMESTAMP
				+ " INTEGER," + Fields.INCOMING + " BOOLEAN," + Fields.READ
				+ " BOOLEAN," + Fields.SENT + " BOOLEAN," + Fields.ERROR
				+ " BOOLEAN," + Fields.TAG + " TEXT);";
		DatabaseManager.execSQL(db, sql);
		sql = "CREATE INDEX " + NAME + "_list ON " + NAME + " ("
				+ Fields.ACCOUNT + ", " + Fields.USER + ", " + Fields.TIMESTAMP
				+ " ASC)";
		DatabaseManager.execSQL(db, sql);
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
		super.migrate(db, toVersion);
		String sql;
		switch (toVersion) {
		case 4:
			sql = "CREATE TABLE messages (_id INTEGER PRIMARY KEY,"
					+ "account INTEGER," + "user TEXT," + "text TEXT,"
					+ "timestamp INTEGER," + "delay_timestamp INTEGER,"
					+ "incoming BOOLEAN," + "read BOOLEAN,"
					+ "notified BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE INDEX messages_list ON messages (account, user, timestamp ASC);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 8:
			DatabaseManager.dropTable(db, "messages");
			sql = "CREATE TABLE messages (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "user TEXT," + "text TEXT,"
					+ "timestamp INTEGER," + "delay_timestamp INTEGER,"
					+ "incoming BOOLEAN," + "read BOOLEAN,"
					+ "notified BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "CREATE INDEX messages_list ON messages (account, user, timestamp ASC);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 10:
			sql = "ALTER TABLE messages ADD COLUMN send BOOLEAN;";
			DatabaseManager.execSQL(db, sql);
			sql = "ALTER TABLE messages ADD COLUMN error BOOLEAN;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE messages SET send = 1, error = 0 WHERE incoming = 0;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 15:
			sql = "UPDATE messages SET send = 1 WHERE incoming = 1;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 17:
			sql = "ALTER TABLE messages ADD COLUMN save BOOLEAN;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE messages SET save = 1;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 23:
			sql = "ALTER TABLE messages ADD COLUMN resource TEXT;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE messages SET resource = \"\";";
			DatabaseManager.execSQL(db, sql);
			sql = "ALTER TABLE messages ADD COLUMN action TEXT;";
			DatabaseManager.execSQL(db, sql);
			sql = "UPDATE messages SET action = \"\";";
			DatabaseManager.execSQL(db, sql);
			break;
		case 27:
			DatabaseManager.renameTable(db, "messages", "old_messages");
			sql = "CREATE TABLE messages (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "user TEXT," + "resource TEXT,"
					+ "text TEXT," + "action TEXT," + "timestamp INTEGER,"
					+ "delay_timestamp INTEGER," + "incoming BOOLEAN,"
					+ "read BOOLEAN," + "notified BOOLEAN," + "send BOOLEAN,"
					+ "error BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT INTO messages ("
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, notified, send, error"
					+ ") SELECT "
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, notified, send, error"
					+ " FROM old_messages WHERE save;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "old_messages");
			// Create index after drop old index.
			sql = "CREATE INDEX messages_list ON messages (account, user, timestamp ASC);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 28:
			DatabaseManager.renameTable(db, "messages", "old_messages");
			sql = "CREATE TABLE messages (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "user TEXT," + "resource TEXT,"
					+ "text TEXT," + "action TEXT," + "timestamp INTEGER,"
					+ "delay_timestamp INTEGER," + "incoming BOOLEAN,"
					+ "read BOOLEAN," + "notified BOOLEAN," + "sent BOOLEAN,"
					+ "error BOOLEAN);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT INTO messages ("
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, notified, sent, error"
					+ ") SELECT "
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, notified, send, error"
					+ " FROM old_messages;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "old_messages");
			sql = "CREATE INDEX messages_list ON messages (account, user, timestamp ASC);";
			DatabaseManager.execSQL(db, sql);
			break;
		case 58:
			sql = "ALTER TABLE messages ADD COLUMN tag TEXT;";
			DatabaseManager.execSQL(db, sql);
			break;
		case 61:
			DatabaseManager.renameTable(db, "messages", "old_messages");
			sql = "CREATE TABLE messages (_id INTEGER PRIMARY KEY,"
					+ "account TEXT," + "user TEXT," + "resource TEXT,"
					+ "text TEXT," + "action TEXT," + "timestamp INTEGER,"
					+ "delay_timestamp INTEGER," + "incoming BOOLEAN,"
					+ "read BOOLEAN," + "sent BOOLEAN," + "error BOOLEAN,"
					+ "tag TEXT);";
			DatabaseManager.execSQL(db, sql);
			sql = "INSERT INTO messages ("
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, sent, error, tag"
					+ ") SELECT "
					+ "account, user, resource, text, action, timestamp, delay_timestamp, incoming, read, sent, error, tag"
					+ " FROM old_messages;";
			DatabaseManager.execSQL(db, sql);
			DatabaseManager.dropTable(db, "old_messages");
			sql = "CREATE INDEX messages_list ON messages (account, user, timestamp ASC);";
			DatabaseManager.execSQL(db, sql);
			break;
		default:
			break;
		}
	}

	/**
	 * Save new message to the database.
	 * 
	 * @return Assigned id.
	 */
	long add(String account, String bareAddress, String tag, String resource,
			String text, ChatAction action, Date timeStamp,
			Date delayTimeStamp, boolean incoming, boolean read, boolean sent,
			boolean error) {
		final String actionString;
		if (action == null)
			actionString = "";
		else
			actionString = action.name();
		synchronized (insertNewMessageLock) {
			if (insertNewMessageStatement == null) {
				SQLiteDatabase db = databaseManager.getWritableDatabase();
				insertNewMessageStatement = db.compileStatement("INSERT INTO "
						+ NAME + " (" + Fields.ACCOUNT + ", " + Fields.USER
						+ ", " + Fields.RESOURCE + ", " + Fields.TEXT + ", "
						+ Fields.ACTION + ", " + Fields.TIMESTAMP + ", "
						+ Fields.DELAY_TIMESTAMP + ", " + Fields.INCOMING
						+ ", " + Fields.READ + ", " + Fields.SENT + ", "
						+ Fields.ERROR + ", " + Fields.TAG + ") VALUES "
						+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			}
			insertNewMessageStatement.bindString(1, account);
			insertNewMessageStatement.bindString(2, bareAddress);
			insertNewMessageStatement.bindString(3, resource);
			insertNewMessageStatement.bindString(4, text);
			insertNewMessageStatement.bindString(5, actionString);
			insertNewMessageStatement.bindLong(6, timeStamp.getTime());
			if (delayTimeStamp == null)
				insertNewMessageStatement.bindNull(7);
			else
				insertNewMessageStatement.bindLong(7, delayTimeStamp.getTime());
			insertNewMessageStatement.bindLong(8, incoming ? 1 : 0);
			insertNewMessageStatement.bindLong(9, read ? 1 : 0);
			insertNewMessageStatement.bindLong(10, sent ? 1 : 0);
			insertNewMessageStatement.bindLong(11, error ? 1 : 0);
			if (tag == null)
				insertNewMessageStatement.bindNull(12);
			else
				insertNewMessageStatement.bindString(12, tag);
			return insertNewMessageStatement.executeInsert();
		}
	}

	void markAsRead(Collection<Long> ids) {
		if (ids.isEmpty())
			return;
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Fields.READ, 1);
		db.update(NAME, values, DatabaseManager.in(Fields._ID, ids), null);
	}

	void markAsSent(Collection<Long> ids) {
		if (ids.isEmpty())
			return;
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Fields.SENT, 1);
		db.update(NAME, values, DatabaseManager.in(Fields._ID, ids), null);
	}

	void markAsError(long id) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Fields.ERROR, 1);
		db.update(NAME, values, Fields._ID + " = ?",
				new String[] { String.valueOf(id) });
	}

	/**
	 * @param account
	 * @param bareAddress
	 * @return Result set with messages for the chat.
	 */
	Cursor list(String account, String bareAddress) {
		SQLiteDatabase db = databaseManager.getReadableDatabase();
		return db.query(NAME, PROJECTION, Fields.ACCOUNT + " = ? AND "
				+ Fields.USER + " = ?", new String[] { account, bareAddress },
				null, null, Fields.TIMESTAMP);
	}

	/**
	 * @return Messages to be sent.
	 */
	Cursor messagesToSend() {
		SQLiteDatabase db = databaseManager.getReadableDatabase();
		return db.query(NAME, PROJECTION, Fields.INCOMING + " = ? AND "
				+ Fields.SENT + " = ?", new String[] { "0", "0" }, null, null,
				Fields.TIMESTAMP);
	}

	/**
	 * Removes all read and sent messages.
	 * 
	 * @param account
	 */
	void removeReadAndSent(String account) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.READ
				+ " = ? AND " + Fields.SENT + " = ?", new String[] { account,
				"1", "1" });
	}

	/**
	 * Removes all sent messages.
	 * 
	 * @param account
	 */
	void removeSent(String account) {
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.SENT + " = ?",
				new String[] { account, "1", });
	}

	void removeMessages(Collection<Long> ids) {
		if (ids.isEmpty())
			return;
		SQLiteDatabase db = databaseManager.getWritableDatabase();
		db.delete(NAME, DatabaseManager.in(Fields._ID, ids), null);
	}

	@Override
	protected String getTableName() {
		return NAME;
	}

	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	static long getId(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(Fields._ID));
	}

	static String getTag(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.TAG));
	}

	static String getResource(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.RESOURCE));
	}

	static String getText(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(Fields.TEXT));
	}

	static ChatAction getAction(Cursor cursor) {
		return ChatAction.getChatAction(cursor.getString(cursor
				.getColumnIndex(Fields.ACTION)));
	}

	static boolean isIncoming(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.INCOMING)) != 0;
	}

	static boolean isSent(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.SENT)) != 0;
	}

	static boolean isRead(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.READ)) != 0;
	}

	static boolean hasError(Cursor cursor) {
		return cursor.getInt(cursor.getColumnIndex(Fields.ERROR)) != 0;
	}

	static Date getTimeStamp(Cursor cursor) {
		return new Date(cursor.getLong(cursor.getColumnIndex(Fields.TIMESTAMP)));
	}

	static Date getDelayTimeStamp(Cursor cursor) {
		if (cursor.isNull(cursor.getColumnIndex(Fields.DELAY_TIMESTAMP)))
			return null;
		return new Date(cursor.getLong(cursor
				.getColumnIndex(Fields.DELAY_TIMESTAMP)));
	}
}