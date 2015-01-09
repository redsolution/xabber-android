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
package com.xabber.xmpp.archive;

import java.util.NoSuchElementException;

/**
 * Type of archive method.
 * 
 * @author alexander.ivanov
 * 
 */
public enum TypeMode {

	/**
	 * Preferences for use of automatic archiving on the user's server.
	 */
	auto,

	/**
	 * Preferences for use of local archiving to a file or database on the
	 * user's machine or device.
	 */
	local,

	/**
	 * Preferences for use of manual archiving by the user's client to the
	 * user's server.
	 */
	manual;

	public static TypeMode fromString(String value)
			throws NoSuchElementException {
		if (value == null)
			throw new NoSuchElementException();
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new NoSuchElementException();
		}
	}

}
