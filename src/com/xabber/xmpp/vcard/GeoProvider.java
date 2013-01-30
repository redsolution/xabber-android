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
import com.xabber.xmpp.ProviderUtils;

class GeoProvider extends AbstractProvider<Geo> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Geo instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (Geo.LAT_NAME.equals(parser.getName()))
			instance.setLat(ProviderUtils.parseText(parser));
		else if (Geo.LON_NAME.equals(parser.getName()))
			instance.setLon(ProviderUtils.parseText(parser));
		else
			return false;
		return true;
	}

	@Override
	protected Geo createInstance(XmlPullParser parser) {
		return new Geo();
	}

	private GeoProvider() {
	}

	private static final GeoProvider instance = new GeoProvider();

	public static GeoProvider getInstance() {
		return instance;
	}

}
