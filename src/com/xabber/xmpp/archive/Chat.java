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

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.SerializerUtils;
import com.xabber.xmpp.rsm.Set;

/**
 * Represents message archive collection.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Chat extends AbstractChat implements CollectionHeader {

	static final String ELEMENT_NAME = "chat";

	static final String SUBJECT_ATTRIBUTE = "subject";
	static final String THREAD_ATTRIBUTE = "thread";
	static final String VERSION_ATTRIBUTE = "version";

	private String subject;
	private String thread;
	private Integer version;
	private Next next;
	private Previous previous;
	private final Collection<AbstractMessage> messages;
	private Set rsm;

	// TODO: notes

	public Chat() {
		messages = new ArrayList<AbstractMessage>();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		super.serializeContent(serializer);
		if (subject != null)
			SerializerUtils.setTextAttribute(serializer, SUBJECT_ATTRIBUTE,
					subject);
		if (thread != null)
			SerializerUtils.setTextAttribute(serializer, THREAD_ATTRIBUTE,
					thread);
		if (version != null)
			SerializerUtils.setIntegerAttribute(serializer, VERSION_ATTRIBUTE,
					version);
		if (next != null)
			next.serialize(serializer);
		if (previous != null)
			previous.serialize(serializer);
		for (AbstractMessage abstractMessage : messages)
			abstractMessage.serialize(serializer);
		if (rsm != null)
			rsm.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && getStart() != null && getWith() != null;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getThread() {
		return thread;
	}

	public void setThread(String thread) {
		this.thread = thread;
	}

	@Override
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Next getNext() {
		return next;
	}

	public void setNext(Next next) {
		this.next = next;
	}

	public Previous getPrevious() {
		return previous;
	}

	public void setPrevious(Previous previous) {
		this.previous = previous;
	}

	public void addMessage(AbstractMessage value) {
		messages.add(value);
	}

	public Collection<AbstractMessage> getMessages() {
		return Collections.unmodifiableCollection(messages);
	}

	public Set getRsm() {
		// TODO: Use getExtensions().
		return rsm;
	}

	public void setRsm(Set rsm) {
		this.rsm = rsm;
	}

}
