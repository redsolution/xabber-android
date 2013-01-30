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

import com.xabber.xmpp.AbstractProvider;
import com.xabber.xmpp.ProviderUtils;

public abstract class AbstractMessageProvider<T extends AbstractMessage>
		extends AbstractProvider<T> {

	@Override
	protected T preProcess(XmlPullParser parser, T instance) {
		instance.setJid(parser.getAttributeValue(null,
				AbstractMessage.JID_ATTRIBUTE));
		instance.setName(parser.getAttributeValue(null,
				AbstractMessage.NAME_ATTRIBUTE));
		instance.setSecs(ProviderUtils.parseInteger(parser.getAttributeValue(
				null, AbstractMessage.SECS_ATTRIBUTE)));
		instance.setUtc(ProviderUtils.parseDateTime(parser.getAttributeValue(
				null, AbstractMessage.UTC_ATTRIBUTE)));
		return super.preProcess(parser, instance);
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, T instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (AbstractMessage.BODY_NAME.equals(name)) {
			instance.setBody(ProviderUtils.parseText(parser));
		} else
			return false;
		return true;
	}

}
