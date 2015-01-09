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

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.rsm.Set;

/**
 * Retrieve message archive collection.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Retrieve extends AbstractChat {

	static final String ELEMENT_NAME = "retrieve";

	private Set rsm;

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		super.serializeContent(serializer);
		if (rsm != null)
			rsm.serialize(serializer);
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	public Set getRsm() {
		return rsm;
	}

	public void setRsm(Set rsm) {
		this.rsm = rsm;
	}

}
