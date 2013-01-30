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
 * Representation of XML tag.
 * 
 * @author alexander.ivanov
 * 
 */
public interface Container extends Instance {

	/**
	 * @return XML element name.
	 */
	public String getElementName();

	/**
	 * @return XML namespace.
	 */
	public String getNamespace();

	/**
	 * Serializes an inner content of XML tag.
	 * 
	 * @param serializer
	 * @throws IOException
	 */
	public abstract void serializeContent(XmlSerializer serializer)
			throws IOException;

}
