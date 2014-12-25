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
package com.xabber.xmpp.delay;

import java.util.Date;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DelayInformation;

import com.xabber.xmpp.address.Jid;

/**
 * Helper class to get delay information.
 * 
 * @author alexander.ivanov
 * 
 */
public class Delay {

	private Delay() {
	}

	/**
	 * @param packet
	 * @return Delay value from packet. <code>null</code> if no delay is
	 *         specified.
	 */
	public static Date getDelay(Packet packet) {
		DelayInformation delay = (DelayInformation) packet.getExtension(
				"delay", "urn:xmpp:delay");
		// If there was no delay based on XEP-0203, then try XEP-0091 for
		// backward compatibility
		if (delay == null) {
			delay = (DelayInformation) packet.getExtension("x",
					"jabber:x:delay");
		}
		if (delay == null)
			return null;
		else
			return delay.getStamp();
	}

	/**
	 * @param server
	 * @param packet
	 * @return Whether message was delayed by server.
	 */
	public static boolean isOfflineMessage(String server, Packet packet) {
		for (PacketExtension extension : packet.getExtensions())
			if (extension instanceof DelayInformation) {
				String from = ((DelayInformation) extension).getFrom();
				if (server.equals(Jid.getStringPrep(from)))
					return true;
			}
		return false;
	}

}
