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

import java.util.NoSuchElementException;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractProvider;
import com.xabber.xmpp.ProviderUtils;

public abstract class AbstractSettingsProvider<T extends AbstractSettings>
		extends AbstractProvider<T> {

	@Override
	protected T preProcess(XmlPullParser parser, T instance) {
		instance.setExpire(ProviderUtils.parseInteger(parser.getAttributeValue(
				null, AbstractSettings.EXPIRE_ATTRIBUTE)));
		try {
			instance.setOtr(OtrMode.fromString(parser.getAttributeValue(null,
					AbstractSettings.OTR_ATTRIBUTE)));
		} catch (NoSuchElementException e) {
		}
		try {
			instance.setSave(SaveMode.fromString(parser.getAttributeValue(null,
					AbstractSettings.SAVE_ATTRIBUTE)));
		} catch (NoSuchElementException e) {
		}
		return super.preProcess(parser, instance);
	}

}
