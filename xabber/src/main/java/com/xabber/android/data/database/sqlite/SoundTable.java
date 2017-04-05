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
import android.net.Uri;

import com.xabber.android.data.database.DatabaseManager;

/**
 * Storage with sound associated with chat.
 *
 * @author alexander.ivanov
 */
public class SoundTable extends AbstractChatPropertyTable<Uri> {

    static final String NAME = "chat_sound";

    private static SoundTable instance;

    public static SoundTable getInstance() {
        if (instance == null) {
            instance = new SoundTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private SoundTable(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected String getTableName() {
        return NAME;
    }

    @Override
    String getValueType() {
        return "TEXT";
    }

    @Override
    void bindValue(SQLiteStatement writeStatement, Uri value) {
        writeStatement.bindString(3, value.toString());
    }

    @Override
    public void migrate(SQLiteDatabase db, int toVersion) {
        super.migrate(db, toVersion);
        switch (toVersion) {
            case 52:
                initialMigrate(db, "chat_sound", "TEXT");
                break;
            default:
                break;
        }
    }

    public static Uri getValue(Cursor cursor) {
        return Uri.parse(cursor.getString(cursor.getColumnIndex(Fields.VALUE)));
    }

}
