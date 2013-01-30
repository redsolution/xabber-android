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
package com.xabber.android.data.connection;

import java.util.Date;

/**
 * Holder for listener to be notified about packet delivery.
 * 
 * @author alexander.ivanov
 * 
 */
public class RequestHolder {

	private final long timeout;

	private final OnResponseListener listener;

	public RequestHolder(OnResponseListener listener) {
		super();
		this.timeout = new Date().getTime()
				+ ConnectionManager.PACKET_REPLY_TIMEOUT;
		this.listener = listener;
	}

	public boolean isExpired(long now) {
		return now > timeout;
	}

	public OnResponseListener getListener() {
		return listener;
	}

}
