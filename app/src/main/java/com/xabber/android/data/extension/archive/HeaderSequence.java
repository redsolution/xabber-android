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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import com.xabber.xmpp.archive.CollectionHeader;

/**
 * Store received sequence of the chat collection headers.
 * 
 * @author alexander.ivanov
 * 
 */
public class HeaderSequence {

	/**
	 * Whether sequence or inner chat request is in progress.
	 */
	private boolean inProgress;

	/**
	 * Whether all chats has been received.
	 */
	private boolean received;

	/**
	 * List of chats to be populated.
	 */
	private final Queue<CollectionHeader> headers;

	/**
	 * Number of received chat headers.
	 */
	private int headerCount;

	/**
	 * First received chat used for backward pagination.
	 */
	private String next;

	public HeaderSequence() {
		headers = new LinkedList<CollectionHeader>();
		reset();
		setInProgress(false);
	}

	void reset() {
		received = false;
		headers.clear();
		headerCount = 0;
		next = "";
	}

	public boolean isInProgress() {
		return inProgress;
	}

	void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public boolean isHeadersReceived() {
		return received;
	}

	public void onHeadersReceived() {
		received = true;
	}

	public void addHeaders(Collection<? extends CollectionHeader> headers) {
		ArrayList<CollectionHeader> backward = new ArrayList<CollectionHeader>();
		for (CollectionHeader header : headers)
			backward.add(0, header);
		this.headers.addAll(backward);
		headerCount += headers.size();
	}

	public int getHeaderCount() {
		return headerCount;
	}

	/**
	 * @return Returns and removes first chat header from the query.
	 *         <code>null</code> if there is no more items.
	 */
	public CollectionHeader pollHeader() {
		return headers.poll();
	}

	/**
	 * @return Returns, but doesn't remove first chat header from the query.
	 *         <code>null</code> if there is no more items.
	 */
	public CollectionHeader peekHeader() {
		return headers.peek();
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}
}
