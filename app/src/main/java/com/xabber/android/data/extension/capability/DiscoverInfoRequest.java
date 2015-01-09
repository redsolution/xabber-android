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
package com.xabber.android.data.extension.capability;

import com.xabber.android.data.entity.BaseEntity;

/**
 * Information about discovery info request.
 * 
 * @author alexander.ivanov
 * 
 */
class DiscoverInfoRequest extends BaseEntity {

	private final String packetId;

	private final Capability capability;

	public DiscoverInfoRequest(String account, String user, String packetId,
			Capability capability) {
		super(account, user);
		this.packetId = packetId;
		this.capability = capability;
	}

	public String getPacketId() {
		return packetId;
	}

	public Capability getCapability() {
		return capability;
	}

}
