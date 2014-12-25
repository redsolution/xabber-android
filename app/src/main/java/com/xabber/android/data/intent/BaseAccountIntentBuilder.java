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
package com.xabber.android.data.intent;

import android.content.Context;
import android.content.Intent;

class BaseAccountIntentBuilder<T extends BaseAccountIntentBuilder<?>> extends
		SegmentIntentBuilder<T> {

	private String account;

	public BaseAccountIntentBuilder(Context context, Class<?> cls) {
		super(context, cls);
	}

	@SuppressWarnings("unchecked")
	public T setAccount(String account) {
		this.account = account;
		return (T) this;
	}

	@Override
	void preBuild() {
		super.preBuild();
		if (account == null)
			return;
		if (getSegmentCount() != 0)
			throw new IllegalStateException();
		addSegment(account);
	}

	public static String getAccount(Intent intent) {
		return getSegment(intent, 0);
	}

}