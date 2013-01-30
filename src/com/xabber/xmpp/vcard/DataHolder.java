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

/**
 * Holder for the some object.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
class DataHolder<T extends Instance> implements Instance {

	private T payload;

	@Override
	public boolean isValid() {
		return payload != null && payload.isValid();
	}

	public T getPayload() {
		return payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		// TODO:
		throw new UnsupportedOperationException();
	}

}