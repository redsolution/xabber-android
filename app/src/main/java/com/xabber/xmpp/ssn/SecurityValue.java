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
package com.xabber.xmpp.ssn;

import java.util.NoSuchElementException;

import org.jivesoftware.smackx.FormField.Option;

/**
 * Security parameter values.
 * 
 * http://xmpp.org/extensions/xep-0155.html#parameters
 * 
 * @author alexander.ivanov
 * 
 */
public enum SecurityValue {

	none("Secure connections not required"),

	c2s("Both parties must be securely connected to their servers"),

	e2e("Both parties must be securely connected to each other");

	private final String label;

	private SecurityValue(String label) {
		this.label = label;
	}

	public Option createOption() {
		return new Option(label, name());
	}

	public static SecurityValue fromString(String value)
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
