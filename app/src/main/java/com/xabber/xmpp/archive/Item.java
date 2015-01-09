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

public class Item extends AbstractSettings {

	public static final String ELEMENT_NAME = "item";

	public static final String EXACTMATCH_ATTRIBUTE = "exactmatch";
	public static final String JID_ATTRIBUTE = "jid";

	private Boolean exactmatch;
	private String jid;

	@Override
	public boolean isValid() {
		return super.isValid() && jid != null;
	}

	@Override
	String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	protected void serializeAttributes(XmlSerializer serializer)
			throws IOException {
		if (exactmatch != null)
			SerializerUtils.setBooleanAttribute(serializer,
					EXACTMATCH_ATTRIBUTE, exactmatch);
		SerializerUtils.setTextAttribute(serializer, JID_ATTRIBUTE, jid);
		super.serializeAttributes(serializer);
	}

	public Boolean getExactmatch() {
		return exactmatch;
	}

	public void setExactmatch(Boolean exactmatch) {
		this.exactmatch = exactmatch;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

}
