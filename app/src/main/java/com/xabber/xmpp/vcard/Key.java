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
package com.xabber.xmpp.vcard;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

public class Key implements Instance {

	/**
	 * Limit received encoded data size.
	 */
	public static final int MAX_ENCODED_DATA_SIZE = 64 * 1024;

	public static final String ELEMENT_NAME = "KEY";
	public static final String TYPE_NAME = "TYPE";
	public static final String CRED_NAME = "CRED";

	private String type;
	private String encodedData;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEncodedData() {
		return encodedData;
	}

	public void setEncodedData(String encodedData) {
		this.encodedData = encodedData;
	}

	@Override
	public boolean isValid() {
		return encodedData != null;
	}

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, ELEMENT_NAME);
		if (type != null)
			SerializerUtils.addTextTag(serializer, TYPE_NAME, type);
		SerializerUtils.addTextTag(serializer, CRED_NAME, encodedData);
		serializer.endTag(null, ELEMENT_NAME);
	}

}
