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
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

public class Organization implements Instance {

	public static final String ELEMENT_NAME = "ORG";
	public static final String ORGNAME_NAME = "ORGNAME";
	public static final String ORGUNIT_NAME = "ORGUNIT";

	private String name;
	private final List<String> units;

	public Organization() {
		units = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getUnits() {
		return units;
	}

	@Override
	public boolean isValid() {
		return name != null;
	}

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, ELEMENT_NAME);
		SerializerUtils.addTextTag(serializer, ORGNAME_NAME, name);
		for (String unit : units)
			SerializerUtils.addTextTag(serializer, ORGUNIT_NAME, unit);
		serializer.endTag(null, ELEMENT_NAME);
	}

}
