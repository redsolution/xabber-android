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

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;

/**
 * Supported security mode.
 * 
 * @author alexander.ivanov
 * 
 */
public enum TLSMode {

	/**
	 * Security via TLS encryption is used whenever it's available.
	 */
	enabled,

	/**
	 * Security via TLS encryption is required in order to connect.
	 */
	required,

	/**
	 * Security via old SSL based encryption is enabled. If the server does not
	 * handle legacy-SSL, the connection to the server will fail.
	 */
	legacy;

	SecurityMode getSecurityMode() {
		if (this == enabled)
			return SecurityMode.enabled;
		else if (this == required)
			return SecurityMode.required;
		else if (this == legacy)
			return SecurityMode.legacy;
		else
			throw new IllegalStateException();
	}

}
