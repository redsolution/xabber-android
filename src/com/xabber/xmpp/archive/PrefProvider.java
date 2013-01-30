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

import java.util.NoSuchElementException;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;

public class PrefProvider extends AbstractIQProvider<Pref> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Pref instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (Pref.AUTO_NAME.equals(name)) {
			Boolean save = ProviderUtils.parseBoolean(parser.getAttributeValue(
					null, Pref.SAVE_ATTRIBUTE));
			if (save != null) {
				ScopeMode scope = null;
				try {
					scope = ScopeMode.fromString(parser.getAttributeValue(null,
							Pref.SCOPE_ATTRIBUTE));
				} catch (NoSuchElementException e) {
				}
				instance.setAuto(save, scope);
			}
			return false; // Only tag attributes has bee read.
		} else if (Default.ELEMENT_NAME.equals(name)) {
			Default value = DefaultProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.setDefault(value);
		} else if (Item.ELEMENT_NAME.equals(name)) {
			Item value = ItemProvider.getInstance().provideInstance(parser);
			if (value.isValid())
				instance.addItem(value);
		} else if (Session.ELEMENT_NAME.equals(name)) {
			Session value = SessionProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.addSession(value);
		} else if (Pref.METHOD_NAME.equals(name)) {
			TypeMode type = null;
			UseMode use = null;
			try {
				type = TypeMode.fromString(parser.getAttributeValue(null,
						Pref.TYPE_ATTRIBUTE));
			} catch (NoSuchElementException e) {
			}
			try {
				use = UseMode.fromString(parser.getAttributeValue(null,
						Pref.USE_ATTRIBUTE));
			} catch (NoSuchElementException e) {
			}
			if (type != null && use != null)
				instance.setMethod(type, use);
			return false; // Only tag attributes has bee read.
		} else
			return false;
		return true;
	}

	@Override
	protected Pref createInstance(XmlPullParser parser) {
		return new Pref();
	}

}
