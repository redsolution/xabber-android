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

import com.xabber.android.data.entity.AccountRelated;

/**
 * Named roster group.
 * 
 * @author alexander.ivanov
 * 
 */
public class RosterGroup extends AccountRelated implements Group {

	private final String name;

	/**
	 * System contact list id.
	 * 
	 * MUST BE MANAGED FROM BACKGROUND THREAD ONLY.
	 */
	private Long id;

	public RosterGroup(String account, String name) {
		super(account);
		this.name = name;
		id = null;
	}

	@Override
	public String getName() {
		return name;
	}

	Long getId() {
		return id;
	}

	void setId(Long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RosterGroup other = (RosterGroup) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
