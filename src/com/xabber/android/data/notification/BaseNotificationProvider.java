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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.xabber.android.data.SettingsManager;

import android.media.AudioManager;
import android.net.Uri;

/**
 * Base provider for the notifications to be displayed.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public class BaseNotificationProvider<T extends NotificationItem> implements
		NotificationProvider<T> {

	protected final Collection<T> items;
	private final int icon;
	private boolean canClearNotifications;

	public BaseNotificationProvider(int icon) {
		super();
		this.items = new ArrayList<T>();
		this.icon = icon;
		canClearNotifications = true;
	}

	/**
	 * Adds new or update existed notification item and update notification bar.
	 * 
	 * @param item
	 * @param notify
	 *            <code>null</code> if notification ticker should be shown only
	 *            if such notification does exists yet.
	 */
	public void add(T item, Boolean notify) {
		boolean exists = items.remove(item);
		if (notify == null)
			notify = !exists;
		items.add(item);
		NotificationManager.getInstance().updateNotifications(this,
				notify ? item : null);
	}

	/**
	 * Removes notification and update notification bar.
	 * 
	 * @param item
	 * @return
	 */
	public boolean remove(T item) {
		boolean result = items.remove(item);
		if (result)
			NotificationManager.getInstance().updateNotifications(this, null);
		return result;
	}

	public void setCanClearNotifications(boolean canClearNotifications) {
		this.canClearNotifications = canClearNotifications;
	}

	@Override
	public Collection<T> getNotifications() {
		return Collections.unmodifiableCollection(items);
	}

	@Override
	public boolean canClearNotifications() {
		return canClearNotifications;
	}

	@Override
	public void clearNotifications() {
		items.clear();
	}

	@Override
	public Uri getSound() {
		return SettingsManager.eventsSound();
	}

	@Override
	public int getStreamType() {
		return AudioManager.STREAM_NOTIFICATION;
	}

	@Override
	public int getIcon() {
		return icon;
	}

}
