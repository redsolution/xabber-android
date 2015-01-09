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
package com.xabber.android.data.account;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;

import com.xabber.androiddev.R;

/**
 * Status mode.
 * 
 * @author alexander.ivanov
 * 
 */
public enum StatusMode {

	/**
	 * Free to chat.
	 */
	chat,

	/**
	 * Available.
	 */
	available,

	/**
	 * Away.
	 */
	away,

	/**
	 * Away for an extended period of time.
	 */
	xa,

	/**
	 * Do not disturb.
	 */
	dnd,

	/**
	 * Account is invisible.
	 */
	invisible,

	/**
	 * The user is unavailable.
	 */
	unavailable,

	/**
	 * Account is in connection state.
	 * 
	 * Don't use it for {@link AccountManager#setStatus(String, StatusMode)}.
	 */
	connection,

	/**
	 * Account is unable to get presence information. Account is unsubscribed.
	 * 
	 * Don't use it for {@link AccountManager#setStatus(String, StatusMode)}.
	 */
	unsubscribed;

	/**
	 * Creates new {@link StatusMode} form {@link Presence}.
	 * 
	 * @param mode
	 * @return
	 */
	static public StatusMode createStatusMode(Presence presence) {
		if (presence.getType() == Presence.Type.unavailable)
			return StatusMode.unavailable;
		final Mode mode = presence.getMode();
		if (mode == Mode.away)
			return StatusMode.away;
		else if (mode == Mode.chat)
			return StatusMode.chat;
		else if (mode == Mode.dnd)
			return StatusMode.dnd;
		else if (mode == Mode.xa)
			return StatusMode.xa;
		else
			return StatusMode.available;
	}

	/**
	 * Get {@link Mode} for {@link StatusMode}.
	 * 
	 * @return
	 */
	public Mode getMode() {
		if (this == StatusMode.away)
			return Mode.away;
		else if (this == StatusMode.chat)
			return Mode.chat;
		else if (this == StatusMode.dnd)
			return Mode.dnd;
		else if (this == StatusMode.xa)
			return Mode.xa;
		else if (this == StatusMode.available)
			return Mode.available;
		throw new IllegalStateException();
	}

	/**
	 * @return ID of the string resource.
	 */
	public int getStringID() {
		if (this == StatusMode.available)
			return R.string.available;
		else if (this == StatusMode.dnd)
			return R.string.dnd;
		else if (this == StatusMode.xa)
			return R.string.xa;
		else if (this == StatusMode.chat)
			return R.string.chat;
		else if (this == StatusMode.away)
			return R.string.away;
		else if (this == StatusMode.unsubscribed)
			return R.string.unsubscribed;
		else if (this == StatusMode.invisible)
			return R.string.invisible;
		return R.string.unavailable;
	}

	/**
	 * @return Drawable level for status icon.
	 */
	public int getStatusLevel() {
		return ordinal();
	}

	/**
	 * @return Whether entity is online.
	 */
	public boolean isOnline() {
		return this != StatusMode.unavailable && this != unsubscribed;
	}

}
