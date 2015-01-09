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

/**
 * Named roster conctact's group.
 * 
 * @author alexander.ivanov
 * 
 */
public class RosterGroupReference implements Group {

	private final RosterGroup rosterGroup;

	/**
	 * System contact list id.
	 * 
	 * MUST BE MANAGED FROM BACKGROUND THREAD ONLY.
	 */
	private Long id;

	public RosterGroupReference(RosterGroup rosterGroup) {
		this.rosterGroup = rosterGroup;
		id = null;
	}

	@Override
	public String getName() {
		return rosterGroup.getName();
	}

	/**
	 * @return the rosterGroup
	 */
	RosterGroup getRosterGroup() {
		return rosterGroup;
	}

	Long getId() {
		return id;
	}

	void setId(Long id) {
		this.id = id;
	}

}
