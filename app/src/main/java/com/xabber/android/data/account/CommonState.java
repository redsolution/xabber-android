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
 * Common state of accounts and its connections.
 * 
 * @author alexander.ivanov
 */
public enum CommonState {

	/**
	 * There is no one account.
	 */
	empty,

	/**
	 * There is only disabled accounts.
	 */
	disabled,

	/**
	 * There is only offline or disabled accounts.
	 */
	offline,

	/**
	 * All accounts are in waiting state or in any preceding states.
	 */
	waiting,

	/**
	 * All accounts are in connecting state or in any preceding states.
	 */
	connecting,

	/**
	 * All accounts are waiting for roster or in any preceding states.
	 */
	roster,

	/**
	 * There is at least one account with received roster.
	 */
	online;

}
