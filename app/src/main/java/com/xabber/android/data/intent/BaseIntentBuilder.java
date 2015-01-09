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

public class BaseIntentBuilder<T extends BaseIntentBuilder<?>> {

	private final Context context;
	private final Class<?> cls;

	public BaseIntentBuilder(Context context, Class<?> cls) {
		super();
		this.context = context;
		this.cls = cls;
	}

	public Intent build() {
		if (context != null || cls != null)
			return new Intent(context, cls);
		else
			return new Intent();
	}

}