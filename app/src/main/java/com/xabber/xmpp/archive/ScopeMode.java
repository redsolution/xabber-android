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
 * Scope for auto element.
 * 
 * @author alexander.ivanov
 * 
 */
public enum ScopeMode {

	/**
	 * The setting will remain for next streams.
	 */
	global,

	/**
	 * The setting is true only until the end of the stream.
	 */
	stream;

	public static ScopeMode fromString(String value)
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
