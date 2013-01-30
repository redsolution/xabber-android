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
import java.util.Comparator;

import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.GroupStateProvider;
import com.xabber.android.data.roster.ShowOfflineMode;

/**
 * Group representation in the contact list.
 */
public class GroupConfiguration extends BaseEntity {

	/**
	 * List of contacts in group.
	 */
	private final ArrayList<AbstractContact> abstractContacts;

	/**
	 * Whether group has no contacts to display in expanded mode.
	 */
	private boolean empty;

	/**
	 * Whether group is expanded.
	 */
	private final boolean expanded;

	/**
	 * Total number of contacts in group.
	 */
	private int total;

	/**
	 * Number of online contacts in group.
	 */
	private int online;

	/**
	 * Mode of showing offline contacts.
	 */
	private final ShowOfflineMode showOfflineMode;

	public GroupConfiguration(String account, String group,
			GroupStateProvider groupStateProvider) {
		super(account, group);
		abstractContacts = new ArrayList<AbstractContact>();
		expanded = groupStateProvider.isExpanded(account, group);
		showOfflineMode = groupStateProvider.getShowOfflineMode(account, group);
		empty = true;
		total = 0;
		online = 0;
	}

	/**
	 * Adds new contact.
	 * 
	 * @param abstractContact
	 */
	public void addAbstractContact(AbstractContact abstractContact) {
		abstractContacts.add(abstractContact);
	}

	/**
	 * Gets list of contacts.
	 * 
	 * @return
	 */
	public Collection<AbstractContact> getAbstractContacts() {
		return abstractContacts;
	}

	/**
	 * Sorts list of abstract contacts.
	 * 
	 * @param comparator
	 */
	public void sortAbstractContacts(Comparator<AbstractContact> comparator) {
		Collections.sort(abstractContacts, comparator);
	}

	/**
	 * Increments number of contacts in group.
	 * 
	 * @param online
	 *            whether contact is online.
	 */
	public void increment(boolean online) {
		this.total++;
		if (online)
			this.online++;
	}

	/**
	 * @return Whether there is no one contact to be displayed in expanded mode.
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Set that there is at least one contact to be displayed in expanded mode.
	 */
	public void setNotEmpty() {
		empty = false;
	}

	/**
	 * @return Whether group is expanded.
	 */
	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * @return Total number of contacts in group.
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @return Number of online contacts in group.
	 */
	public int getOnline() {
		return online;
	}

	/**
	 * @return Mode of showing offline contacts.
	 */
	public ShowOfflineMode getShowOfflineMode() {
		return showOfflineMode;
	}

	@Override
	public int compareTo(BaseEntity another) {
		final String anotherUser = another.getUser();
		int result = account.compareTo(another.getAccount());
		if (result != 0) {
			if (user.compareTo(another.getUser()) != 0) {
				if (user == GroupManager.ACTIVE_CHATS)
					return -1;
				if (anotherUser == GroupManager.ACTIVE_CHATS)
					return 1;
			}
			return result;
		}
		result = user.compareTo(anotherUser);
		if (result != 0) {
			if (user == GroupManager.ACTIVE_CHATS)
				return -1;
			if (anotherUser == GroupManager.ACTIVE_CHATS)
				return 1;
			if (user == GroupManager.IS_ACCOUNT)
				return -1;
			if (anotherUser == GroupManager.IS_ACCOUNT)
				return 1;
			if (user == GroupManager.NO_GROUP)
				return -1;
			if (anotherUser == GroupManager.NO_GROUP)
				return 1;
			if (user == GroupManager.IS_ROOM)
				return -1;
			if (anotherUser == GroupManager.IS_ROOM)
				return 1;
			return result;
		}
		return 0;
	}

}