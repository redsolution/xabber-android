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

import java.util.Collection;
import java.util.NoSuchElementException;

import com.xabber.xmpp.ssn.DisclosureValue;
import com.xabber.xmpp.ssn.LoggingValue;
import com.xabber.xmpp.ssn.SecurityValue;

/**
 * User's default setting for OTR Mode.
 * 
 * http://xmpp.org/extensions/xep-0136.html#pref-syntax-default-otr
 * 
 * @author alexander.ivanov
 * 
 */
public enum OtrMode {

	/**
	 * The user MUST explicitly approve off-the-record communication.
	 */
	approve(new LoggingValue[] { LoggingValue.mustnot, LoggingValue.may }),

	/**
	 * Communications MAY be off the record if requested by another user.
	 */
	concede(new LoggingValue[] { LoggingValue.may, LoggingValue.mustnot }),

	/**
	 * Communications MUST NOT be off the record.
	 */
	forbid(new LoggingValue[] { LoggingValue.may }),

	/**
	 * Communications SHOULD NOT be off the record even if requested.
	 */
	oppose(new LoggingValue[] { LoggingValue.may, LoggingValue.mustnot }),

	/**
	 * Communications SHOULD be off the record if possible.
	 */
	prefer(new LoggingValue[] { LoggingValue.mustnot, LoggingValue.may }),

	/**
	 * Communications MUST be off the record.
	 */
	require(new LoggingValue[] { LoggingValue.mustnot });

	private final LoggingValue[] loggingValues;

	private OtrMode(LoggingValue[] loggingValues) {
		this.loggingValues = loggingValues;
	}

	public LoggingValue[] getLoggingValues() {
		return loggingValues;
	}

	public DisclosureValue getDisclosureValue() {
		if (this == require) {
			return DisclosureValue.never;
		} else if (this == prefer || this == approve || this == concede) {
			return DisclosureValue.disabled;
		} else if (this == oppose || this == forbid) {
			return DisclosureValue.enabled;
		} else
			throw new IllegalStateException();
	}

	public SecurityValue getSecurityValue() {
		if (this == require || this == prefer || this == approve
				|| this == concede) {
			return SecurityValue.c2s;
		} else if (this == oppose || this == forbid) {
			return SecurityValue.none;
		} else
			throw new IllegalStateException();
	}

	public boolean acceptLoggingValue(LoggingValue loggingValue) {
		for (LoggingValue check : loggingValues)
			if (check == loggingValue)
				return true;
		return false;
	}

	public LoggingValue selectLoggingValue(Collection<LoggingValue> values) {
		if (this == require) {
			if (values.contains(LoggingValue.mustnot))
				return LoggingValue.mustnot;
			else
				return null;
		} else if (this == prefer) {
			if (values.contains(LoggingValue.mustnot))
				return LoggingValue.mustnot;
			else if (values.contains(LoggingValue.may))
				return LoggingValue.may;
			else
				return null;
		} else if (this == approve || this == concede) {
			if (values.isEmpty())
				return null;
			else
				return values.iterator().next();
		} else if (this == oppose) {
			if (values.contains(LoggingValue.may))
				return LoggingValue.may;
			else if (values.contains(LoggingValue.mustnot))
				return LoggingValue.mustnot;
			else
				return null;
		} else if (this == forbid) {
			if (values.contains(LoggingValue.may))
				return LoggingValue.may;
			else
				return null;
		} else
			throw new IllegalStateException();
	}

	public static OtrMode fromString(String value)
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
