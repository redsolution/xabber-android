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


import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.entity.AccountJid;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Table with account related information.
 *
 * @author alexander.ivanov
 */
public abstract class AbstractAccountTable extends AbstractTable {

    public interface Fields extends BaseColumns {

        String ACCOUNT = "account";

    }

    /**
     * Remove records with specified account.
     *
     * @param account
     */
    public void removeAccount(AccountJid account) {
        SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
        db.delete(getTableName(), Fields.ACCOUNT + " = ?",
                new String[]{account.toString()});
    }

    public static String getAccount(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(Fields.ACCOUNT));
    }

}
