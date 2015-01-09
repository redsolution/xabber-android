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
package com.xabber.android.data.notification;

public class EntityNotificationProvider<T extends EntityNotificationItem>
		extends BaseAccountNotificationProvider<T> {

	public EntityNotificationProvider(int icon) {
		super(icon);
	}

	@Override
	public T get(String account) {
		throw new UnsupportedOperationException();
	}

	public T get(String account, String user) {
		for (T item : items)
			if (item.getAccount().equals(account)
					&& item.getUser().equals(user))
				return item;
		return null;
	}

	public boolean remove(String account, String user) {
		return remove(get(account, user));
	}

}
