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

import com.xabber.xmpp.SerializerUtils;

public abstract class AbstractTypedDataWithValue<Type extends Enum<?>> extends
		AbstractTypedData<Type> {

	private String value;

	public AbstractTypedDataWithValue() {
		super();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	protected abstract String getValueName();

	@Override
	protected void writeBody(XmlSerializer serializer) throws IOException {
		SerializerUtils.addTextTag(serializer, getValueName(), value);
	}

	@Override
	public boolean isValid() {
		return value != null;
	}

}
