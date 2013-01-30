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

import com.xabber.xmpp.SerializerUtils;

/**
 * Packet to remove collection from the message archive.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Remove extends AbstractChat {

	static final String ELEMENT_NAME = "remove";

	static final String EXACTMATCH_ATTRIBUTE = "exactmatch";
	static final String END_ATTRIBUTE = "end";
	static final String OPEN_ATTRIBUTE = "open";

	private boolean exactmatch;
	private boolean open;
	private Date end;

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		super.serializeContent(serializer);
		if (end != null)
			SerializerUtils
					.setDateTimeAttribute(serializer, END_ATTRIBUTE, end);
		if (exactmatch)
			SerializerUtils.setBooleanAttribute(serializer,
					EXACTMATCH_ATTRIBUTE, exactmatch);
		if (open)
			SerializerUtils.setBooleanAttribute(serializer, OPEN_ATTRIBUTE,
					open);
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	public boolean isExactmatch() {
		return exactmatch;
	}

	public void setExactmatch(boolean exactmatch) {
		this.exactmatch = exactmatch;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

}
