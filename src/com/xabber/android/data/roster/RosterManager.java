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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountDisabledListener;
import com.xabber.android.data.account.OnAccountEnabledListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.archive.OnArchiveModificationsReceivedListener;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

/**
 * Manage contact list (roster).
 * 
 * @author alexander.ivanov
 * 
 */
public class RosterManager implements OnDisconnectListener, OnPacketListener,
		OnAccountEnabledListener, OnAccountDisabledListener,
		OnArchiveModificationsReceivedListener, OnAccountRemovedListener {

	/**
	 * List of roster groups for its names in accounts.
	 */
	private final NestedMap<RosterGroup> rosterGroups;

	/**
	 * Managed contacts for bare addresses in accounts.
	 */
	private final NestedMap<RosterContact> rosterContacts;

	/**
	 * List of accounts for witch roster was requested.
	 */
	private final Set<String> requestedRosters;

	/**
	 * List of accounts for witch roster has been received.
	 */
	private final Set<String> receivedRosters;

	private final static RosterManager instance;

	static {
		instance = new RosterManager();
		Application.getInstance().addManager(instance);
	}

	public static RosterManager getInstance() {
		return instance;
	}

	private RosterManager() {
		rosterGroups = new NestedMap<RosterGroup>();
		rosterContacts = new NestedMap<RosterContact>();
		receivedRosters = new HashSet<String>();
		requestedRosters = new HashSet<String>();
	}

	public Collection<RosterContact> getContacts() {
		return Collections.unmodifiableCollection(rosterContacts.values());
	}

	public Collection<RosterGroup> getRosterGroups() {
		return Collections.unmodifiableCollection(rosterGroups.values());
	}

	/**
	 * @param account
	 * @param user
	 * @return <code>null</code> can be returned.
	 */
	public RosterContact getRosterContact(String account, String user) {
		return rosterContacts.get(account, user);
	}

	/**
	 * Gets {@link RoomContact}, {@link RosterContact}, {@link ChatContact} or
	 * creates new {@link ChatContact}.
	 * 
	 * @param account
	 * @param user
	 * @return
	 */
	public AbstractContact getBestContact(String account, String user) {
		AbstractChat abstractChat = MessageManager.getInstance().getChat(
				account, user);
		if (abstractChat != null && abstractChat instanceof RoomChat)
			return new RoomContact((RoomChat) abstractChat);
		RosterContact rosterContact = getRosterContact(account, user);
		if (rosterContact != null)
			return rosterContact;
		if (abstractChat != null)
			return new ChatContact(abstractChat);
		return new ChatContact(account, user);
	}

	/**
	 * Adds new group to be managed.
	 * 
	 * @param contact
	 */
	void addRosterGroup(RosterGroup group) {
		rosterGroups.put(group.getAccount(), group.getName(), group);
	}

	/**
	 * Adds new contact to be managed.
	 * 
	 * @param contact
	 */
	void addRosterContact(RosterContact contact) {
		rosterContacts.put(contact.getAccount(), contact.getUser(), contact);
	}

	/**
	 * Adds new contact to be managed and populates addedContacts map.
	 * 
	 * @param contact
	 * @param name
	 * @param addedContacts
	 */
	private void addContact(RosterContact contact, String name,
			Map<RosterContact, String> addedContacts) {
		addRosterContact(contact);
		addedContacts.put(contact, name);
	}

	/**
	 * Removes managed contact and populates removedContacts map.
	 * 
	 * @param contact
	 * @param removedContacts
	 */
	private void removeContact(RosterContact contact,
			Collection<RosterContact> removedContacts) {
		rosterContacts.remove(contact.getAccount(), contact.getUser());
		removedContacts.add(contact);
	}

	/**
	 * Sets name for managed contact and populates renamedContacts map.
	 * 
	 * @param contact
	 * @param name
	 * @param renamedContacts
	 */
	private void setName(RosterContact contact, String name,
			Map<RosterContact, String> renamedContacts) {
		contact.setName(name);
		renamedContacts.put(contact, name);
	}

	/**
	 * Adds group to managed contact and populate addedGroups,
	 * addedGroupReference.
	 * 
	 * @param contact
	 * @param groupName
	 * @param addedGroups
	 * @param addedGroupReference
	 */
	private void addGroup(
			RosterContact contact,
			String groupName,
			Collection<RosterGroup> addedGroups,
			Map<RosterContact, Collection<RosterGroupReference>> addedGroupReference) {
		RosterGroup rosterGroup = rosterGroups.get(contact.getAccount(),
				groupName);
		if (rosterGroup == null) {
			rosterGroup = new RosterGroup(contact.getAccount(), groupName);
			addRosterGroup(rosterGroup);
			addedGroups.add(rosterGroup);
		}
		RosterGroupReference groupReference = new RosterGroupReference(
				rosterGroup);
		contact.addGroupReference(groupReference);
		Collection<RosterGroupReference> collection = addedGroupReference
				.get(contact);
		if (collection == null) {
			collection = new ArrayList<RosterGroupReference>();
			addedGroupReference.put(contact, collection);
		}
		collection.add(groupReference);
	}

	/**
	 * Removes group from managed contact and populates removedGroups,
	 * removedGroupReference.
	 * 
	 * @param contact
	 * @param groupReference
	 * @param removedGroups
	 * @param removedGroupReference
	 */
	private void removeGroupReference(
			RosterContact contact,
			RosterGroupReference groupReference,
			Collection<RosterGroup> removedGroups,
			Map<RosterContact, Collection<RosterGroupReference>> removedGroupReference) {
		contact.removeGroupReference(groupReference);
		Collection<RosterGroupReference> collection = removedGroupReference
				.get(contact);
		if (collection == null) {
			collection = new ArrayList<RosterGroupReference>();
			removedGroupReference.put(contact, collection);
		}
		collection.add(groupReference);
		RosterGroup rosterGroup = groupReference.getRosterGroup();
		for (RosterContact check : rosterContacts.values())
			for (RosterGroupReference reference : check.getGroups())
				if (reference.getRosterGroup() == rosterGroup)
					return;
		rosterGroups.remove(rosterGroup.getAccount(), rosterGroup.getName());
		removedGroups.add(rosterGroup);
	}

	/**
	 * @param account
	 * @return List of groups in specified account.
	 */
	public Collection<String> getGroups(String account) {
		return Collections.unmodifiableCollection(rosterGroups
				.getNested(account).keySet());
	}

	/**
	 * @param account
	 * @param user
	 * @return Contact's name.
	 */
	public String getName(String account, String user) {
		RosterContact contact = getRosterContact(account, user);
		if (contact == null)
			return user;
		return contact.getName();
	}

	/**
	 * @param account
	 * @param user
	 * @return Contact's groups.
	 */
	public Collection<String> getGroups(String account, String user) {
		RosterContact contact = getRosterContact(account, user);
		if (contact == null)
			return Collections.emptyList();
		return contact.getGroupNames();
	}

	/**
	 * Requests to create new contact.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param name
	 * @param groups
	 * @throws NetworkException
	 */
	public void createContact(String account, String bareAddress, String name,
			Collection<String> groups) throws NetworkException {
		RosterPacket packet = new RosterPacket();
		packet.setType(IQ.Type.SET);
		RosterPacket.Item item = new RosterPacket.Item(bareAddress, name);
		for (String group : groups)
			if (group.trim().length() > 0)
				item.addGroupName(group);
		packet.addRosterItem(item);
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	/**
	 * Requests contact removing.
	 * 
	 * @param account
	 * @param bareAddress
	 * @throws NetworkException
	 */
	public void removeContact(String account, String bareAddress)
			throws NetworkException {
		RosterPacket packet = new RosterPacket();
		packet.setType(IQ.Type.SET);
		RosterPacket.Item item = new RosterPacket.Item(bareAddress, "");
		item.setItemType(RosterPacket.ItemType.remove);
		packet.addRosterItem(item);
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	/**
	 * Requests to change contact's name and groups.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param name
	 * @param groups
	 * @throws NetworkException
	 */
	public void setNameAndGroup(String account, String bareAddress,
			String name, Collection<String> groups) throws NetworkException {
		RosterContact contact = getRosterContact(account, bareAddress);
		if (contact == null)
			throw new NetworkException(R.string.ENTRY_IS_NOT_FOUND);
		if (contact.getRealName().equals(name)) {
			HashSet<String> check = new HashSet<String>(contact.getGroupNames());
			if (check.size() == groups.size()) {
				check.removeAll(groups);
				if (check.isEmpty())
					return;
			}
		}
		RosterPacket packet = new RosterPacket();
		packet.setType(IQ.Type.SET);
		RosterPacket.Item item = new RosterPacket.Item(bareAddress, name);
		for (String group : groups)
			item.addGroupName(group);
		packet.addRosterItem(item);
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	/**
	 * Requests to remove group from all contacts in account.
	 * 
	 * @param account
	 * @param group
	 * @throws NetworkException
	 */
	public void removeGroup(String account, String group)
			throws NetworkException {
		RosterPacket packet = new RosterPacket();
		packet.setType(IQ.Type.SET);
		for (RosterContact contact : rosterContacts.getNested(account).values()) {
			HashSet<String> groups = new HashSet<String>(
					contact.getGroupNames());
			if (!groups.remove(group))
				continue;
			RosterPacket.Item item = new RosterPacket.Item(contact.getUser(),
					contact.getRealName());
			for (String one : groups)
				item.addGroupName(one);
			packet.addRosterItem(item);
		}
		if (packet.getRosterItemCount() == 0)
			return;
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	/**
	 * Requests to remove group from all contacts in all accounts.
	 * 
	 * @param group
	 * @throws NetworkException
	 */
	public void removeGroup(String group) throws NetworkException {
		NetworkException networkException = null;
		boolean success = false;
		for (String account : AccountManager.getInstance().getAccounts()) {
			try {
				removeGroup(account, group);
			} catch (NetworkException e) {
				if (networkException == null)
					networkException = e;
				continue;
			}
			success = true;
		}
		if (!success && networkException != null)
			throw networkException;
	}

	/**
	 * Requests to rename group.
	 * 
	 * @param account
	 * @param oldGroup
	 *            can be <code>null</code> for "no group".
	 * @param newGroup
	 * @throws NetworkException
	 */
	public void renameGroup(String account, String oldGroup, String newGroup)
			throws NetworkException {
		if (newGroup.equals(oldGroup))
			return;
		RosterPacket packet = new RosterPacket();
		packet.setType(IQ.Type.SET);
		for (RosterContact contact : rosterContacts.getNested(account).values()) {
			HashSet<String> groups = new HashSet<String>(
					contact.getGroupNames());
			if (!groups.remove(oldGroup)
					&& !(oldGroup == null && groups.isEmpty()))
				continue;
			groups.add(newGroup);
			RosterPacket.Item item = new RosterPacket.Item(contact.getUser(),
					contact.getRealName());
			for (String one : groups)
				item.addGroupName(one);
			packet.addRosterItem(item);
		}
		if (packet.getRosterItemCount() == 0)
			return;
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	/**
	 * Requests to rename group from all accounts.
	 * 
	 * @param oldGroup
	 *            can be <code>null</code> for "no group".
	 * @param newGroup
	 * @throws NetworkException
	 */
	public void renameGroup(String oldGroup, String newGroup)
			throws NetworkException {
		NetworkException networkException = null;
		boolean success = false;
		for (String account : AccountManager.getInstance().getAccounts()) {
			try {
				renameGroup(account, oldGroup, newGroup);
			} catch (NetworkException e) {
				if (networkException == null)
					networkException = e;
				continue;
			}
			success = true;
		}
		if (!success && networkException != null)
			throw networkException;
	}

	/**
	 * @param account
	 * @return Whether roster for specified account has been received.
	 */
	public boolean isRosterReceived(String account) {
		return receivedRosters.contains(account);
	}

	/**
	 * Sets whether contacts in accounts are enabled.
	 * 
	 * @param account
	 * @param enabled
	 */
	private void setEnabled(String account, boolean enabled) {
		for (RosterContact contact : rosterContacts.getNested(account).values())
			contact.setEnabled(enabled);
	}

	@Override
	public void onAccountEnabled(AccountItem accountItem) {
		setEnabled(accountItem.getAccount(), true);
	}

	@Override
	public void onArchiveModificationsReceived(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		// Request roster only when server side archive modifications
		// received.
		String account = ((AccountItem) connection).getAccount();
		requestedRosters.add(account);
		try {
			ConnectionManager.getInstance().sendPacket(account,
					new RosterPacket());
		} catch (NetworkException e) {
			LogManager.exception(this, e);
		}
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		for (RosterContact contact : rosterContacts.getNested(account).values())
			contact.setConnected(false);
		requestedRosters.remove(account);
		receivedRosters.remove(account);
	}

	@Override
	public void onAccountDisabled(AccountItem accountItem) {
		setEnabled(accountItem.getAccount(), false);
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		rosterGroups.clear(accountItem.getAccount());
		rosterContacts.clear(accountItem.getAccount());
	}

	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (!(packet instanceof RosterPacket))
			return;
		if (((RosterPacket) packet).getType() != IQ.Type.ERROR) {
			boolean rosterWasReceived = requestedRosters.remove(account);
			ArrayList<RosterContact> remove = new ArrayList<RosterContact>();
			if (rosterWasReceived)
				for (RosterContact contact : rosterContacts.getNested(account)
						.values()) {
					contact.setConnected(true);
					remove.add(contact);
				}
			RosterPacket rosterPacket = (RosterPacket) packet;
			ArrayList<BaseEntity> entities = new ArrayList<BaseEntity>();
			Collection<RosterGroup> addedGroups = new ArrayList<RosterGroup>();
			Map<RosterContact, String> addedContacts = new HashMap<RosterContact, String>();
			Map<RosterContact, String> renamedContacts = new HashMap<RosterContact, String>();
			Map<RosterContact, Collection<RosterGroupReference>> addedGroupReference = new HashMap<RosterContact, Collection<RosterGroupReference>>();
			Map<RosterContact, Collection<RosterGroupReference>> removedGroupReference = new HashMap<RosterContact, Collection<RosterGroupReference>>();
			Collection<RosterContact> removedContacts = new ArrayList<RosterContact>();
			Collection<RosterGroup> removedGroups = new ArrayList<RosterGroup>();

			for (RosterPacket.Item item : rosterPacket.getRosterItems()) {
				String user = Jid.getBareAddress(item.getUser());
				if (user == null)
					continue;
				entities.add(new BaseEntity(account, user));
				RosterContact contact = getRosterContact(account, user);
				if (item.getItemType() == RosterPacket.ItemType.remove) {
					if (contact != null)
						removeContact(contact, removedContacts);
				} else {
					String name = item.getName();
					if (name == null)
						name = "";
					if (contact == null) {
						contact = new RosterContact(account, user, name);
						addContact(contact, name, addedContacts);
					} else {
						remove.remove(contact);
						if (!contact.getRealName().equals(name))
							setName(contact, name, renamedContacts);
					}
					ArrayList<RosterGroupReference> removeGroupReferences = new ArrayList<RosterGroupReference>(
							contact.getGroups());
					for (String groupName : item.getGroupNames()) {
						RosterGroupReference rosterGroup = contact
								.getRosterGroupReference(groupName);
						if (rosterGroup == null)
							addGroup(contact, groupName, addedGroups,
									addedGroupReference);
						else
							removeGroupReferences.remove(rosterGroup);
					}
					for (RosterGroupReference rosterGroup : removeGroupReferences)
						removeGroupReference(contact, rosterGroup,
								removedGroups, removedGroupReference);
					contact.setSubscribed(item.getItemType() == ItemType.both
							|| item.getItemType() == ItemType.to);
				}
			}
			for (RosterContact contact : remove) {
				entities.add(new BaseEntity(account, contact.getUser()));
				removeContact(contact, removedContacts);
			}
			for (OnRosterChangedListener listener : Application.getInstance()
					.getManagers(OnRosterChangedListener.class))
				listener.onRosterUpdate(addedGroups, addedContacts,
						renamedContacts, addedGroupReference,
						removedGroupReference, removedContacts, removedGroups);
			onContactsChanged(entities);
			if (rosterWasReceived) {
				AccountItem accountItem = (AccountItem) connection;
				receivedRosters.add(account);
				for (OnRosterReceivedListener listener : Application
						.getInstance().getManagers(
								OnRosterReceivedListener.class))
					listener.onRosterReceived(accountItem);
				AccountManager.getInstance().onAccountChanged(account);
			}
		}
	}

	/**
	 * Notifies registered {@link OnContactChangedListener}.
	 * 
	 * @param entities
	 */
	public void onContactsChanged(final Collection<BaseEntity> entities) {
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (OnContactChangedListener onContactChangedListener : Application
						.getInstance().getUIListeners(
								OnContactChangedListener.class))
					onContactChangedListener.onContactsChanged(entities);
			}
		});
	}

	/**
	 * Notifies registered {@link OnContactChangedListener}.
	 * 
	 * @param entities
	 */
	public void onContactChanged(String account, String bareAddress) {
		final ArrayList<BaseEntity> entities = new ArrayList<BaseEntity>();
		entities.add(new BaseEntity(account, bareAddress));
		onContactsChanged(entities);
	}

}
