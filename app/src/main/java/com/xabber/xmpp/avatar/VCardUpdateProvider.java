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
package com.xabber.xmpp.avatar;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractExtensionProvider;
import com.xabber.xmpp.ProviderUtils;

public class VCardUpdateProvider extends AbstractExtensionProvider<VCardUpdate> {

	@Override
	protected VCardUpdate createInstance(XmlPullParser parser) {
		return new VCardUpdate();
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, VCardUpdate instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (VCardUpdate.PHOTO_NAME.equals(name))
			instance.setPhotoHash(ProviderUtils.parseText(parser));
		else
			return false;
		return true;
	}

}
