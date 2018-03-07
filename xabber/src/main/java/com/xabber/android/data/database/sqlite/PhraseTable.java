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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.xabber.android.data.database.DatabaseManager;

/**
 * Storage with phrase notification settings.
 *
 * @author alexander.ivanov
 */
public class PhraseTable extends AbstractTable {

    private static final class Fields implements BaseColumns {

        private Fields() {
        }

        /**
         * Text pattern.
         */
        public static final String VALUE = "value";

        /**
         * JID pattern.
         */
        public static final String USER = "user";

        /**
         * Contact list group pattern.
         */
        public static final String GROUP = "_group";

        /**
         * Whether text should be processed as regexp.
         */
        public static final String REGEXP = "regexp";

        /**
         * Used sound.
         */
        public static final String SOUND = "sound";

    }

    private static final String NAME = "phrase";
    private static final String[] PROJECTION = new String[]{Fields._ID,
            Fields.VALUE, Fields.USER, Fields.GROUP, Fields.REGEXP,
            Fields.SOUND,};

    private final DatabaseManager databaseManager;

    private static PhraseTable instance;

    public static PhraseTable getInstance() {
        if (instance == null) {
            instance = new PhraseTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private PhraseTable(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void create(SQLiteDatabase db) {
        String sql;
        sql = "CREATE TABLE " + NAME + " (" + Fields._ID
                + " INTEGER PRIMARY KEY," + Fields.VALUE + " TEXT,"
                + Fields.USER + " TEXT," + Fields.GROUP + " TEXT,"
                + Fields.REGEXP + " INTEGER," + Fields.SOUND + " TEXT);";
        DatabaseManager.execSQL(db, sql);
    }

    @Override
    public void migrate(SQLiteDatabase db, int toVersion) {
        super.migrate(db, toVersion);
        String sql;
        switch (toVersion) {
            case 63:
                sql = "CREATE TABLE phrase (_id INTEGER PRIMARY KEY,"
                        + "value TEXT," + "regexp INTEGER," + "sound TEXT);";
                DatabaseManager.execSQL(db, sql);
                break;
            case 64:
                sql = "ALTER TABLE phrase ADD COLUMN user TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE phrase SET user = \"\";";
                DatabaseManager.execSQL(db, sql);
                sql = "ALTER TABLE phrase ADD COLUMN _group TEXT;";
                DatabaseManager.execSQL(db, sql);
                sql = "UPDATE phrase SET _group = \"\";";
                DatabaseManager.execSQL(db, sql);
                break;
            default:
                break;
        }
    }

    public long write(Long id, String value, String user, String group,
               boolean regexp, Uri sound) {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Fields.VALUE, value);
        values.put(Fields.USER, user);
        values.put(Fields.GROUP, group);
        values.put(Fields.REGEXP, regexp ? 1 : 0);
        values.put(Fields.SOUND, sound.toString());
        if (id == null)
            return db.insert(NAME, Fields.VALUE, values);
        db.update(NAME, values, Fields._ID + " = ?",
                new String[]{String.valueOf(id)});
        return id;
    }

    public void remove(long id) {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        db.delete(NAME, Fields._ID + " = ?",
                new String[]{String.valueOf(id)});
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
        return Fields._ID;
    }

    public static long getId(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields._ID));
    }

    public static String getValue(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.VALUE));
    }

    public static String getUser(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.USER));
    }

    public static String getGroup(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.GROUP));
    }

    public static boolean isRegexp(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields.REGEXP)) != 0;
    }

    public static Uri getSound(Cursor cursor) {
        return Uri.parse(cursor.getString(cursor.getColumnIndex(Fields.SOUND)));
    }

}