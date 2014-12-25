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
 * Modification item inside the {@link Modified}.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractModified implements CollectionHeader, Instance {

	static final String START_ATTRIBUTE = "start";
	static final String VERSION_ATTRIBUTE = "version";
	static final String WITH_ATTRIBUTE = "with";

	private Date start;
	private String startString;
	private Integer version;
	private String with;

	public AbstractModified() {
	}

	abstract String getElementName();

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, getElementName());
		if (startString != null)
			SerializerUtils.setTextAttribute(serializer, START_ATTRIBUTE,
					startString);
		else
			SerializerUtils.setDateTimeAttribute(serializer, START_ATTRIBUTE,
					start);
		SerializerUtils.setIntegerAttribute(serializer, VERSION_ATTRIBUTE,
				version);
		SerializerUtils.setTextAttribute(serializer, WITH_ATTRIBUTE, with);
		serializer.endTag(null, getElementName());
	}

	@Override
	public boolean isValid() {
		return start != null && version != null && with != null;
	}

	@Override
	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	@Override
	public String getStartString() {
		return startString;
	}

	public void setStartString(String startString) {
		this.startString = startString;
	}

	@Override
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public String getWith() {
		return with;
	}

	public void setWith(String with) {
		this.with = with;
	}

}
