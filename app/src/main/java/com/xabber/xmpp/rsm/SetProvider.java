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
package com.xabber.xmpp.rsm;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractExtensionProvider;
import com.xabber.xmpp.ProviderUtils;

public class SetProvider extends AbstractExtensionProvider<Set> {

	@Override
	protected Set createInstance(XmlPullParser parser) {
		return new Set();
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, Set instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (Set.AFTER_NAME.equals(parser.getName()))
			instance.setAfter(ProviderUtils.parseText(parser));
		else if (Set.AFTER_NAME.equals(parser.getName()))
			instance.setAfter(ProviderUtils.parseText(parser));
		else if (Set.BEFORE_NAME.equals(parser.getName()))
			instance.setBefore(ProviderUtils.parseText(parser));
		else if (Set.COUNT_NAME.equals(parser.getName()))
			instance.setCount(ProviderUtils.parseInteger(parser));
		else if (Set.FIRST_NAME.equals(parser.getName())) {
			instance.setFirstIndex(ProviderUtils.parseInteger(parser
					.getAttributeValue(null, Set.INDEX_ATTRIBUTE)));
			instance.setFirst(ProviderUtils.parseText(parser));
		} else if (Set.INDEX_NAME.equals(parser.getName()))
			instance.setIndex(ProviderUtils.parseInteger(parser));
		else if (Set.LAST_NAME.equals(parser.getName()))
			instance.setLast(ProviderUtils.parseText(parser));
		else if (Set.MAX_NAME.equals(parser.getName()))
			instance.setMax(ProviderUtils.parseInteger(parser));
		else
			return false;
		return true;
	}

}
