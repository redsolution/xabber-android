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
package com.xabber.xmpp.archive;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.ProviderUtils;

public class ItemProvider extends AbstractSettingsProvider<Item> {

	@Override
	protected Item createInstance(XmlPullParser parser) {
		return new Item();
	}

	@Override
	protected Item preProcess(XmlPullParser parser, Item instance) {
		Boolean exactmatch = ProviderUtils.parseBoolean(parser
				.getAttributeValue(null, Item.EXACTMATCH_ATTRIBUTE));
		instance.setExactmatch(exactmatch == null ? false : true);
		instance.setJid(parser.getAttributeValue(null, Item.JID_ATTRIBUTE));
		return super.preProcess(parser, instance);
	}

	private ItemProvider() {
	}

	private static final ItemProvider instance = new ItemProvider();

	public static ItemProvider getInstance() {
		return instance;
	}

}
