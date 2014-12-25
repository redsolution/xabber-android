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

class LabelProvider extends AbstractTypedDataProvider<AddressType, Label> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Label instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (Label.LINE_NAME.equals(parser.getName())) {
			instance.getLines().add(ProviderUtils.parseText(parser));
			return true;
		}
		return false;
	}

	@Override
	protected AddressType[] getTypes() {
		return AddressType.values();
	}

	@Override
	protected Label createInstance(XmlPullParser parser) {
		return new Label();
	}

	private LabelProvider() {
	}

	private static final LabelProvider instance = new LabelProvider();

	public static LabelProvider getInstance() {
		return instance;
	}

}
