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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.util.StringUtils;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountDisabledListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.archive.OnArchiveModificationsReceivedListener;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

/**
 * Process contact's presence information.
 * 
 * @author alexander.ivanov
 * 
 */
public class PresenceManager implements OnArchiveModificationsReceivedListener,
		OnPacketListener, OnLoadListener, OnAccountDisabledListener,
		OnDisconnectListener {

	private final EntityNotificationProvider<SubscriptionRequest> subscriptionRequestProvider;

	/**
	 * List of account with requested subscriptions for auto accept incoming
	 * subscription request.
	 */
	private final HashMap<String, HashSet<String>> requestedSubscriptions;

	/**
	 * Presence container for bare address in account.
	 */
	private final NestedMap<ResourceContainer> presenceContainers;

	/**
	 * Account ready to send / update its presence information.
	 */
	private final ArrayList<String> readyAccounts;

	private final static PresenceManager instance;

	static {
		instance = new PresenceManager();
		Application.getInstance().addManager(instance);
	}

	public static PresenceManager getInstance() {
		return instance;
	}

	private PresenceManager() {
		subscriptionRequestProvider = new EntityNotificationProvider<SubscriptionRequest>(
				R.drawable.ic_stat_subscribe);
		requestedSubscriptions = new HashMap<String, HashSet<String>>();
		presenceContainers = new NestedMap<ResourceContainer>();
		readyAccounts = new ArrayList<String>();
	}

	@Override
	public void onLoad() {
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded();
			}
		});
	}

	private void onLoaded() {
		NotificationManager.getInstance().registerNotificationProvider(
				subscriptionRequestProvider);
	}

	/**
	 * @param account
	 * @param user
	 * @return <code>null</code> can be returned.
	 */
	public SubscriptionRequest getSubscriptionRequest(String account,
			String user) {
		return subscriptionRequestProvider.get(account, user);
	}

	/**
	 * Requests subscription to the contact.
	 * 
	 * @param account
	 * @param bareAddress
	 * @throws NetworkException
	 */
	public void requestSubscription(String account, String bareAddress)
			throws NetworkException {
		Presence packet = new Presence(Presence.Type.subscribe);
		packet.setTo(bareAddress);
		ConnectionManager.getInstance().sendPacket(account, packet);
		HashSet<String> set = requestedSubscriptions.get(account);
		if (set == null) {
			set = new HashSet<String>();
			requestedSubscriptions.put(account, set);
		}
		set.add(bareAddress);
	}

	private void removeRequestedSubscription(String account, String bareAddress) {
		HashSet<String> set = requestedSubscriptions.get(account);
		if (set != null)
			set.remove(bareAddress);
	}

	/**
	 * Accepts subscription request from the entity (share own presence).
	 * 
	 * @param account
	 * @param bareAddress
	 * @throws NetworkException
	 */
	public void acceptSubscription(String account, String bareAddress)
			throws NetworkException {
		Presence packet = new Presence(Presence.Type.subscribed);
		packet.setTo(bareAddress);
		ConnectionManager.getInstance().sendPacket(account, packet);
		subscriptionRequestProvider.remove(account, bareAddress);
		removeRequestedSubscription(account, bareAddress);
	}

	/**
	 * Discards subscription request from the entity (deny own presence
	 * sharing).
	 * 
	 * @param account
	 * @param bareAddress
	 * @throws NetworkException
	 */
	public void discardSubscription(String account, String bareAddress)
			throws NetworkException {
		Presence packet = new Presence(Presence.Type.unsubscribed);
		packet.setTo(bareAddress);
		ConnectionManager.getInstance().sendPacket(account, packet);
		subscriptionRequestProvider.remove(account, bareAddress);
		removeRequestedSubscription(account, bareAddress);
	}

	public boolean hasSubscriptionRequest(String account, String bareAddress) {
		return getSubscriptionRequest(account, bareAddress) != null;
	}

	/**
	 * @param account
	 * @param bareAddress
	 * @return Best resource item for specified user. <code>null</code> if there
	 *         is no such user or user has no available resource.
	 */
	public ResourceItem getResourceItem(String account, String bareAddress) {
		ResourceContainer resourceContainer = presenceContainers.get(account,
				bareAddress);
		if (resourceContainer == null)
			return null;
		return resourceContainer.getBest();
	}

	/**
	 * @return Collection with available resources.
	 */
	public Collection<ResourceItem> getResourceItems(String account,
			String bareAddress) {
		ResourceContainer container = presenceContainers.get(account,
				bareAddress);
		if (container == null)
			return Collections.emptyList();
		return container.getResourceItems();
	}

	public StatusMode getStatusMode(String account, String bareAddress) {
		ResourceItem resourceItem = getResourceItem(account, bareAddress);
		if (resourceItem == null)
			return StatusMode.unavailable;
		return resourceItem.getStatusMode();
	}

	public String getStatusText(String account, String bareAddress) {
		ResourceItem resourceItem = getResourceItem(account, bareAddress);
		if (resourceItem == null)
			return "";
		return resourceItem.getStatusText();
	}

	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (packet instanceof Presence) {
			if (bareAddress == null)
				return;
			Presence presence = (Presence) packet;
			if (presence.getType() == Presence.Type.subscribe) {
				// Subscription request
				HashSet<String> set = requestedSubscriptions.get(account);
				if (set != null && set.contains(bareAddress)) {
					try {
						acceptSubscription(account, bareAddress);
					} catch (NetworkException e) {
					}
					subscriptionRequestProvider.remove(account, bareAddress);
				} else {
					subscriptionRequestProvider.add(new SubscriptionRequest(
							account, bareAddress), null);
				}
				return;
			}
			String verbose = StringUtils.parseResource(presence.getFrom());
			String resource = Jid.getResource(presence.getFrom());
			ResourceContainer resourceContainer = presenceContainers.get(
					account, bareAddress);
			ResourceItem resourceItem;
			if (resourceContainer == null)
				resourceItem = null;
			else
				resourceItem = resourceContainer.get(resource);
			StatusMode previousStatusMode = getStatusMode(account, bareAddress);
			String previousStatusText = getStatusText(account, bareAddress);
			if (presence.getType() == Type.available) {
				StatusMode statusMode = StatusMode.createStatusMode(presence);
				String statusText = presence.getStatus();
				int priority = presence.getPriority();
				if (statusText == null)
					statusText = "";
				if (priority == Integer.MIN_VALUE)
					priority = 0;
				if (resourceItem == null) {
					if (resourceContainer == null) {
						resourceContainer = new ResourceContainer();
						presenceContainers.put(account, bareAddress,
								resourceContainer);
					}
					resourceContainer.put(resource, new ResourceItem(verbose,
							statusMode, statusText, priority));
					resourceContainer.updateBest();
				} else {
					resourceItem.setVerbose(verbose);
					resourceItem.setStatusMode(statusMode);
					resourceItem.setStatusText(statusText);
					resourceItem.setPriority(priority);
					resourceContainer.updateBest();
				}
			} else if (presence.getType() == Presence.Type.error
					|| presence.getType() == Type.unavailable) {
				if (presence.getType() == Presence.Type.error
						&& "".equals(resource) && resourceContainer != null)
					presenceContainers.remove(account, bareAddress);
				else if (resourceItem != null) {
					resourceContainer.remove(resource);
					resourceContainer.updateBest();
				}
			}

			// Notify about changes
			StatusMode newStatusMode = getStatusMode(account, bareAddress);
			String newStatusText = getStatusText(account, bareAddress);
			if (previousStatusMode != newStatusMode
					|| !previousStatusText.equals(newStatusText))
				for (OnStatusChangeListener listener : Application
						.getInstance()
						.getManagers(OnStatusChangeListener.class))
					if (previousStatusMode == newStatusMode)
						listener.onStatusChanged(account, bareAddress,
								resource, newStatusText);
					else
						listener.onStatusChanged(account, bareAddress,
								resource, newStatusMode, newStatusText);

			RosterContact rosterContact = RosterManager.getInstance()
					.getRosterContact(account, bareAddress);
			if (rosterContact != null) {
				ArrayList<RosterContact> rosterContacts = new ArrayList<RosterContact>();
				rosterContacts.add(rosterContact);
				for (OnRosterChangedListener listener : Application
						.getInstance().getManagers(
								OnRosterChangedListener.class))
					listener.onPresenceChanged(rosterContacts);
			}

			RosterManager.getInstance().onContactChanged(account, bareAddress);
		} else if (packet instanceof RosterPacket
				&& ((RosterPacket) packet).getType() != IQ.Type.ERROR) {
			RosterPacket rosterPacket = (RosterPacket) packet;
			for (RosterPacket.Item item : rosterPacket.getRosterItems()) {
				if (item.getItemType() == ItemType.both
						|| item.getItemType() == ItemType.from) {
					String user = Jid.getBareAddress(item.getUser());
					if (user == null)
						continue;
					// Contact can be subscribed or unsubscribed from
					// another IM.
					subscriptionRequestProvider.remove(account, user);
				}
			}
		}
	}

	@Override
	public void onArchiveModificationsReceived(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		// Send presence information only when server side archive modifications
		// received.
		String account = ((AccountItem) connection).getAccount();
		readyAccounts.add(account);
		Collection<String> previous = new HashSet<String>();
		for (NestedMap.Entry<ResourceContainer> entry : presenceContainers)
			previous.add(entry.getSecond());
		presenceContainers.clear(account);
		ArrayList<RosterContact> rosterContacts = new ArrayList<RosterContact>();
		for (String bareAddress : previous) {
			RosterContact rosterContact = RosterManager.getInstance()
					.getRosterContact(account, bareAddress);
			if (rosterContact != null)
				rosterContacts.add(rosterContact);
		}
		for (OnRosterChangedListener listener : Application.getInstance()
				.getManagers(OnRosterChangedListener.class))
			listener.onPresenceChanged(rosterContacts);
		try {
			resendPresence(account);
		} catch (NetworkException e) {
		}
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		readyAccounts.remove(account);
	}

	@Override
	public void onAccountDisabled(AccountItem accountItem) {
		requestedSubscriptions.remove(accountItem.getAccount());
		presenceContainers.clear(accountItem.getAccount());
	}

	/**
	 * Sends new presence information.
	 * 
	 * @param account
	 * @throws NetworkException
	 */
	public void resendPresence(String account) throws NetworkException {
		if (!readyAccounts.contains(account))
			throw new NetworkException(R.string.NOT_CONNECTED);
		ConnectionManager.getInstance().sendPacket(account,
				AccountManager.getInstance().getAccount(account).getPresence());
	}

}
