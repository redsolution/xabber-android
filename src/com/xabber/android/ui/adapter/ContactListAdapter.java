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
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.app.ListActivity;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;

/**
 * Adapter for contact list in the main activity.
 * 
 * @author alexander.ivanov
 * 
 */
public class ContactListAdapter extends
		GroupedContactAdapter<ChatContactInflater, GroupManager> implements
		Runnable {

	/**
	 * Number of milliseconds between lazy refreshes.
	 */
	private static final long REFRESH_INTERVAL = 1000;

	/**
	 * View with information shown on empty contact list.
	 */
	private final View infoView;

	/**
	 * Image view with connected icon.
	 */
	private View connectedView;

	/**
	 * Image view with disconnected icon.
	 */
	private View disconnectedView;

	/**
	 * View with help text.
	 */
	private TextView textView;

	/**
	 * Button to apply help text.
	 */
	private Button buttonView;

	/**
	 * Animation for disconnected view.
	 */
	private Animation animation;

	/**
	 * Handler for deferred refresh.
	 */
	private final Handler handler;

	/**
	 * Lock for refresh requests.
	 */
	private final Object refreshLock;

	/**
	 * Whether refresh was requested.
	 */
	private boolean refreshRequested;

	/**
	 * Whether refresh is in progress.
	 */
	private boolean refreshInProgess;

	/**
	 * Minimal time when next refresh can be executed.
	 */
	private Date nextRefresh;

	public ContactListAdapter(ListActivity activity) {
		super(activity, activity.getListView(), new ChatContactInflater(
				activity), GroupManager.getInstance());
		infoView = activity.findViewById(R.id.info);
		if (infoView != null) {
			connectedView = infoView.findViewById(R.id.connected);
			disconnectedView = infoView.findViewById(R.id.disconnected);
			textView = (TextView) infoView.findViewById(R.id.text);
			buttonView = (Button) infoView.findViewById(R.id.button);
			animation = AnimationUtils.loadAnimation(activity,
					R.anim.connection);
		} else {
			connectedView = null;
			disconnectedView = null;
			textView = null;
			buttonView = null;
			animation = null;
		}
		handler = new Handler();
		refreshLock = new Object();
		refreshRequested = false;
		refreshInProgess = false;
		nextRefresh = new Date();
	}

	/**
	 * Requests refresh in some time in future.
	 */
	public void refreshRequest() {
		synchronized (refreshLock) {
			if (refreshRequested)
				return;
			if (refreshInProgess)
				refreshRequested = true;
			else {
				long delay = nextRefresh.getTime() - new Date().getTime();
				handler.postDelayed(this, delay > 0 ? delay : 0);
			}
		}
	}

	/**
	 * Remove refresh requests.
	 */
	public void removeRefreshRequests() {
		synchronized (refreshLock) {
			refreshRequested = false;
			refreshInProgess = false;
			handler.removeCallbacks(this);
		}
	}

	@Override
	public void onChange() {
		synchronized (refreshLock) {
			refreshRequested = false;
			refreshInProgess = true;
			handler.removeCallbacks(this);
		}

		final Collection<RosterContact> rosterContacts = RosterManager
				.getInstance().getContacts();
		final boolean showOffline = SettingsManager.contactsShowOffline();
		final boolean showGroups = SettingsManager.contactsShowGroups();
		final boolean showEmptyGroups = SettingsManager
				.contactsShowEmptyGroups();
		final boolean showActiveChats = SettingsManager
				.contactsShowActiveChats();
		final boolean stayActiveChats = SettingsManager
				.contactsStayActiveChats();
		final boolean showAccounts = SettingsManager.contactsShowAccounts();
		final Comparator<AbstractContact> comparator = SettingsManager
				.contactsOrder();
		final CommonState commonState = AccountManager.getInstance()
				.getCommonState();
		final String selectedAccount = AccountManager.getInstance()
				.getSelectedAccount();

		/**
		 * Accounts.
		 */
		final TreeMap<String, AccountConfiguration> accounts = new TreeMap<String, AccountConfiguration>();

		/**
		 * Groups.
		 */
		final TreeMap<String, GroupConfiguration> groups;

		/**
		 * Contacts.
		 */
		final ArrayList<AbstractContact> contacts;

		/**
		 * List of active chats.
		 */
		final GroupConfiguration activeChats;

		/**
		 * List of rooms and active chats grouped by users inside accounts.
		 */
		final TreeMap<String, TreeMap<String, AbstractChat>> abstractChats = new TreeMap<String, TreeMap<String, AbstractChat>>();

		/**
		 * Whether there is at least one contact.
		 */
		boolean hasContact = false;

		/**
		 * Whether there is at least one visible contact.
		 */
		boolean hasVisible = false;

		for (String account : AccountManager.getInstance().getAccounts())
			accounts.put(account, null);

		for (AbstractChat abstractChat : MessageManager.getInstance()
				.getChats()) {
			if ((abstractChat instanceof RoomChat || abstractChat.isActive())
					&& accounts.containsKey(abstractChat.getAccount())) {
				final String account = abstractChat.getAccount();
				TreeMap<String, AbstractChat> users = abstractChats
						.get(account);
				if (users == null) {
					users = new TreeMap<String, AbstractChat>();
					abstractChats.put(account, users);
				}
				users.put(abstractChat.getUser(), abstractChat);
			}
		}

		if (filterString == null) {
			// Create arrays.
			if (showAccounts) {
				groups = null;
				contacts = null;
				for (Entry<String, AccountConfiguration> entry : accounts
						.entrySet()) {
					entry.setValue(new AccountConfiguration(entry.getKey(),
							GroupManager.IS_ACCOUNT, groupStateProvider));
				}
			} else {
				if (showGroups) {
					groups = new TreeMap<String, GroupConfiguration>();
					contacts = null;
				} else {
					groups = null;
					contacts = new ArrayList<AbstractContact>();
				}
			}
			if (showActiveChats)
				activeChats = new GroupConfiguration(GroupManager.NO_ACCOUNT,
						GroupManager.ACTIVE_CHATS, groupStateProvider);
			else
				activeChats = null;

			// Build structure.
			for (RosterContact rosterContact : rosterContacts) {
				if (!rosterContact.isEnabled())
					continue;
				hasContact = true;
				final boolean online = rosterContact.getStatusMode().isOnline();
				final String account = rosterContact.getAccount();
				final TreeMap<String, AbstractChat> users = abstractChats
						.get(account);
				final AbstractChat abstractChat;
				if (users == null)
					abstractChat = null;
				else
					abstractChat = users.remove(rosterContact.getUser());
				if (showActiveChats && abstractChat != null
						&& abstractChat.isActive()) {
					activeChats.setNotEmpty();
					hasVisible = true;
					if (activeChats.isExpanded())
						activeChats.addAbstractContact(rosterContact);
					activeChats.increment(online);
					if (!stayActiveChats || (!showAccounts && !showGroups))
						continue;
				}
				if (selectedAccount != null && !selectedAccount.equals(account))
					continue;
				if (addContact(rosterContact, online, accounts, groups,
						contacts, showAccounts, showGroups, showOffline))
					hasVisible = true;
			}
			for (TreeMap<String, AbstractChat> users : abstractChats.values())
				for (AbstractChat abstractChat : users.values()) {
					final AbstractContact abstractContact;
					if (abstractChat instanceof RoomChat)
						abstractContact = new RoomContact(
								(RoomChat) abstractChat);
					else
						abstractContact = new ChatContact(abstractChat);
					if (showActiveChats && abstractChat.isActive()) {
						activeChats.setNotEmpty();
						hasVisible = true;
						if (activeChats.isExpanded())
							activeChats.addAbstractContact(abstractContact);
						activeChats.increment(false);
						if (!stayActiveChats || (!showAccounts && !showGroups))
							continue;
					}
					if (selectedAccount != null
							&& !selectedAccount.equals(abstractChat
									.getAccount()))
						continue;
					final String group;
					final boolean online;
					if (abstractChat instanceof RoomChat) {
						group = GroupManager.IS_ROOM;
						online = abstractContact.getStatusMode().isOnline();
					} else {
						group = GroupManager.NO_GROUP;
						online = false;
					}
					hasVisible = true;
					addContact(abstractContact, group, online, accounts,
							groups, contacts, showAccounts, showGroups);
				}

			// Remove empty groups, sort and apply structure.
			baseEntities.clear();
			if (hasVisible) {
				if (showActiveChats) {
					if (!activeChats.isEmpty()) {
						if (showAccounts || showGroups)
							baseEntities.add(activeChats);
						activeChats
								.sortAbstractContacts(ComparatorByChat.COMPARATOR_BY_CHAT);
						baseEntities.addAll(activeChats.getAbstractContacts());
					}
				}
				if (showAccounts) {
					for (AccountConfiguration rosterAccount : accounts.values()) {
						baseEntities.add(rosterAccount);
						if (showGroups) {
							if (rosterAccount.isExpanded())
								for (GroupConfiguration rosterConfiguration : rosterAccount
										.getSortedGroupConfigurations())
									if (showEmptyGroups
											|| !rosterConfiguration.isEmpty()) {
										baseEntities.add(rosterConfiguration);
										rosterConfiguration
												.sortAbstractContacts(comparator);
										baseEntities.addAll(rosterConfiguration
												.getAbstractContacts());
									}
						} else {
							rosterAccount.sortAbstractContacts(comparator);
							baseEntities.addAll(rosterAccount
									.getAbstractContacts());
						}
					}
				} else {
					if (showGroups) {
						for (GroupConfiguration rosterConfiguration : groups
								.values())
							if (showEmptyGroups
									|| !rosterConfiguration.isEmpty()) {
								baseEntities.add(rosterConfiguration);
								rosterConfiguration
										.sortAbstractContacts(comparator);
								baseEntities.addAll(rosterConfiguration
										.getAbstractContacts());
							}
					} else {
						Collections.sort(contacts, comparator);
						baseEntities.addAll(contacts);
					}
				}
			}
		} else { // Search
			final ArrayList<AbstractContact> baseEntities = new ArrayList<AbstractContact>();

			// Build structure.
			for (RosterContact rosterContact : rosterContacts) {
				if (!rosterContact.isEnabled())
					continue;
				final String account = rosterContact.getAccount();
				final TreeMap<String, AbstractChat> users = abstractChats
						.get(account);
				if (users != null)
					users.remove(rosterContact.getUser());
				if (rosterContact.getName().toLowerCase(locale)
						.contains(filterString))
					baseEntities.add(rosterContact);
			}
			for (TreeMap<String, AbstractChat> users : abstractChats.values())
				for (AbstractChat abstractChat : users.values()) {
					final AbstractContact abstractContact;
					if (abstractChat instanceof RoomChat)
						abstractContact = new RoomContact(
								(RoomChat) abstractChat);
					else
						abstractContact = new ChatContact(abstractChat);
					if (abstractContact.getName().toLowerCase(locale)
							.contains(filterString))
						baseEntities.add(abstractContact);
				}
			Collections.sort(baseEntities, comparator);
			this.baseEntities.clear();
			this.baseEntities.addAll(baseEntities);
			hasVisible = baseEntities.size() > 0;
		}

		if (infoView != null) {
			if (hasVisible) {
				infoView.setVisibility(View.GONE);
				disconnectedView.clearAnimation();
			} else {
				infoView.setVisibility(View.VISIBLE);
				final int text;
				final int button;
				final ContactListState state;
				if (filterString != null) {
					if (commonState == CommonState.online)
						state = ContactListState.online;
					else if (commonState == CommonState.roster
							|| commonState == CommonState.connecting)
						state = ContactListState.connecting;
					else
						state = ContactListState.offline;
					text = R.string.application_state_no_online;
					button = 0;
				} else if (hasContact) {
					state = ContactListState.online;
					text = R.string.application_state_no_online;
					button = R.string.application_action_no_online;
				} else if (commonState == CommonState.online) {
					state = ContactListState.online;
					text = R.string.application_state_no_contacts;
					button = R.string.application_action_no_contacts;
				} else if (commonState == CommonState.roster) {
					state = ContactListState.connecting;
					text = R.string.application_state_roster;
					button = 0;
				} else if (commonState == CommonState.connecting) {
					state = ContactListState.connecting;
					text = R.string.application_state_connecting;
					button = 0;
				} else if (commonState == CommonState.waiting) {
					state = ContactListState.offline;
					text = R.string.application_state_waiting;
					button = R.string.application_action_waiting;
				} else if (commonState == CommonState.offline) {
					state = ContactListState.offline;
					text = R.string.application_state_offline;
					button = R.string.application_action_offline;
				} else if (commonState == CommonState.disabled) {
					state = ContactListState.offline;
					text = R.string.application_state_disabled;
					button = R.string.application_action_disabled;
				} else if (commonState == CommonState.empty) {
					state = ContactListState.offline;
					text = R.string.application_state_empty;
					button = R.string.application_action_empty;
				} else {
					throw new IllegalStateException();
				}
				if (state == ContactListState.offline) {
					connectedView.setVisibility(View.INVISIBLE);
					disconnectedView.setVisibility(View.VISIBLE);
					disconnectedView.clearAnimation();
				} else if (state == ContactListState.connecting) {
					connectedView.setVisibility(View.VISIBLE);
					disconnectedView.setVisibility(View.VISIBLE);
					if (disconnectedView.getAnimation() == null)
						disconnectedView.startAnimation(animation);
				} else if (state == ContactListState.online) {
					connectedView.setVisibility(View.VISIBLE);
					disconnectedView.setVisibility(View.INVISIBLE);
					disconnectedView.clearAnimation();
				}
				textView.setText(text);
				if (button == 0) {
					buttonView.setVisibility(View.GONE);
				} else {
					buttonView.setVisibility(View.VISIBLE);
					buttonView.setText(button);
					buttonView.setTag(Integer.valueOf(button));
				}
			}
		}

		super.onChange();

		synchronized (refreshLock) {
			nextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
			refreshInProgess = false;
			handler.removeCallbacks(this); // Just to be sure.
			if (refreshRequested)
				handler.postDelayed(this, REFRESH_INTERVAL);
		}
	}

	@Override
	public void run() {
		onChange();
	}

}
