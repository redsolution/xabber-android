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

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

public class Geo implements Instance {

	public static final String ELEMENT_NAME = "GEO";
	public static final String LAT_NAME = "LAT";
	public static final String LON_NAME = "LON";

	private String lat;
	private String lon;

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	@Override
	public boolean isValid() {
		return lat != null && lon != null;
	}

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, ELEMENT_NAME);
		SerializerUtils.addTextTag(serializer, LAT_NAME, lat);
		SerializerUtils.addTextTag(serializer, LON_NAME, lon);
		serializer.endTag(null, ELEMENT_NAME);
	}

}
