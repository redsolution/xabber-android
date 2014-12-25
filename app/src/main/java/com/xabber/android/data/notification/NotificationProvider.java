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

import java.util.Collection;

import android.net.Uri;

/**
 * Provides list of notifications first of which should be shown.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public interface NotificationProvider<T extends NotificationItem> {

	/**
	 * @return List of notifications.
	 */
	Collection<T> getNotifications();

	/**
	 * @return Whether notification can be cleared.
	 */
	boolean canClearNotifications();

	/**
	 * Clear notifications.
	 */
	void clearNotifications();

	/**
	 * @return Sound for notification.
	 */
	Uri getSound();

	/**
	 * @return Audio stream type for notification.
	 */
	int getStreamType();

	/**
	 * @return Resource id with icon for notification bar.
	 */
	int getIcon();

}
