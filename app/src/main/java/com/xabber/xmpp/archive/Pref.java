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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

/**
 * Message archive preferences.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Pref extends IQ {

	public static final String ELEMENT_NAME = "pref";
	public static final String NAMESPACE = "urn:xmpp:archive";

	public static final String AUTO_NAME = "auto";
	public static final String SAVE_ATTRIBUTE = "save";
	public static final String SCOPE_ATTRIBUTE = "scope";

	public static final String METHOD_NAME = "method";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String USE_ATTRIBUTE = "use";

	private Boolean autoSave;
	private ScopeMode autoScope;
	private Default defaultItem;
	private final Collection<Item> items;
	private final Collection<Session> sessions;
	private final Map<TypeMode, UseMode> methods;

	public Pref() {
		items = new ArrayList<Item>();
		sessions = new ArrayList<Session>();
		methods = new HashMap<TypeMode, UseMode>();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (autoSave != null) {
			serializer.startTag(null, AUTO_NAME);
			SerializerUtils.setBooleanAttribute(serializer, SAVE_ATTRIBUTE,
					autoSave);
			if (autoScope != null)
				SerializerUtils.setTextAttribute(serializer, SCOPE_ATTRIBUTE,
						autoScope.toString());
			serializer.endTag(null, AUTO_NAME);
		}
		if (defaultItem != null)
			defaultItem.serialize(serializer);
		for (Item item : items)
			item.serialize(serializer);
		for (Session session : sessions)
			session.serialize(serializer);
		for (Entry<TypeMode, UseMode> entry : methods.entrySet()) {
			serializer.startTag(null, METHOD_NAME);
			SerializerUtils.setTextAttribute(serializer, TYPE_ATTRIBUTE, entry
					.getKey().toString());
			SerializerUtils.setTextAttribute(serializer, USE_ATTRIBUTE, entry
					.getValue().toString());
			serializer.endTag(null, METHOD_NAME);
		}
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

	public void setMethod(TypeMode type, UseMode use) {
		methods.put(type, use);
	}

	public void setAuto(Boolean save, ScopeMode scope) {
		this.autoSave = save;
		this.autoScope = scope;
	}

	public void setDefault(Default value) {
		defaultItem = value;
	}

	public void addItem(Item item) {
		items.add(item);
	}

	public void addSession(Session session) {
		sessions.add(session);
	}

	public Boolean getAutoSave() {
		return autoSave;
	}

	public Default getDefault() {
		return defaultItem;
	}

	public Collection<Item> getItems() {
		return Collections.unmodifiableCollection(items);
	}

	public Collection<Session> getSessions() {
		return Collections.unmodifiableCollection(sessions);
	}

}
