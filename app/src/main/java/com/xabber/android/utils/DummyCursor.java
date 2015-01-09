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
package com.xabber.android.utils;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

public class DummyCursor implements Cursor {

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public int getPosition() {
		return 0;
	}

	@Override
	public boolean move(int paramInt) {
		return false;
	}

	@Override
	public boolean moveToPosition(int paramInt) {
		return false;
	}

	@Override
	public boolean moveToFirst() {
		return false;
	}

	@Override
	public boolean moveToLast() {
		return false;
	}

	@Override
	public boolean moveToNext() {
		return false;
	}

	@Override
	public boolean moveToPrevious() {
		return false;
	}

	@Override
	public boolean isFirst() {
		return false;
	}

	@Override
	public boolean isLast() {
		return false;
	}

	@Override
	public boolean isBeforeFirst() {
		return false;
	}

	@Override
	public boolean isAfterLast() {
		return false;
	}

	@Override
	public int getColumnIndex(String paramString) {
		return 0;
	}

	@Override
	public int getColumnIndexOrThrow(String paramString)
			throws IllegalArgumentException {
		return 0;
	}

	@Override
	public String getColumnName(int paramInt) {
		return null;
	}

	@Override
	public String[] getColumnNames() {
		return null;
	}

	@Override
	public int getColumnCount() {
		return 0;
	}

	@Override
	public byte[] getBlob(int paramInt) {
		return null;
	}

	@Override
	public String getString(int paramInt) {
		return null;
	}

	@Override
	public void copyStringToBuffer(int paramInt,
			CharArrayBuffer paramCharArrayBuffer) {
	}

	@Override
	public short getShort(int paramInt) {
		return 0;
	}

	@Override
	public int getInt(int paramInt) {
		return 0;
	}

	@Override
	public long getLong(int paramInt) {
		return 0;
	}

	@Override
	public float getFloat(int paramInt) {
		return 0;
	}

	@Override
	public double getDouble(int paramInt) {
		return 0;
	}

	@Override
	public boolean isNull(int paramInt) {
		return false;
	}

	@Override
	public void deactivate() {
	}

	@Override
	public boolean requery() {
		return false;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void registerContentObserver(ContentObserver paramContentObserver) {
	}

	@Override
	public void unregisterContentObserver(ContentObserver paramContentObserver) {
	}

	@Override
	public void registerDataSetObserver(DataSetObserver paramDataSetObserver) {
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver paramDataSetObserver) {
	}

	@Override
	public void setNotificationUri(ContentResolver paramContentResolver,
			Uri paramUri) {
	}

	@Override
	public boolean getWantsAllOnMoveCalls() {
		return false;
	}

	@Override
	public Bundle getExtras() {
		return null;
	}

	@Override
	public Bundle respond(Bundle paramBundle) {
		return null;
	}

}
