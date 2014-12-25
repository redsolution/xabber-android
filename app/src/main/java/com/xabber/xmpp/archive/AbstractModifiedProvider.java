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

public abstract class AbstractModifiedProvider<T extends AbstractModified>
		extends AbstractProvider<T> {

	@Override
	protected T preProcess(XmlPullParser parser, T instance) {
		instance.setStart(ProviderUtils.parseDateTime(parser.getAttributeValue(
				null, AbstractModified.START_ATTRIBUTE)));
		instance.setStartString(parser.getAttributeValue(null,
				AbstractModified.START_ATTRIBUTE));
		instance.setVersion(ProviderUtils.parseInteger(parser
				.getAttributeValue(null, AbstractModified.VERSION_ATTRIBUTE)));
		instance.setWith(parser.getAttributeValue(null,
				AbstractModified.WITH_ATTRIBUTE));
		return super.preProcess(parser, instance);
	}

}
