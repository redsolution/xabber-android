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
package com.xabber.xmpp;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * IQ packet.
 * 
 * Note: we are going to remove underlying smack package.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class IQ extends org.jivesoftware.smack.packet.IQ implements
		Container {

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		SerializerUtils.serialize(serializer, this);
	}

	@Override
	public String getChildElementXML() {
		return SerializerUtils.toXml(this);
	}

}
