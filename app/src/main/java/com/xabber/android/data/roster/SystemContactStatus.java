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

import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.StatusUpdates;

import com.xabber.android.data.account.StatusMode;

/**
 * Represents information about status of contact in system contact list.
 * 
 * @author alexander.ivanov
 * 
 */
class SystemContactStatus {

	public static final SystemContactStatus UNAVAILABLE = new SystemContactStatus(
			getPresence(StatusMode.unavailable), "");

	/**
	 * Contact's presence level.
	 * 
	 * @see {@link StatusUpdates#PRESENCE}
	 */
	private final Integer presence;

	/**
	 * Contact's status text.
	 */
	private final String text;

	public SystemContactStatus(Integer presence, String text) {
		super();
		this.presence = presence;
		this.text = text;
	}

	private static Integer getPresence(StatusMode statusMode) {
		if (statusMode == StatusMode.available)
			return Im.AVAILABLE;
		else if (statusMode == StatusMode.away)
			return Im.AWAY;
		else if (statusMode == StatusMode.chat)
			return Im.AVAILABLE;
		else if (statusMode == StatusMode.connection)
			return Im.OFFLINE;
		else if (statusMode == StatusMode.dnd)
			return Im.DO_NOT_DISTURB;
		else if (statusMode == StatusMode.invisible)
			return Im.INVISIBLE;
		else if (statusMode == StatusMode.unavailable)
			return null;
		else if (statusMode == StatusMode.unsubscribed)
			return Im.OFFLINE;
		else if (statusMode == StatusMode.xa)
			return Im.IDLE;
		else
			return null;
	}

	/**
	 * Create new status object from current contact`s status mode and text.
	 * 
	 * @param rosterContact
	 * @return
	 */
	public static SystemContactStatus createStatus(RosterContact rosterContact) {
		return new SystemContactStatus(
				SystemContactStatus.getPresence(rosterContact.getStatusMode()),
				rosterContact.getStatusText());
	}

	public boolean isEmpty() {
		return presence == null && "".equals(text);
	}

	public Integer getPresence() {
		return presence;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return presence + ":" + text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((presence == null) ? 0 : presence.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemContactStatus other = (SystemContactStatus) obj;
		if (presence == null) {
			if (other.presence != null)
				return false;
		} else if (!presence.equals(other.presence))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

}