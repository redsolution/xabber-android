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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;
import com.xabber.xmpp.rsm.Set;

/**
 * List of the chat collection in the message archive.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class List extends IQ {

	static final String ELEMENT_NAME = "list";
	static final String NAMESPACE = "urn:xmpp:archive";

	static final String EXACTMATCH_ATTRIBUTE = "exactmatch";
	static final String START_ATTRIBUTE = "start";
	static final String END_ATTRIBUTE = "end";
	static final String WITH_ATTRIBUTE = "with";

	private boolean exactmatch;
	private Date start;
	private Date end;
	private String with;
	private Set rsm;
	private final Collection<Chat> chats;

	public List() {
		chats = new ArrayList<Chat>();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (start != null)
			SerializerUtils.setDateTimeAttribute(serializer, START_ATTRIBUTE,
					start);
		if (end != null)
			SerializerUtils
					.setDateTimeAttribute(serializer, END_ATTRIBUTE, end);
		if (with != null)
			SerializerUtils.setTextAttribute(serializer, WITH_ATTRIBUTE, with);
		if (exactmatch)
			SerializerUtils.setBooleanAttribute(serializer,
					EXACTMATCH_ATTRIBUTE, exactmatch);
		for (Chat chat : chats)
			chat.serialize(serializer);
		if (rsm != null)
			rsm.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public boolean isExactmatch() {
		return exactmatch;
	}

	public void setExactmatch(boolean exactmatch) {
		this.exactmatch = exactmatch;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public String getWith() {
		return with;
	}

	public void setWith(String with) {
		this.with = with;
	}

	public Set getRsm() {
		return rsm;
	}

	public void setRsm(Set rsm) {
		this.rsm = rsm;
	}

	public Collection<Chat> getChats() {
		return Collections.unmodifiableCollection(chats);
	}

	public void addChat(Chat value) {
		chats.add(value);
	}

}
