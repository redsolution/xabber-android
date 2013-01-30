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
package com.xabber.xmpp.receipt;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;

/**
 * Request receipt extension.
 * 
 * http://xmpp.org/extensions/xep-0184.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Request extends PacketExtension {

	public static final String ELEMENT_NAME = "request";
	public static final String NAMESPACE = "urn:xmpp:receipts";

	public Request() {
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
	}

	@Override
	public boolean isValid() {
		return true;
	}

}
