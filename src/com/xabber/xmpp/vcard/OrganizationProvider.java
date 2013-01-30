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

class OrganizationProvider extends AbstractProvider<Organization> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Organization instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (Organization.ORGNAME_NAME.equals(parser.getName()))
			instance.setName(ProviderUtils.parseText(parser));
		else if (Organization.ORGUNIT_NAME.equals(parser.getName()))
			instance.getUnits().add(ProviderUtils.parseText(parser));
		else
			return false;
		return true;
	}

	@Override
	protected Organization createInstance(XmlPullParser parser) {
		return new Organization();
	}

	private OrganizationProvider() {
	}

	private static final OrganizationProvider instance = new OrganizationProvider();

	public static OrganizationProvider getInstance() {
		return instance;
	}

}
