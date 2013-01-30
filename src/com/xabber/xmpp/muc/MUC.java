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
package com.xabber.xmpp.muc;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.MUCUser;

/**
 * Helper class to get MUC information.
 * 
 * @author alexander.ivanov
 * 
 */
public class MUC {

	private MUC() {
	}

	/**
	 * Returns the MUCUser packet extension included in the packet or
	 * <tt>null</tt> if none.
	 * 
	 * @param packet
	 *            the packet that may include the MUCUser extension.
	 * @return the MUCUser found in the packet.
	 */
	public static MUCUser getMUCUserExtension(Packet packet) {
		if (packet != null)
			for (PacketExtension extension : packet.getExtensions())
				if (extension instanceof MUCUser)
					return (MUCUser) extension;
		return null;
	}

}
