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

import com.xabber.android.data.entity.AccountJid;

import java.util.Iterator;


public class BaseAccountNotificationProvider<T extends AccountNotificationItem>
        extends BaseNotificationProvider<T> implements
        AccountNotificationProvider<T> {

    public BaseAccountNotificationProvider(int icon) {
        super(icon);
    }

    public BaseAccountNotificationProvider(int icon, String channelID) {
        super(icon, channelID);
    }

    public T get(AccountJid account) {
        for (T item : items)
            if (item != null && item.getAccount().equals(account))
                return item;
        return null;
    }

    public boolean remove(AccountJid account) {
        return remove(get(account));
    }

    @Override
    public void clearAccountNotifications(AccountJid account) {
        for (Iterator<T> iterator = items.iterator(); iterator.hasNext(); )
            if (account.equals(iterator.next().getAccount()))
                iterator.remove();
    }

}
