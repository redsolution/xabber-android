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

public class SessionProvider extends AbstractProvider<Session> {

	@Override
	protected Session createInstance(XmlPullParser parser) {
		return new Session();
	}

	@Override
	protected Session preProcess(XmlPullParser parser, Session instance) {
		instance.setTimeout(ProviderUtils.parseInteger(parser
				.getAttributeValue(null, Session.TIMEOUT_ATTRIBUTE)));
		instance.setThread(parser.getAttributeValue(null,
				Session.THREAD_ATTRIBUTE));
		try {
			instance.setSave(SaveMode.fromString(parser.getAttributeValue(null,
					Session.SAVE_ATTRIBUTE)));
		} catch (NoSuchElementException e) {
		}
		return super.preProcess(parser, instance);
	}

	private SessionProvider() {
	}

	private static final SessionProvider instance = new SessionProvider();

	public static SessionProvider getInstance() {
		return instance;
	}

}
