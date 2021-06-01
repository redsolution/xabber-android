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
package com.xabber.android.ui.adapter.contactlist;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.CircleStateProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Account representation in the contact list.
 */
public class AccountConfiguration extends GroupConfiguration {

    private final TreeMap<String, GroupConfiguration> groups;

    public AccountConfiguration(AccountJid account, String group,
                         CircleStateProvider circleStateProvider) {
        super(account, group, circleStateProvider);
        groups = new TreeMap<>();
    }

    /**
     * Gets group by name.
     *
     * @return <code>null</code> will be returns if there is no such group.
     */
    GroupConfiguration getGroupConfiguration(String group) {
        return groups.get(group);
    }

    /**
     * Adds new group.
     */
    void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        groups.put(groupConfiguration.getGroup(), groupConfiguration);
    }

    /**
     * Returns sorted list of groups.
     */
    public Collection<GroupConfiguration> getSortedGroupConfigurations() {
        ArrayList<GroupConfiguration> groups = new ArrayList<>(this.groups.values());
        Collections.sort(groups);
        return Collections.unmodifiableCollection(groups);
    }

    public Collection<GroupConfiguration> getNotSortedGroupConfigurations() {
        ArrayList<GroupConfiguration> groups = new ArrayList<>(this.groups.values());
        return Collections.unmodifiableCollection(groups);
    }
}