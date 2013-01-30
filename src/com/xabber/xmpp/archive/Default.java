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

import com.xabber.xmpp.SerializerUtils;

public class Default extends AbstractSettings {

	public static final String ELEMENT_NAME = "default";

	public static final String UNSET_ATTRIBUTE = "unset";

	private boolean unset;

	@Override
	public boolean isValid() {
		return super.isValid() && getOtr() != null && getSave() != null;
	}

	@Override
	protected String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	void serializeAttributes(XmlSerializer serializer) throws IOException {
		if (unset)
			SerializerUtils.setBooleanAttribute(serializer, UNSET_ATTRIBUTE,
					unset);
		super.serializeAttributes(serializer);
	}

	public boolean isUnset() {
		return unset;
	}

	public void setUnset(boolean unset) {
		this.unset = unset;
	}

}
