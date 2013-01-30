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
 * Used methods in message archive.
 * 
 * @author alexander.ivanov
 * 
 */
public enum UseMode {

	/**
	 * This method MAY be used if no other methods are available.
	 */
	concede,

	/**
	 * This method MUST NOT be used.
	 */
	forbid,

	/**
	 * This method SHOULD be used if available.
	 */
	prefer;

	public static UseMode fromString(String value)
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
