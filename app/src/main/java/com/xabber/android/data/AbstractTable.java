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
package com.xabber.android.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Abstract database table.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractTable implements DatabaseTable {

	protected abstract String getTableName();

	protected abstract String[] getProjection();

	protected String getListOrder() {
		return null;
	}

	@Override
	public void migrate(SQLiteDatabase db, int toVersion) {
	}

	/**
	 * Query table.
	 * 
	 * @return Result set with defined projection and in defined order.
	 */
	public Cursor list() {
		SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();
		return db.query(getTableName(), getProjection(), null, null, null,
				null, getListOrder());
	}

	@Override
	public void clear() {
		SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
		db.delete(getTableName(), null, null);
	}

}
