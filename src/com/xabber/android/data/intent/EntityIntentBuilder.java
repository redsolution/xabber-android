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

/**
 * Intent builder with account and user fields.
 * 
 * @author alexander.ivanov
 * 
 */
public class EntityIntentBuilder extends
		BaseAccountIntentBuilder<EntityIntentBuilder> {

	public EntityIntentBuilder(Context context, Class<?> cls) {
		super(context, cls);
	}

	private String user;

	public EntityIntentBuilder setUser(String user) {
		this.user = user;
		return this;
	}

	@Override
	void preBuild() {
		super.preBuild();
		if (user == null)
			return;
		if (getSegmentCount() == 0)
			throw new IllegalStateException();
		addSegment(user);
	}

	public static String getUser(Intent intent) {
		return getSegment(intent, 1);
	}

}