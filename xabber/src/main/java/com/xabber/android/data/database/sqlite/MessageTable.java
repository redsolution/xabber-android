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
package com.xabber.android.data.database.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.ChatAction;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Storage with messages.
 *
 * @author alexander.ivanov
 */
public class MessageTable extends AbstractEntityTable {

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
         * <p/>
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

        /**
         * usual message stanza (packet) id
         */
        public static final String STANZA_ID = "stanza_id";

        /**
         * Unique and Stable Stanza ID (XEP-0359)
         */
        public static final String UNIQUE_STANZA_ID = "unique_stanza_id";

        /**
         * If message was received from server message archive (XEP-0313)
         */
        public static final String IS_RECEIVED_FROM_MESSAGE_ARCHIVE = "is_received_from_message_archive";
    }

    private static final String NAME = "messages";
    private static final String[] PROJECTION = new String[]{Fields._ID,
            Fields.ACCOUNT, Fields.USER, Fields.RESOURCE, Fields.TEXT,
            Fields.ACTION, Fields.TIMESTAMP, Fields.DELAY_TIMESTAMP,
            Fields.INCOMING, Fields.READ, Fields.SENT, Fields.ERROR, Fields.TAG, Fields.STANZA_ID,
            Fields.UNIQUE_STANZA_ID, Fields.IS_RECEIVED_FROM_MESSAGE_ARCHIVE};

    private final DatabaseManager databaseManager;

    private static MessageTable instance;

    public static MessageTable getInstance() {
        if (instance == null) {
            instance = new MessageTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private MessageTable(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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
                + " BOOLEAN," + Fields.TAG + " TEXT, " + Fields.STANZA_ID + " TEXT, "
                + Fields.UNIQUE_STANZA_ID + " TEXT," + Fields.IS_RECEIVED_FROM_MESSAGE_ARCHIVE + " BOOLEAN" + ");";
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
            case 69:
                sql = "ALTER TABLE " + NAME + " ADD COLUMN " + Fields.STANZA_ID + " TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE " + NAME + " ADD COLUMN " + Fields.UNIQUE_STANZA_ID + " TEXT;";
                DatabaseManager.execSQL(db, sql);
                break;
            case 70:
                sql = "ALTER TABLE " + NAME + " ADD COLUMN " + Fields.IS_RECEIVED_FROM_MESSAGE_ARCHIVE + " BOOLEAN;";
                DatabaseManager.execSQL(db, sql);
                break;
            default:
                break;
        }
    }

    public static MessageItem createMessageItem(Cursor cursor) throws XmppStringprepException, UserJid.UserJidCreateException {
        MessageItem messageItem = new MessageItem();
        messageItem.setAccount(AccountJid.from(getAccount(cursor)));
        messageItem.setUser(UserJid.from(getUser(cursor)));
        messageItem.setResource(Resourcepart.from(getResource(cursor)));
        messageItem.setText(getText(cursor));

        ChatAction action = getAction(cursor);
        if (action != null) {
            messageItem.setAction(action.name());
        }
        messageItem.setTimestamp(getTimeStamp(cursor));
        messageItem.setDelayTimestamp(getDelayTimeStamp(cursor));
        messageItem.setIncoming(isIncoming(cursor));
        messageItem.setRead(isRead(cursor));
        messageItem.setSent(isSent(cursor));
        messageItem.setError(hasError(cursor));
        messageItem.setStanzaId(getStanzaId(cursor));
        messageItem.setReceivedFromMessageArchive(getReceivedFromMessageArchive(cursor));

        return messageItem;
    }

    public Cursor getAllMessages() {
        SQLiteDatabase db = databaseManager.getReadableDatabase();
        return db.query(NAME, PROJECTION, null, null, null, null, null);
    }

    public int removeAllMessages() {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        return db.delete(NAME, null, null);
    }

    /**
     * Removes all read and sent messages.
     *
     * @param account
     */
    void removeReadAndSent(String account) {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.READ
                + " = ? AND " + Fields.SENT + " = ?", new String[]{account,
                "1", "1"});
    }

    /**
     * Removes all sent messages.
     *
     * @param account
     */
    void removeSent(String account) {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        db.delete(NAME, Fields.ACCOUNT + " = ? AND " + Fields.SENT + " = ?",
                new String[]{account, "1",});
    }



    @Override
    protected String getTableName() {
        return NAME;
    }

    @Override
    protected String[] getProjection() {
        return PROJECTION;
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

    static Long getTimeStamp(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields.TIMESTAMP));
    }

    static Long getDelayTimeStamp(Cursor cursor) {
        if (cursor.isNull(cursor.getColumnIndex(Fields.DELAY_TIMESTAMP))) {
            return null;
        }
        return cursor.getLong(cursor.getColumnIndex(Fields.DELAY_TIMESTAMP));
    }

    static String getStanzaId(Cursor cursor) {
        if (cursor.isNull(cursor.getColumnIndex(Fields.STANZA_ID))) {
            return null;
        } else {
            return cursor.getString(cursor.getColumnIndex(Fields.STANZA_ID));
        }
    }

    static boolean getReceivedFromMessageArchive(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Fields.IS_RECEIVED_FROM_MESSAGE_ARCHIVE)) != 0;
    }
}