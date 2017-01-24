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
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.message.chat.ShowMessageTextInNotification;

/**
 * Storage with settings to show text in notification for each chat.
 *
 * @author alexander.ivanov
 */
public class ShowTextTable extends AbstractChatPropertyTable<ShowMessageTextInNotification> {

    static final String NAME = "chat_show_text";

    private static ShowTextTable instance;

    public static ShowTextTable getInstance() {
        if (instance == null) {
            instance = new ShowTextTable(DatabaseManager.getInstance());
        }

        return instance;
    }

    private ShowTextTable(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public static ShowMessageTextInNotification getValue(Cursor cursor) {
        return ShowMessageTextInNotification.fromInteger((int) cursor.getLong(cursor.getColumnIndex(Fields.VALUE)));
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
    void bindValue(SQLiteStatement writeStatement, ShowMessageTextInNotification showMessageTextInNotification) {
        writeStatement.bindLong(3, showMessageTextInNotification.ordinal());
    }

    @Override
    public void migrate(SQLiteDatabase db, int toVersion) {
        super.migrate(db, toVersion);
        switch (toVersion) {
            case 52:
                initialMigrate(db, "chat_show_text", "INTEGER");
                break;

            case 67:
                int trueMigrationValue;
                int falseMigrationValue;

                if (SettingsManager.eventsShowText()) {
                    trueMigrationValue = ShowMessageTextInNotification.default_settings.ordinal();
                    falseMigrationValue = ShowMessageTextInNotification.hide.ordinal();
                } else {
                    trueMigrationValue = ShowMessageTextInNotification.show.ordinal();
                    falseMigrationValue = ShowMessageTextInNotification.default_settings.ordinal();
                }


                String sql = "UPDATE " + NAME
                        + " SET " + Fields.VALUE + " = CASE WHEN (" + Fields.VALUE + "=1) THEN "
                        + trueMigrationValue + " ELSE " + falseMigrationValue + " END;";

                DatabaseManager.execSQL(db, sql);

            default:
                break;
        }
    }

}
