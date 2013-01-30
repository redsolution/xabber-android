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
package com.xabber.xmpp;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Provides common interface to parse iq packets.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public abstract class AbstractIQProvider<T extends IQ> extends
		AbstractProvider<T> implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		return provideInstance(parser);
	}

}
