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
package com.xabber.android.data.extension.muc;

import com.xabber.android.data.account.StatusMode;
import com.xabber.xmpp.muc.Affiliation;
import com.xabber.xmpp.muc.Role;

/**
 * Room occupant.
 * 
 * @author alexander.ivanov
 * 
 */
public class Occupant implements Comparable<Occupant> {

	private final String nickname;

	private String jid;

	private Role role;

	private Affiliation affiliation;

	private StatusMode statusMode;

	private String statusText;

	public Occupant(String nickname) {
		this.nickname = nickname;
	}

	public String getNickname() {
		return nickname;
	}

	/**
	 * @return can be <code>null</code>.
	 */
	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Affiliation getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(Affiliation affiliation) {
		this.affiliation = affiliation;
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

	@Override
	public int compareTo(Occupant another) {
		int result = another.role.ordinal() - role.ordinal();
		if (result != 0)
			return result;
		return nickname.compareTo(another.nickname);
	}

}
