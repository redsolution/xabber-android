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

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.Group;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.GroupStateProvider;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.androiddev.R;

/**
 * Provide grouping implementation for the list of contacts.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class GroupedContactAdapter<Inflater extends BaseContactInflater, StateProvider extends GroupStateProvider>
		extends SmoothContactAdapter<Inflater> {

	/**
	 * List of groups used if contact has no groups.
	 */
	static final Collection<Group> NO_GROUP_LIST;

	/**
	 * View type used for contact items.
	 */
	static final int TYPE_CONTACT = 0;

	/**
	 * View type used for groups and accounts expanders.
	 */
	static final int TYPE_GROUP = 1;

	static {
		Collection<Group> groups = new ArrayList<Group>(1);
		groups.add(new Group() {
			@Override
			public String getName() {
				return GroupManager.NO_GROUP;
			}
		});
		NO_GROUP_LIST = Collections.unmodifiableCollection(groups);
	}

	/**
	 * Account's color.
	 */
	private final ColorStateList expanderAccountTextColor;

	/**
	 * Group's color.
	 */
	private final ColorStateList expanderGroupTextColor;

	/**
	 * Group state provider.
	 */
	final StateProvider groupStateProvider;

	/**
	 * Layout inflater
	 */
	private final LayoutInflater layoutInflater;

	public GroupedContactAdapter(Activity activity, ListView listView,
			Inflater inflater, StateProvider groupStateProvider) {
		super(activity, listView, inflater);
		layoutInflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.groupStateProvider = groupStateProvider;
		TypedArray typedArray;
		typedArray = activity.getTheme().obtainStyledAttributes(
				R.styleable.ContactList);
		expanderAccountTextColor = typedArray
				.getColorStateList(R.styleable.ContactList_expanderAccountColor);
		expanderGroupTextColor = typedArray
				.getColorStateList(R.styleable.ContactList_expanderGroupColor);
		typedArray.recycle();
	}

	/**
	 * Returns group state provider.
	 * 
	 * @return
	 */
	public StateProvider getGroupStateProvider() {
		return groupStateProvider;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		Object object = getItem(position);
		if (object instanceof AbstractContact)
			return TYPE_CONTACT;
		else if (object instanceof GroupConfiguration)
			return TYPE_GROUP;
		else
			throw new IllegalStateException();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == TYPE_CONTACT) {
			return super.getView(position, convertView, parent);
		} else if (getItemViewType(position) == TYPE_GROUP) {
			final View view;
			final GroupViewHolder viewHolder;
			if (convertView == null) {
				view = layoutInflater.inflate(R.layout.base_group_item, parent,
						false);
				TypedArray typedArray = activity
						.obtainStyledAttributes(R.styleable.ContactList);
				view.setBackgroundDrawable(typedArray
						.getDrawable(R.styleable.ContactList_expanderBackground));
				((ImageView) view.findViewById(R.id.indicator))
						.setImageDrawable(typedArray
								.getDrawable(R.styleable.ContactList_expanderIndicator));
				typedArray.recycle();

				viewHolder = new GroupViewHolder(view);
				view.setTag(viewHolder);
			} else {
				view = convertView;
				viewHolder = (GroupViewHolder) view.getTag();
			}
			final GroupConfiguration configuration = (GroupConfiguration) getItem(position);
			final int level;
			if (configuration instanceof AccountConfiguration) {
				level = AccountManager.getInstance().getColorLevel(
						configuration.getAccount());
				viewHolder.name.setTextColor(expanderAccountTextColor);
			} else {
				level = AccountManager.getInstance().getColorCount();
				viewHolder.name.setTextColor(expanderGroupTextColor);
			}
			view.getBackground().setLevel(level);
			viewHolder.name.getBackground().setLevel(
					configuration.getShowOfflineMode().ordinal());
			final String name = GroupManager.getInstance().getGroupName(
					configuration.getAccount(), configuration.getUser());
			viewHolder.name.setText(name + " (" + configuration.getOnline()
					+ "/" + configuration.getTotal() + ")");
			viewHolder.indicator.setImageLevel(configuration.isExpanded() ? 1
					: 0);
			return view;
		} else
			throw new IllegalStateException();
	}

	/**
	 * Gets or creates roster group in roster account.
	 * 
	 * @param accountConfiguration
	 * @param name
	 * @return
	 */
	protected GroupConfiguration getGroupConfiguration(
			AccountConfiguration accountConfiguration, String name) {
		GroupConfiguration groupConfiguration = accountConfiguration
				.getGroupConfiguration(name);
		if (groupConfiguration != null)
			return groupConfiguration;
		groupConfiguration = new GroupConfiguration(
				accountConfiguration.getAccount(), name, groupStateProvider);
		accountConfiguration.addGroupConfiguration(groupConfiguration);
		return groupConfiguration;
	}

	/**
	 * Gets or creates roster group in tree map.
	 * 
	 * @param groups
	 * @param name
	 * @return
	 */
	protected GroupConfiguration getGroupConfiguration(
			TreeMap<String, GroupConfiguration> groups, String name) {
		GroupConfiguration groupConfiguration = groups.get(name);
		if (groupConfiguration != null)
			return groupConfiguration;
		groupConfiguration = new GroupConfiguration(GroupManager.NO_ACCOUNT,
				name, groupStateProvider);
		groups.put(name, groupConfiguration);
		return groupConfiguration;
	}

	/**
	 * Adds contact to specified group.
	 * 
	 * @param abstractContact
	 * @param group
	 * @param online
	 * @param accounts
	 * @param groups
	 * @param contacts
	 * @param showAccounts
	 * @param showGroups
	 */
	protected void addContact(AbstractContact abstractContact, String group,
			boolean online, TreeMap<String, AccountConfiguration> accounts,
			TreeMap<String, GroupConfiguration> groups,
			ArrayList<AbstractContact> contacts, boolean showAccounts,
			boolean showGroups) {
		if (showAccounts) {
			final String account = abstractContact.getAccount();
			final AccountConfiguration accountConfiguration;
			accountConfiguration = accounts.get(account);
			if (accountConfiguration == null)
				return;
			if (showGroups) {
				GroupConfiguration groupConfiguration = getGroupConfiguration(
						accountConfiguration, group);
				if (accountConfiguration.isExpanded()) {
					groupConfiguration.setNotEmpty();
					if (groupConfiguration.isExpanded())
						groupConfiguration.addAbstractContact(abstractContact);
				}
				groupConfiguration.increment(online);
			} else {
				if (accountConfiguration.isExpanded())
					accountConfiguration.addAbstractContact(abstractContact);
			}
			accountConfiguration.increment(online);
		} else {
			if (showGroups) {
				GroupConfiguration groupConfiguration = getGroupConfiguration(
						groups, group);
				groupConfiguration.setNotEmpty();
				if (groupConfiguration.isExpanded())
					groupConfiguration.addAbstractContact(abstractContact);
				groupConfiguration.increment(online);
			} else {
				contacts.add(abstractContact);
			}
		}
	}

	/**
	 * Adds contact to there groups.
	 * 
	 * @param abstractContact
	 * @param online
	 * @param accounts
	 * @param groups
	 * @param contacts
	 * @param showAccounts
	 * @param showGroups
	 * @param showOffline
	 * @return whether contact is visible.
	 */
	protected boolean addContact(AbstractContact abstractContact,
			boolean online, TreeMap<String, AccountConfiguration> accounts,
			TreeMap<String, GroupConfiguration> groups,
			ArrayList<AbstractContact> contacts, boolean showAccounts,
			boolean showGroups, boolean showOffline) {
		boolean hasVisible = false;
		if (showAccounts) {
			final AccountConfiguration accountConfiguration;
			accountConfiguration = accounts.get(abstractContact.getAccount());
			if (accountConfiguration == null)
				return hasVisible;
			if (showGroups) {
				Collection<? extends Group> abstractGroups = abstractContact
						.getGroups();
				if (abstractGroups.size() == 0)
					abstractGroups = NO_GROUP_LIST;
				for (Group abstractGroup : abstractGroups) {
					GroupConfiguration groupConfiguration = getGroupConfiguration(
							accountConfiguration, abstractGroup.getName());
					if (online
							|| (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
							|| (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.always && groupConfiguration
									.getShowOfflineMode() == ShowOfflineMode.normal)
							|| (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.normal
									&& groupConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
						// ............. group
						// ......... | A | N | E
						// ....... A | + | + | -
						// account N | + | ? | -
						// ....... E | + | - | -
						hasVisible = true;
						if (accountConfiguration.isExpanded()) {
							groupConfiguration.setNotEmpty();
							if (groupConfiguration.isExpanded())
								groupConfiguration
										.addAbstractContact(abstractContact);
						}
					}
					groupConfiguration.increment(online);
				}
			} else {
				if (online
						|| (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
						|| (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
					hasVisible = true;
					if (accountConfiguration.isExpanded())
						accountConfiguration
								.addAbstractContact(abstractContact);
				}
			}
			accountConfiguration.increment(online);
		} else {
			if (showGroups) {
				Collection<? extends Group> abstractGroups = abstractContact
						.getGroups();
				if (abstractGroups.size() == 0)
					abstractGroups = NO_GROUP_LIST;
				for (Group abstractGroup : abstractGroups) {
					GroupConfiguration groupConfiguration = getGroupConfiguration(
							groups, abstractGroup.getName());
					if (online
							|| (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
							|| (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
						groupConfiguration.setNotEmpty();
						hasVisible = true;
						if (groupConfiguration.isExpanded())
							groupConfiguration
									.addAbstractContact(abstractContact);
					}
					groupConfiguration.increment(online);
				}
			} else {
				if (online || showOffline) {
					hasVisible = true;
					contacts.add(abstractContact);
				}
			}
		}
		return hasVisible;
	}

	/**
	 * Sets whether group in specified account is expanded.
	 * 
	 * @param account
	 * @param group
	 *            Use {@link #IS_ACCOUNT} to set expanded for account.
	 * @param expanded
	 */
	public void setExpanded(String account, String group, boolean expanded) {
		groupStateProvider.setExpanded(account, group, expanded);
		onChange();
	}

	/**
	 * Holder for views in contact list group.
	 */
	private static class GroupViewHolder {
		final ImageView indicator;
		final TextView name;

		public GroupViewHolder(View view) {
			indicator = (ImageView) view.findViewById(R.id.indicator);
			name = (TextView) view.findViewById(R.id.name);
		}
	}

}
