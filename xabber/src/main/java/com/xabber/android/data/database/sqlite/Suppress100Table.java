package com.xabber.android.data.database.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.xabber.android.data.database.DatabaseManager;

/**
 * Storage with suppress100 settings for each chat.
 * @author <a href="mailto:jaro.fietz@uniscon.de">Jaro Fietz</a>.
 */
public class Suppress100Table extends AbstractChatPropertyTable<Boolean> {

    static final String NAME = "chat_suppress_100";

    private static Suppress100Table instance;

    public static Suppress100Table getInstance() {
        if (instance == null) {
            instance = new Suppress100Table(DatabaseManager.getInstance());
        }

        return instance;
    }

    private Suppress100Table(DatabaseManager databaseManager) {
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
            case 68:
                initialMigrate(db, NAME, "INTEGER");
                break;
            default:
                break;
        }
    }

    public static boolean getValue(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields.VALUE)) != 0;
    }

}
