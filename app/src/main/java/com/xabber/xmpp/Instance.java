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
package com.xabber.xmpp;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * Representation of some XML instance.
 * 
 * @author alexander.ivanov
 * 
 */
public interface Instance {

	// TODO: Invalidate iq packets and packet extensions shouldn't be delivered
	// to the ConnectionManager.

	/**
	 * @return Whether parsed instance has valid values (e.g. required field are
	 *         not <code>null</code>).
	 */
	public boolean isValid();

	/**
	 * Serializes an instance into XML using serializer.
	 * 
	 * @param serializer
	 * @throws IOException
	 */
	public void serialize(XmlSerializer serializer) throws IOException;

}
