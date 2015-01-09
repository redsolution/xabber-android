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
package com.xabber.android.data.connection;

import com.xabber.androiddev.R;

/**
 * State of connection.
 * 
 * @author alexander.ivanov
 * 
 */
public enum ConnectionState {

	/**
	 * Connection is not active.
	 */
	offline,

	/**
	 * Waiting for connection before first connection or before reconnection.
	 */
	waiting,

	/**
	 * Connection is in progress.
	 */
	connecting,

	/**
	 * Connection was established, authentication is in progress.
	 */
	authentication,

	/**
	 * Authorized connection has been established.
	 */
	connected;

	/**
	 * @return whether authorized connection has been established.
	 */
	public boolean isConnected() {
		return this == ConnectionState.connected;
	}

	/**
	 * @return whether connection has already been established or will be
	 *         established later.
	 */
	public boolean isConnectable() {
		return this != ConnectionState.offline;
	}

	/**
	 * @return Resource id with associated string.
	 */
	public int getStringId() {
		if (this == ConnectionState.offline)
			return R.string.account_state_offline;
		else if (this == ConnectionState.waiting)
			return R.string.account_state_waiting;
		else if (this == ConnectionState.connecting)
			return R.string.account_state_connecting;
		else if (this == ConnectionState.authentication)
			return R.string.account_state_authentication;
		else if (this == ConnectionState.connected)
			return R.string.account_state_connected;
		else
			throw new IllegalStateException();
	}

}
