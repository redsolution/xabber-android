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

public abstract class AbstractTypedDataWithValueProvider<Type extends Enum<?>, Instance extends AbstractTypedDataWithValue<Type>>
		extends AbstractTypedDataProvider<Type, Instance> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Instance instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (getValueName().equals(parser.getName())) {
			instance.setValue(ProviderUtils.parseText(parser));
			return true;
		}
		return false;
	}

	protected abstract String getValueName();

}
