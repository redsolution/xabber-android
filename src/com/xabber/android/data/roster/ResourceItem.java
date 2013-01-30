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
package com.xabber.android.data.roster;

import com.xabber.android.data.account.StatusMode;
import com.xabber.xmpp.address.Jid;

/**
 * Represents information about contact's resource.
 * 
 * @author alexander.ivanov
 * 
 */
public class ResourceItem implements Comparable<ResourceItem> {

	/**
	 * Unchanged resource name.
	 */
	private String verbose;

	private StatusMode statusMode;
	private String statusText;
	private int priority;

	public ResourceItem(String verbose, StatusMode statusMode,
			String statusText, int priority) {
		this.verbose = verbose;
		this.statusMode = statusMode;
		this.statusText = statusText;
		this.priority = priority;
	}

	/**
	 * Note: {@link Jid#getStringPrep(String)} before operate on it.
	 * 
	 * @return Unchanged resource name.
	 */
	public String getVerbose() {
		return verbose;
	}

	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}

	/**
	 * Note: {@link Jid#getStringPrep(String)} before operate on it.
	 * 
	 * @param bareAddress
	 * @return Full JID.
	 */
	public String getUser(String bareAddress) {
		return bareAddress + "/" + verbose;
	}

	public StatusMode getStatusMode() {
		return statusMode;
	}

	public void setStatusMode(StatusMode statusMode) {
		this.statusMode = statusMode;
	}

	public String getStatusText() {
		return statusText;
	}

	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int compareTo(ResourceItem another) {
		int result = priority - another.priority;
		if (result != 0)
			return result;
		return statusMode.compareTo(another.statusMode);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + priority;
		result = prime * result
				+ ((statusMode == null) ? 0 : statusMode.hashCode());
		result = prime * result
				+ ((statusText == null) ? 0 : statusText.hashCode());
		result = prime * result + ((verbose == null) ? 0 : verbose.hashCode());
		return result;
	}
}
