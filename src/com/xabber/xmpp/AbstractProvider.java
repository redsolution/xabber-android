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

import org.xmlpull.v1.XmlPullParser;

/**
 * Provide common interface to create new object from received XML.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public abstract class AbstractProvider<T extends Instance> extends
		AbstractInflater<T> {

	/**
	 * Creates an instance.
	 * 
	 * Parser position mustn't be changed.
	 * 
	 * @param parser
	 * @return
	 */
	abstract protected T createInstance(XmlPullParser parser);

	/**
	 * Parse XML tag and create instance.
	 * 
	 * @param parser
	 *            an XML parser.
	 * @return new instance.
	 * @throws Exception
	 *             if an error occurs while parsing.
	 */
	public T provideInstance(XmlPullParser parser) throws Exception {
		T instance = createInstance(parser);
		return parseTag(parser, instance);
	}

}
