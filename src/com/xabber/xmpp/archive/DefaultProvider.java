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

public class DefaultProvider extends AbstractSettingsProvider<Default> {

	@Override
	protected Default createInstance(XmlPullParser parser) {
		return new Default();
	}

	@Override
	protected Default preProcess(XmlPullParser parser, Default instance) {
		Boolean unset = ProviderUtils.parseBoolean(parser.getAttributeValue(
				null, Default.UNSET_ATTRIBUTE));
		instance.setUnset(unset == null ? false : unset);
		return super.preProcess(parser, instance);
	}

	private DefaultProvider() {
	}

	private static final DefaultProvider instance = new DefaultProvider();

	public static DefaultProvider getInstance() {
		return instance;
	}

}
