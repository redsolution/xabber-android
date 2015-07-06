package com.xabber.android.data.message.chat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.xabber.android.data.DatabaseManager;

/**
 * Storage with suppress100 settings for each chat.
 * @author <a href="mailto:jaro.fietz@uniscon.de">Jaro Fietz</a>.
 */
class Suppress100Table extends AbstractChatPropertyTable<Boolean> {

    static final String NAME = "chat_suppress_100";

    private final static Suppress100Table instance;

    static {
        instance = new Suppress100Table(DatabaseManager.getInstance());
        DatabaseManager.getInstance().addTable(instance);
    }

    public static Suppress100Table getInstance() {
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

    static boolean getValue(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(Fields.VALUE)) != 0;
    }

}
