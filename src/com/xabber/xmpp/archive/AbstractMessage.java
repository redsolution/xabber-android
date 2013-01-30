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

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

/**
 * Message item inside the {@link Chat}.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractMessage implements Instance {

	static final String BODY_NAME = "body";

	static final String JID_ATTRIBUTE = "jid";
	static final String NAME_ATTRIBUTE = "name";
	static final String SECS_ATTRIBUTE = "secs";
	static final String UTC_ATTRIBUTE = "utc";

	private String jid;
	private String name;
	private Integer secs;
	private Date utc;
	private String body;

	// TODO: bodies

	@Override
	public boolean isValid() {
		return body != null && (utc != null || secs != null);
	}

	abstract String getElementName();

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, getElementName());
		if (jid != null)
			SerializerUtils.setTextAttribute(serializer, JID_ATTRIBUTE, jid);
		if (name != null)
			SerializerUtils.setTextAttribute(serializer, NAME_ATTRIBUTE, name);
		if (secs != null)
			SerializerUtils.setIntegerAttribute(serializer, SECS_ATTRIBUTE,
					secs);
		if (utc != null)
			SerializerUtils
					.setDateTimeAttribute(serializer, UTC_ATTRIBUTE, utc);
		if (body != null)
			SerializerUtils.addTextTag(serializer, BODY_NAME, body);
		serializer.endTag(null, getElementName());
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSecs() {
		return secs;
	}

	public void setSecs(Integer secs) {
		this.secs = secs;
	}

	public Date getUtc() {
		return utc;
	}

	public void setUtc(Date utc) {
		this.utc = utc;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}
