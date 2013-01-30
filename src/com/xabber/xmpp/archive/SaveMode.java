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
 * Save mode as specified in http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public enum SaveMode {

	/**
	 * The saving entity SHOULD save only <body/> elements.
	 */
	body("body"),

	/**
	 * The saving entity MUST save nothing.
	 */
	fls("false"),

	/**
	 * The saving entity SHOULD save the full XML content of each <message/>
	 * element.
	 */
	message("message"),

	/**
	 * The saving entity SHOULD save every byte that passes over the stream in
	 * either direction.
	 */
	stream("stream");

	private String value;

	private SaveMode(String name) {
		this.value = name;
	}

	@Override
	public String toString() {
		return value;
	}

	public static SaveMode fromString(String value)
			throws NoSuchElementException {
		for (SaveMode mode : values())
			if (mode.value.equals(value))
				return mode;
		throw new NoSuchElementException();
	}

}
