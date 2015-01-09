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
package com.xabber.xmpp.avatar;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;
import com.xabber.xmpp.SerializerUtils;

/**
 * vCard update packet.
 * 
 * http://xmpp.org/extensions/xep-0153.html
 * 
 * @author alexander.ivanov
 * 
 */
public class VCardUpdate extends PacketExtension {

	public static final String NAMESPACE = "vcard-temp:x:update";
	public static final String ELEMENT_NAME = "x";

	static final String PHOTO_NAME = "photo";

	private String photoHash;

	/**
	 * Create an empty vCard update packet.
	 * 
	 * Information about photo is not ready to be advertised.
	 */
	public VCardUpdate() {
		photoHash = null;
	}

	/**
	 * @return Whether information about photo is ready to be advertised.
	 */
	public boolean isPhotoReady() {
		return photoHash != null;
	}

	/**
	 * @return Whether photo is advertised to be empty.
	 */
	public boolean isEmpty() {
		return "".equals(photoHash);
	}

	/**
	 * @return Photo's hash.
	 */
	public String getPhotoHash() {
		return photoHash;
	}

	/**
	 * @param hash
	 *            photo's hash value. <code>null</code> if information is not
	 *            ready to be advertised. Empty string no photo is to be
	 *            advertised.
	 */
	public void setPhotoHash(String hash) {
		this.photoHash = hash;
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

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (photoHash != null)
			SerializerUtils.addTextTag(serializer, PHOTO_NAME, photoHash);
	}

}
