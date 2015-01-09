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

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.ProviderUtils;

class AddressProvider extends AbstractTypedDataProvider<AddressType, Address> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Address instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		for (AddressProperty key : AddressProperty.values())
			if (key.toString().equals(name)) {
				instance.getProperties().put(key,
						ProviderUtils.parseText(parser));
				return true;
			}
		return false;
	}

	@Override
	protected AddressType[] getTypes() {
		return AddressType.values();
	}

	@Override
	protected Address createInstance(XmlPullParser parser) {
		return new Address();
	}

	private AddressProvider() {
	}

	private static final AddressProvider instance = new AddressProvider();

	public static AddressProvider getInstance() {
		return instance;
	}

}
