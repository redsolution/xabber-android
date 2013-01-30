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
package com.xabber.xmpp.address;

import java.util.Locale;

import org.jivesoftware.smack.util.StringUtils;

/**
 * Provides methods to process Jabber Identifier.
 * 
 * Warning: Implementation should be review, methods renamed according to
 * http://xmpp.org/rfcs/rfc6122.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Jid {

	private Jid() {
	}

	/**
	 * @param user
	 * @return Lower cased resource or <code>null</code> for <code>null</code>
	 *         argument.
	 */
	public static String getResource(String user) {
		return user == null ? null : StringUtils.parseResource(user
				.toLowerCase(Locale.US));
	}

	/**
	 * @param user
	 * @return Lower cased server name or <code>null</code> for
	 *         <code>null</code> argument.
	 */
	public static String getServer(String user) {
		return user == null ? null : StringUtils.parseServer(user
				.toLowerCase(Locale.US));
	}

	/**
	 * @param user
	 * @return Lower cased user name part or <code>null</code> for
	 *         <code>null</code> argument.
	 */
	public static String getName(String user) {
		return user == null ? null : StringUtils.parseName(user
				.toLowerCase(Locale.US));
	}

	/**
	 * @param user
	 * @return Lower cased bare address or <code>null</code> for
	 *         <code>null</code> argument.
	 */
	public static String getBareAddress(String user) {
		return user == null ? null : StringUtils.parseBareAddress(user
				.toLowerCase(Locale.US));
	}

	/**
	 * Gets lower cased address.
	 * 
	 * @param user
	 * @return
	 */
	public static String getStringPrep(String user) {
		return user == null ? null : user.toLowerCase(Locale.US);
	}

}
