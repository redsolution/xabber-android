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
package com.xabber.android.data.extension.archive;

/**
 * Store history for the user while receiving from the message archive.
 * 
 * @author alexander.ivanov
 * 
 */
public class HistoryStorage extends HeaderSequence {

	/**
	 * Packet ID of the list request.
	 */
	private String packetId;

	/**
	 * Number of new received messages.
	 */
	private int receivedNew;

	/**
	 * Number of incoming received messages.
	 */
	private int receivedIncoming;

	/**
	 * Number of new messages to be received.
	 */
	private Integer requestedNew;

	/**
	 * Number of incoming messages to be received.
	 */
	private Integer requestedIncoming;

	public HistoryStorage() {
		super();
		onSuccess();
	}

	public boolean hasPacketId(String packetId) {
		return this.packetId != null && this.packetId.equals(packetId);
	}

	public void setPacketId(String packetId) {
		this.packetId = packetId;
	}

	/**
	 * @param receivedNew
	 * @param receivedIncoming
	 * @return Whether there is enough messages received.
	 */
	public boolean enoughMessages(int receivedNew, int receivedIncoming) {
		this.receivedNew += receivedNew;
		this.receivedIncoming += receivedIncoming;
		return this.receivedNew >= requestedNew
				&& this.receivedIncoming >= requestedIncoming;
	}

	/**
	 * Request has been started.
	 * 
	 * @param requestedNew
	 * @param requestedIncoming
	 */
	public void onRequest(int requestedNew, int requestedIncoming) {
		this.requestedNew = requestedNew;
		this.requestedIncoming = requestedIncoming;
		onResume();
	}

	/**
	 * @return Whether next history request should be sent.
	 */
	public boolean onResume() {
		packetId = null;
		if (requestedNew <= 0 && requestedIncoming <= 0)
			return false;
		super.setInProgress(true);
		return true;
	}

	/**
	 * Set number of requested messages at least as specified.
	 * 
	 * @param requestedNew
	 * @param requestedIncoming
	 */
	public void setRequestedCountAtLeast(int requestedNew, int requestedIncoming) {
		this.requestedNew = Math.max(this.requestedNew, requestedNew);
		this.requestedIncoming = Math.max(this.requestedIncoming,
				requestedIncoming);
	}

	/**
	 * History portion request has been completed.
	 */
	public void onSuccess() {
		super.setInProgress(false);
		packetId = null;
		receivedNew = 0;
		receivedIncoming = 0;
		requestedNew = 0;
		requestedIncoming = 0;
	}

}
