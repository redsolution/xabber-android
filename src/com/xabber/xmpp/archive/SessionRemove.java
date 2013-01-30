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

import com.xabber.xmpp.IQ;

/**
 * Packet to remove sessions from the message archive preferences.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class SessionRemove extends IQ {

	public static final String ELEMENT_NAME = "sessionremove";
	public static final String NAMESPACE = "urn:xmpp:archive";

	private final Collection<Session> sessions;

	public SessionRemove() {
		sessions = new ArrayList<Session>();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		for (Session session : sessions)
			session.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		return sessions.size() > 0;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public void addSession(Session session) {
		sessions.add(session);
	}

	public Collection<Session> getSessions() {
		return Collections.unmodifiableCollection(sessions);
	}

}
