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
package com.xabber.android.ui.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import com.xabber.android.data.roster.GroupStateProvider;

/**
 * Account representation in the contact list.
 */
public class AccountConfiguration extends GroupConfiguration {

	private final TreeMap<String, GroupConfiguration> groups;

	public AccountConfiguration(String account, String user,
			GroupStateProvider groupStateProvider) {
		super(account, user, groupStateProvider);
		groups = new TreeMap<String, GroupConfiguration>();
	}

	/**
	 * Gets group by name.
	 * 
	 * @param group
	 * @return <code>null</code> will be returns if there is no such group.
	 */
	public GroupConfiguration getGroupConfiguration(String group) {
		return groups.get(group);
	}

	/**
	 * Adds new group.
	 * 
	 * @param groupConfiguration
	 */
	public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
		groups.put(groupConfiguration.getUser(), groupConfiguration);
	}

	/**
	 * Returns sorted list of groups.
	 * 
	 * @return
	 */
	public Collection<GroupConfiguration> getSortedGroupConfigurations() {
		ArrayList<GroupConfiguration> groups = new ArrayList<GroupConfiguration>(
				this.groups.values());
		Collections.sort(groups);
		return Collections.unmodifiableCollection(groups);
	}

}