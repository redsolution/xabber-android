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
import java.util.Date;

import org.jivesoftware.smack.packet.PacketExtension;
import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

public abstract class AbstractChat extends IQ implements PacketExtension {

	static final String NAMESPACE = "urn:xmpp:archive";

	static final String START_ATTRIBUTE = "start";
	static final String WITH_ATTRIBUTE = "with";

	private Date start;
	private String startString;
	private String with;

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (startString != null)
			SerializerUtils.setTextAttribute(serializer, START_ATTRIBUTE,
					startString);
		else if (start != null)
			SerializerUtils.setDateTimeAttribute(serializer, START_ATTRIBUTE,
					start);
		if (with != null)
			SerializerUtils.setTextAttribute(serializer, WITH_ATTRIBUTE, with);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public String getStartString() {
		return startString;
	}

	public void setStartString(String startString) {
		this.startString = startString;
	}

	public String getWith() {
		return with;
	}

	public void setWith(String with) {
		this.with = with;
	}

}
