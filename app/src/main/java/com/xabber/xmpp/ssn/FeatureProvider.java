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
package com.xabber.xmpp.ssn;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractExtensionProvider;

public class FeatureProvider extends AbstractExtensionProvider<Feature> {

	@Override
	protected Feature createInstance(XmlPullParser parser) {
		return new Feature();
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, Feature instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (DataForm.ELEMENT_NAME.equals(parser.getName())
				&& DataForm.NAMESPACE.equals(parser.getNamespace())) {
			PacketExtension packetExtension = PacketParserUtils
					.parsePacketExtension(DataForm.ELEMENT_NAME,
							DataForm.NAMESPACE, parser);
			if (packetExtension instanceof DataForm)
				instance.setDataForm((DataForm) packetExtension);
			return true;
		}
		return false;
	}

}
