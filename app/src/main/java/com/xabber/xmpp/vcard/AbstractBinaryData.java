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

import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.SerializerUtils;

public abstract class AbstractBinaryData extends AbstractData {

	/**
	 * Limit received encoded data size.
	 */
	public static final int MAX_ENCODED_DATA_SIZE = 256 * 1024;

	public static final String TYPE_NAME = "TYPE";
	public static final String BINVAL_NAME = "BINVAL";

	private String type;
	private byte[] data;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public boolean isValid() {
		return type != null && data != null;
	}

	@Override
	protected void writeBody(XmlSerializer serializer) throws IOException {
		SerializerUtils.addTextTag(serializer, TYPE_NAME, type);
		SerializerUtils.addTextTag(serializer, BINVAL_NAME,
				StringUtils.encodeBase64(data));
	}

}
