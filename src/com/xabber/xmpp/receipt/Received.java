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
import com.xabber.xmpp.SerializerUtils;

/**
 * Receipt extension.
 * 
 * http://xmpp.org/extensions/xep-0184.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Received extends PacketExtension {

	public static final String ELEMENT_NAME = "received";
	public static final String NAMESPACE = Request.NAMESPACE;
	public static final String ID_ATTRIBUTE = "id";

	private final String id;

	public Received(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
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
		SerializerUtils.setTextAttribute(serializer, ID_ATTRIBUTE, id);
	}

	@Override
	public boolean isValid() {
		return id != null;
	}

}
