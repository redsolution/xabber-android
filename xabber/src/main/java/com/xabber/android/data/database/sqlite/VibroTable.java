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
import android.database.sqlite.SQLiteStatement;

import com.xabber.android.data.database.DatabaseManager;

/**
 * Storage with vibro settings for each chat.
 *
 * @author alexander.ivanov
 */
public class VibroTable extends AbstractChatPropertyTable<Boolean> {

    static final String NAME = "chat_vibro";

    private static VibroTable instance;

    public static VibroTable getInstance() {
        if (instance == null) {
            instance = new VibroTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private VibroTable(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected String getTableName() {
        return NAME;
    }

    @Override
    String getValueType() {
        return "INTEGER";
    }

    @Override
    void bindValue(SQLiteStatement writeStatement, Boolean value) {
        writeStatement.bindLong(3, value ? 1 : 0);
    }

    @Override
    public void migrate(SQLiteDatabase db, int toVersion) {
        super.migrate(db, toVersion);
        switch (toVersion) {
            case 57:
                initialMigrate(db, "chat_vibro", "INTEGER");
                break;
            default:
                break;
        }
    }

    public static boolean getValue(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields.VALUE)) != 0;
    }

}
