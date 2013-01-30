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
package com.xabber.xmpp.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;

public class TimeProvider extends AbstractIQProvider<Time> {

	private final static Pattern TZO = Pattern.compile(
			"^([+-])(\\d{2}):(\\d{2})$|^(Z)$", Pattern.CASE_INSENSITIVE);

	@Override
	protected boolean parseInner(XmlPullParser parser, Time instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (Time.TZO_NAME.equals(name)) {
			String value = ProviderUtils.parseText(parser);
			Matcher matcher = TZO.matcher(value);
			if (matcher.matches()) {
				String z = matcher.group(4);
				if (z == null) {
					int hours = Integer.valueOf(matcher.group(2));
					int minutes = Integer.valueOf(matcher.group(3));
					if (hours >= 0 && hours <= 23 && minutes >= 0
							&& minutes <= 59) {
						int tzo = hours * 60 + minutes;
						if ("-".equals(matcher.group(1)))
							tzo = -tzo;
						instance.setTzo(tzo);
					}
				} else {
					instance.setTzo(0);
				}
			}
		} else if (Time.UTC_NAME.equals(name)) {
			instance.setUtc(ProviderUtils.parseDateTime(parser));
		} else
			return false;
		return true;
	}

	@Override
	protected Time createInstance(XmlPullParser parser) {
		return new Time();
	}

}
