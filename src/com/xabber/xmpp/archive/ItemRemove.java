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
 * Packet to remove items from the message archive preferences.
 * 
 * http://xmpp.org/extensions/xep-0136.html
 * 
 * @author alexander.ivanov
 * 
 */
public class ItemRemove extends IQ {

	public static final String ELEMENT_NAME = "itemremove";
	public static final String NAMESPACE = "urn:xmpp:archive";

	private final Collection<Item> items;

	public ItemRemove() {
		items = new ArrayList<Item>();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		for (Item item : items)
			item.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		return items.size() > 0;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public void addItem(Item item) {
		items.add(item);
	}

	public Collection<Item> getItems() {
		return Collections.unmodifiableCollection(items);
	}

}
