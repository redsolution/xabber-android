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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;
import com.xabber.xmpp.rsm.Set;

public class ModifiedProvider extends AbstractIQProvider<Modified> {

	@Override
	protected Modified createInstance(XmlPullParser parser) {
		return new Modified();
	}

	@Override
	protected Modified preProcess(XmlPullParser parser, Modified instance) {
		instance.setStart(ProviderUtils.parseDateTime(parser.getAttributeValue(
				null, AbstractChat.START_ATTRIBUTE)));
		return super.preProcess(parser, instance);
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, Modified instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (Changed.ELEMENT_NAME.equals(name)) {
			Changed value = ChangedProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.addModified(value);
		} else if (Removed.ELEMENT_NAME.equals(name)) {
			Removed value = RemovedProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.addModified(value);
		} else if (Set.ELEMENT_NAME.equals(name)
				&& Set.NAMESPACE.equals(parser.getNamespace())) {
			PacketExtension packetExtension = PacketParserUtils
					.parsePacketExtension(Set.ELEMENT_NAME, Set.NAMESPACE,
							parser);
			if (packetExtension instanceof Set
					&& ((Set) packetExtension).isValid())
				instance.setRsm((Set) packetExtension);
		} else
			return false;
		return true;
	}

}
