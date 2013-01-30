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
package com.xabber.android.data.account;

/**
 * Message archive mode used for account.
 * 
 * @author alexander.ivanov
 * 
 */
public enum ArchiveMode {

	/**
	 * Load history from server, perform replication on connection. Don't use
	 * local storage.
	 */
	server,

	/**
	 * If server side archive is unavailable then WORKS like {@link #local}.
	 * 
	 * If server side archive becomes available and there is NO messages in
	 * local storage then this mode will be REPLACED with {@link #server}.
	 * 
	 * If server side archive becomes available and there are messages in local
	 * storage then user should make a decision whether {@link #server} or
	 * {@link #local} should be used.
	 */
	available,

	/**
	 * Don't load history from the server. Use local storage.
	 */
	local,

	/**
	 * Don't load history from the server. Store only unread messages locally.
	 */
	unreadOnly,

	/**
	 * Don't load history from the server. Don't store messages locally.
	 */
	dontStore;

	public boolean saveLocally() {
		return this == available || this == local;
	}

}
