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
package com.xabber.android.data.extension.vcard;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.xabber.android.data.entity.BaseEntity;

/**
 * Store information about vCard request for specified user.
 * 
 * @author alexander.ivanov
 * 
 */
class VCardRequest extends BaseEntity {

	private final String packetId;

	/**
	 * List of intent avatar's hashes.
	 */
	private final HashSet<String> hashes;

	public VCardRequest(String account, String bareAddress, String packetId) {
		super(account, bareAddress);
		this.packetId = packetId;
		this.hashes = new HashSet<String>();
	}

	public String getPacketId() {
		return packetId;
	}

	public Collection<String> getHashes() {
		return Collections.unmodifiableCollection(hashes);
	}

	public void addHash(String hash) {
		hashes.add(hash);
	}

}