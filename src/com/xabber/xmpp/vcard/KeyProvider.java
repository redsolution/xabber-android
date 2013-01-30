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

import com.xabber.xmpp.AbstractProvider;
import com.xabber.xmpp.OverflowReceiverBufferException;
import com.xabber.xmpp.ProviderUtils;

class KeyProvider extends AbstractProvider<Key> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Key instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (Key.TYPE_NAME.equals(parser.getName()))
			instance.setType(ProviderUtils.parseText(parser));
		else if (Key.CRED_NAME.equals(parser.getName())) {
			String value;
			try {
				value = ProviderUtils.parseText(parser,
						Key.MAX_ENCODED_DATA_SIZE);
			} catch (OverflowReceiverBufferException e) {
				return true;
			}
			instance.setEncodedData(value);
		} else
			return false;
		return true;
	}

	@Override
	protected Key createInstance(XmlPullParser parser) {
		return new Key();
	}

	private KeyProvider() {
	}

	private static final KeyProvider instance = new KeyProvider();

	public static KeyProvider getInstance() {
		return instance;
	}

}
