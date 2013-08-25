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
package com.xabber.android.data.extension.archive;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.account.OnAccountAddedListener;
import com.xabber.android.data.account.OnAccountArchiveModeChangedListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.connection.OnResponseListener;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.extension.capability.ServerInfoManager;
import com.xabber.android.data.extension.time.OnTimeReceivedListener;
import com.xabber.android.data.extension.time.TimeManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.BaseAccountNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.archive.AbstractMessage;
import com.xabber.xmpp.archive.Auto;
import com.xabber.xmpp.archive.Chat;
import com.xabber.xmpp.archive.CollectionHeader;
import com.xabber.xmpp.archive.Default;
import com.xabber.xmpp.archive.Item;
import com.xabber.xmpp.archive.ItemRemove;
import com.xabber.xmpp.archive.List;
import com.xabber.xmpp.archive.Modified;
import com.xabber.xmpp.archive.OtrMode;
import com.xabber.xmpp.archive.Pref;
import com.xabber.xmpp.archive.Retrieve;
import com.xabber.xmpp.archive.SaveMode;
import com.xabber.xmpp.archive.Session;
import com.xabber.xmpp.archive.SessionRemove;
import com.xabber.xmpp.rsm.Set;

/**
 * Manage server side archive. Replicate it with memory storage.
 * 
 * Terminology used in docstrings:
 * 
 * <b>Chat</b> - jid (domain or bare or full) specified in <code>with</code>
 * attribute of the chat.
 * 
 * <b>Tag</b> - string representation of the <code>start</code> attribute of the
 * chat.
 * 
 * @author alexander.ivanov
 * 
 */
public class MessageArchiveManager implements OnPacketListener,
		OnTimeReceivedListener, OnAccountAddedListener,
		OnAccountRemovedListener, OnLoadListener,
		OnAccountArchiveModeChangedListener, OnDisconnectListener {

	private static final Integer SESSION_TIMEOUT = 7 * 24 * 60 * 60;

	private static final int RSM_MAX = 20;

	private static final String FEATURE_ARCH = "urn:xmpp:archive";
	private static final String FEATURE_PREF = "urn:xmpp:archive:pref";
	private static final String FEATURE_MANAGE = "urn:xmpp:archive:manage";

	/**
	 * Custom auto setting per account.
	 */
	private final Map<String, Boolean> saves;

	/**
	 * Default settings for each account.
	 */
	private final Map<String, ArchivePreference> defaults;

	/**
	 * Per user settings for each account.
	 */
	private final Map<String, Map<MatchMode, Map<String, ArchivePreference>>> items;

	/**
	 * Settings for session in each account.
	 */
	private final NestedMap<SaveMode> sessionSaves;

	/**
	 * Contains whether chat modification has been requested for the given
	 * packet id in the accounts.
	 */
	private final NestedMap<Boolean> modificationRequests;

	/**
	 * Store information about modifications received from the server.
	 */
	private final Map<String, ModificationStorage> modificationStorages;

	/**
	 * Server side timestamp when connection has been established.
	 */
	private final Map<String, Date> connected;

	/**
	 * Store current history request state for the user in each account.
	 */
	private final NestedMap<HistoryStorage> historyStorages;

	/**
	 * Chat storages for tags for users in accounts.
	 */
	private final NestedNestedMaps<String, ChatStorage> chatStorages;

	private final BaseAccountNotificationProvider<AvailableArchiveRequest> availableArchiveRequestProvider;

	private final static MessageArchiveManager instance;

	static {
		instance = new MessageArchiveManager(Application.getInstance());
		Application.getInstance().addManager(instance);
	}

	public static MessageArchiveManager getInstance() {
		return instance;
	}

	private MessageArchiveManager(Application application) {
		saves = new HashMap<String, Boolean>();
		defaults = new HashMap<String, ArchivePreference>();
		items = new HashMap<String, Map<MatchMode, Map<String, ArchivePreference>>>();
		sessionSaves = new NestedMap<SaveMode>();
		modificationStorages = new HashMap<String, ModificationStorage>();
		connected = new HashMap<String, Date>();
		historyStorages = new NestedMap<HistoryStorage>();
		modificationRequests = new NestedMap<Boolean>();
		chatStorages = new NestedNestedMaps<String, ChatStorage>();
		availableArchiveRequestProvider = new BaseAccountNotificationProvider<AvailableArchiveRequest>(
				R.drawable.ic_stat_request);
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
				availableArchiveRequestProvider);
	}

	@Override
	public void onAccountAdded(AccountItem accountItem) {
		Map<MatchMode, Map<String, ArchivePreference>> maps = new HashMap<MatchMode, Map<String, ArchivePreference>>();
		for (MatchMode matchMode : MatchMode.values())
			maps.put(matchMode, new HashMap<String, ArchivePreference>());
		items.put(accountItem.getAccount(), maps);
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		saves.remove(accountItem.getAccount());
		defaults.remove(accountItem.getAccount());
		items.remove(accountItem.getAccount());
		sessionSaves.clear(accountItem.getAccount());
		modificationStorages.remove(accountItem.getAccount());
		connected.remove(accountItem.getAccount());
		historyStorages.clear(accountItem.getAccount());
		chatStorages.clear(accountItem.getAccount());
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (connection instanceof AccountItem)
			modificationRequests.clear(((AccountItem) connection).getAccount());
	}

	@Override
	public void onTimeReceived(final ConnectionItem connection) {
		if (!(connection instanceof AccountItem)) {
			onModifiedAvailable(connection);
			return;
		}
		String account = ((AccountItem) connection).getAccount();
		ModificationStorage modificationStorage = modificationStorages
				.get(account);
		if (modificationStorage == null) {
			modificationStorage = new ModificationStorage();
			modificationStorages.put(account, modificationStorage);
		}
		modificationStorage.onConnected();
		removeNotReceived(account);
		ArchiveMode archiveMode = AccountManager.getInstance().getArchiveMode(
				account);
		if (ServerInfoManager.getInstance().isProtocolSupported(account,
				FEATURE_ARCH)
				|| ServerInfoManager.getInstance().isProtocolSupported(account,
						FEATURE_PREF)
				|| ServerInfoManager.getInstance().isProtocolSupported(account,
						FEATURE_MANAGE)) {
			if (archiveMode == ArchiveMode.available) {
				availableArchiveRequestProvider.add(
						new AvailableArchiveRequest(account), null);
			}
		}
		if (archiveMode != ArchiveMode.server) {
			onModifiedAvailable(account);
			return;
		}
		if (ServerInfoManager.getInstance().isProtocolSupported(account,
				FEATURE_PREF)) {
			requestPreferences(account);
			return;
		}
		onPreferenceAvailable(account);
	}

	/**
	 * Removes chat storages that wasn't fully received.
	 * 
	 * @param account
	 */
	private void removeNotReceived(String account) {
		NestedNestedMaps<String, ChatStorage> remove = new NestedNestedMaps<String, ChatStorage>();
		for (Entry<String, Map<String, ChatStorage>> users : chatStorages
				.getNested(account).entrySet())
			for (Entry<String, ChatStorage> storages : users.getValue()
					.entrySet())
				if (!storages.getValue().isReceived())
					remove.put(account, users.getKey(), storages.getKey(),
							storages.getValue());
		for (Entry<String, Map<String, ChatStorage>> users : remove.getNested(
				account).entrySet())
			for (Entry<String, ChatStorage> storages : users.getValue()
					.entrySet())
				chatStorages.remove(account, users.getKey(), storages.getKey());
	}

	protected void onPreferenceAvailable(String account) {
		if (ServerInfoManager.getInstance().isProtocolSupported(account,
				FEATURE_MANAGE)) {
			ModificationStorage modificationStorage = modificationStorages
					.get(account);
			if (modificationStorage.request(TimeManager.getInstance()
					.getServerTime(account))) {
				requestModified(account, "");
				return;
			}
		}
		onModifiedAvailable(account);
	}

	private void onModifiedAvailable(String account) {
		modificationStorages.get(account).onFinished();
		removeNotReceived(account);
		connected
				.put(account, TimeManager.getInstance().getServerTime(account));
		onModifiedAvailable(AccountManager.getInstance().getAccount(account));
		if (AccountManager.getInstance().getArchiveMode(account) == ArchiveMode.server)
			for (Entry<String, HistoryStorage> entity : historyStorages
					.getNested(account).entrySet())
				if (entity.getValue().onResume())
					requestSequence(account, entity.getKey(), entity.getValue());
	}

	private void onModifiedAvailable(ConnectionItem connection) {
		for (OnArchiveModificationsReceivedListener listener : Application
				.getInstance().getManagers(
						OnArchiveModificationsReceivedListener.class))
			listener.onArchiveModificationsReceived(connection);
	}

	@Override
	public void onPacket(ConnectionItem connection, final String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.server)
			return;
		if (bareAddress != null && !Jid.getServer(account).equals(bareAddress))
			return;
		if (!(packet instanceof IQ))
			return;
		IQ iq = (IQ) packet;
		if (iq.getType() == Type.SET && packet instanceof Pref
				&& ((Pref) packet).isValid())
			onPreferenceReceived(account, (Pref) packet);
		else if (iq.getType() == Type.SET && packet instanceof ItemRemove
				&& ((ItemRemove) packet).isValid())
			onItemRemoveReceived(account, (ItemRemove) packet);
		else if (iq.getType() == Type.SET && packet instanceof SessionRemove
				&& ((SessionRemove) packet).isValid())
			onSessionRemoveReceived(account, (SessionRemove) packet);
		else if (iq.getType() == Type.RESULT && packet instanceof List
				&& ((List) packet).isValid())
			onListReceived(account, (List) packet);
		else if (iq.getType() == Type.RESULT && packet instanceof Chat
				&& ((Chat) packet).isValid())
			onChatReceived(account, (Chat) packet);
	}

	private void onPreferencesResponce(String account, Pref pref) {
		defaults.remove(account);
		for (Map<String, ArchivePreference> map : items.get(account).values())
			map.clear();
		sessionSaves.clear(account);
		checkForDefaults(account, pref.getDefault());
		Boolean autoSave = pref.getAutoSave();
		if (autoSave != null) {
			if (autoSave) {
				// TODO: check whether record can be disabled.
			} else if (AccountManager.getInstance().getArchiveMode(account) == ArchiveMode.server) {
				Auto auto = new Auto();
				auto.setSave(true);
				auto.setType(Type.SET);
				try {
					ConnectionManager.getInstance().sendPacket(account, auto);
				} catch (NetworkException e) {
				}
				// TODO: track results.
				saves.put(account, true);
			}
		}
		onPreferenceReceived(account, pref);
	}

	private void onPreferenceReceived(String account, Pref pref) {
		Default defaultItem = pref.getDefault();
		if (defaultItem != null)
			defaults.put(account, new ArchivePreference(defaultItem.getOtr(),
					defaultItem.getSave()));
		for (Item item : pref.getItems()) {
			MatchMode matchMode;
			String value = Jid.getStringPrep(item.getJid());
			if (item.getExactmatch() != null && item.getExactmatch()) {
				matchMode = MatchMode.exect;
			} else {
				String resource = Jid.getResource(item.getJid());
				if (resource != null && !"".equals(resource))
					matchMode = MatchMode.exect;
				else {
					String name = Jid.getName(item.getJid());
					if (name != null && !"".equals(name)) {
						matchMode = MatchMode.bare;
						value = Jid.getBareAddress(value);
					} else {
						matchMode = MatchMode.domain;
						value = Jid.getServer(value);
					}
				}
				items.get(account)
						.get(matchMode)
						.put(value,
								new ArchivePreference(item.getOtr(), item
										.getSave()));
			}
		}
		for (Session session : pref.getSessions())
			sessionSaves.put(account, session.getThread(), session.getSave());
	}

	private void checkForDefaults(String account, Default received) {
		if (received == null || !received.isUnset()
				|| received.getSave() != SaveMode.fls)
			return;
		Default defaultItem = new Default();
		defaultItem.setExpire(received.getExpire());
		defaultItem.setOtr(received.getOtr());
		defaultItem.setSave(SaveMode.body);
		Pref pref = new Pref();
		pref.setDefault(defaultItem);
		pref.setType(Type.SET);
		try {
			ConnectionManager.getInstance().sendPacket(account, pref);
		} catch (NetworkException e) {
		}
	}

	private void onItemRemoveReceived(String account, ItemRemove itemRemove) {
		for (Item item : itemRemove.getItems())
			for (Map<String, ArchivePreference> map : items.get(account)
					.values())
				map.remove(item.getJid());
	}

	private void onSessionRemoveReceived(String account,
			SessionRemove sessionRemove) {
		for (Session session : sessionRemove.getSessions())
			sessionSaves.remove(account, session.getThread());
	}

	private void onModifiedReceived(String account, Modified modified) {
		ModificationStorage modificationStorage = modificationStorages
				.get(account);
		onHeadersReceived(account, null, modificationStorage,
				modified.getChats(), modified.getRsm());
	}

	private void onListReceived(String account, List list) {
		String bareAddress = null;
		HistoryStorage historyStorage = null;
		for (Entry<String, HistoryStorage> entry : historyStorages.getNested(
				account).entrySet())
			if (entry.getValue().hasPacketId(list.getPacketID())) {
				bareAddress = entry.getKey();
				historyStorage = entry.getValue();
			}
		if (bareAddress == null)
			return;
		historyStorage.setPacketId(null);
		onHeadersReceived(account, bareAddress, historyStorage,
				list.getChats(), list.getRsm());
	}

	private void onHeadersReceived(String account, String bareAddress,
			HeaderSequence sequence,
			Collection<? extends CollectionHeader> headers, Set rsm) {
		sequence.addHeaders(headers);
		if (rsm == null || rsm.isBackwardFinished(headers.size()))
			sequence.onHeadersReceived();
		else
			sequence.setNext(rsm.getFirst());
		requestSequence(account, bareAddress, sequence);
	}

	private void onChatReceived(String account, Chat chat) {
		Boolean modification = modificationRequests.remove(account,
				chat.getPacketID());
		if (modification == null)
			return;
		ChatStorage chatStorage = chatStorages.get(account, chat.getWith(),
				chat.getStartString());
		if (chatStorage == null) {
			LogManager.w(this, "Unexpected chat " + chat.getStartString()
					+ " recevied by " + account + " from " + chat.getWith());
			chatStorage = new ChatStorage(chat.getStart());
			chatStorages.put(account, chat.getWith(), chat.getStartString(),
					chatStorage);
		}
		String bareAddress = Jid.getBareAddress(chat.getWith());
		HeaderSequence sequence;
		if (modification)
			sequence = modificationStorages.get(account);
		else
			sequence = historyStorages.get(account, bareAddress);
		if (sequence == null)
			return;
		AbstractChat abstractChat = MessageManager.getInstance()
				.getOrCreateChat(account, bareAddress);
		for (AbstractMessage abstractMessage : chat.getMessages())
			chatStorage.addItem(abstractChat, chat, abstractMessage,
					TimeManager.getInstance().getServerTimeOffset(account));
		if (chat.getRsm() == null
				|| chat.getRsm().isForwardFinished(chat.getMessages().size())) {
			chatStorage.onItemsReceived(chat.getVersion());
			sequence.pollHeader();
			if (sequence instanceof HistoryStorage)
				if (apply(account, bareAddress, chat.getStartString(),
						chatStorage, (HistoryStorage) sequence))
					return;
			requestSequence(account, bareAddress, sequence);
		} else {
			requestChat(account, chat, chat.getRsm().getLast(), modification);
		}
	}

	/**
	 * Apply received messages.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param tag
	 * @param chatStorage
	 * @param historyStorage
	 * @return Whether enough messages have been received.
	 */
	private boolean apply(String account, String bareAddress, String tag,
			ChatStorage chatStorage, HistoryStorage historyStorage) {
		AbstractChat abstractChat = MessageManager.getInstance().getChat(
				account, bareAddress);
		int newCount = abstractChat.onMessageDownloaded(tag,
				chatStorage.getItems(), false);
		int incomingCount = 0;
		for (MessageItem messageItem : chatStorage.getItems())
			if (messageItem.isIncoming())
				incomingCount += 1;
		chatStorage.onApplied();
		if (historyStorage.enoughMessages(newCount, incomingCount)) {
			historyStorage.onSuccess();
			return true;
		}
		return false;
	}

	/**
	 * Apply received messages.
	 * 
	 * @param account
	 * @param modificationStorage
	 */
	private void apply(String account, ModificationStorage modificationStorage) {
		for (Entry<String, Map<String, ChatStorage>> users : chatStorages
				.getNested(account).entrySet()) {
			String bareAddress = Jid.getBareAddress(users.getKey());
			AbstractChat abstractChat = MessageManager.getInstance().getChat(
					account, bareAddress);
			for (Entry<String, ChatStorage> storages : users.getValue()
					.entrySet()) {
				ChatStorage chatStorage = storages.getValue();
				if (chatStorage.isApplied())
					continue;
				abstractChat.onMessageDownloaded(storages.getKey(),
						chatStorage.getItems(), true);
				chatStorage.onApplied();
			}
		}
		modificationStorage.onSuccess();
	}

	private void requestPreferences(String account) {
		Pref pref = new Pref();
		pref.setType(Type.GET);
		try {
			ConnectionManager.getInstance().sendRequest(account, pref,
					new OnResponseListener() {

						@Override
						public void onReceived(String account, String packetId,
								IQ iq) {
							if (iq instanceof Pref && ((Pref) iq).isValid())
								onPreferencesResponce(account, (Pref) iq);
							onPreferenceAvailable(account);
						}

						@Override
						public void onError(String account, String packetId,
								IQ iq) {
							onPreferenceAvailable(account);
						}

						@Override
						public void onTimeout(String account, String packetId) {
							onError(account, packetId, null);
						}

						@Override
						public void onDisconnect(String account, String packetId) {
						}

					});
		} catch (NetworkException e) {
		}
	}

	private void requestSequence(String account, String bareAddress,
			HeaderSequence sequence) {
		while (true) {
			CollectionHeader header = sequence.peekHeader();
			if (header == null) {
				if (sequence.isHeadersReceived()) {
					if (sequence instanceof ModificationStorage) {
						apply(account, (ModificationStorage) sequence);
						onModifiedAvailable(account);
					}
				} else {
					if (sequence instanceof ModificationStorage) {
						requestModified(account, sequence.getNext());
					} else {
						((HistoryStorage) sequence).setPacketId(requestList(
								account, bareAddress, sequence.getNext()));
					}
				}
			} else {
				ChatStorage chatStorage = chatStorages.get(account,
						header.getWith(), header.getStartString());
				if (chatStorage == null
						|| (chatStorage.isReceived() && !chatStorage
								.hasVersion(header.getVersion()))) {
					chatStorage = new ChatStorage(header.getStart());
					chatStorages.put(account, header.getWith(),
							header.getStartString(), chatStorage);
				} else if (chatStorage.isReceived()) {
					if (sequence instanceof HistoryStorage
							&& !chatStorage.isApplied())
						if (apply(account, header.getWith(),
								header.getStartString(), chatStorage,
								(HistoryStorage) sequence))
							break;
					sequence.pollHeader();
					continue;
				}
				requestChat(account, header, null,
						sequence instanceof ModificationStorage);
			}
			break;
		}
	}

	private void requestModified(String account, String before) {
		Modified packet = new Modified();
		packet.setType(Type.GET);
		Set rsm = new Set();
		rsm.setMax(RSM_MAX);
		rsm.setBefore(before);
		packet.setRsm(rsm);
		packet.setStart(modificationStorages.get(account).getLastRequest());
		try {
			ConnectionManager.getInstance().sendRequest(account, packet,
					new OnResponseListener() {

						@Override
						public void onReceived(String account, String packetId,
								IQ iq) {
							if (iq instanceof Modified
									&& ((Modified) iq).isValid())
								onModifiedReceived(account, (Modified) iq);
							else
								onError(account, packetId, iq);
						}

						@Override
						public void onError(String account, String packetId,
								IQ iq) {
							onModifiedAvailable(account);
						}

						@Override
						public void onTimeout(String account, String packetId) {
							onError(account, packetId, null);
						}

						@Override
						public void onDisconnect(String account, String packetId) {
						}

					});
		} catch (NetworkException e) {
		}
	}

	/**
	 * At east <code>newCount</code> new messages and <code>incomingCount</code>
	 * incoming messages should be loaded from the server side history.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param newCount
	 * @param incomingCount
	 */
	public void requestHistory(String account, String bareAddress,
			int newCount, int incomingCount) {
		if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.server
				|| (newCount <= 0 && incomingCount <= 0))
			return;
		HistoryStorage historyStorage = historyStorages.get(account,
				bareAddress);
		if (historyStorage == null) {
			historyStorage = new HistoryStorage();
			historyStorages.put(account, bareAddress, historyStorage);
		}
		ModificationStorage modificationStorage = modificationStorages
				.get(account);
		if (historyStorage.isInProgress()
				|| (modificationStorage != null && modificationStorage
						.isInProgress())) {
			historyStorage.setRequestedCountAtLeast(newCount, incomingCount);
			return;
		}
		historyStorage.onRequest(newCount, incomingCount);
		requestSequence(account, bareAddress, historyStorage);
	}

	private String requestList(String account, String bareAddress, String before) {
		List packet = new List();
		packet.setType(Type.GET);
		Set rsm = new Set();
		rsm.setMax(RSM_MAX);
		rsm.setBefore(before);
		packet.setRsm(rsm);
		packet.setWith(bareAddress);
		packet.setEnd(connected.get(account));
		String packetId = packet.getPacketID();
		try {
			ConnectionManager.getInstance().sendPacket(account, packet);
		} catch (NetworkException e) {
		}
		return packetId;
	}

	private void requestChat(String account, CollectionHeader header,
			String after, boolean modification) {
		Retrieve packet = new Retrieve();
		packet.setType(Type.GET);
		Set rsm = new Set();
		rsm.setMax(RSM_MAX);
		rsm.setAfter(after);
		packet.setRsm(rsm);
		packet.setWith(header.getWith());
		packet.setStartString(header.getStartString());
		modificationRequests.put(account, packet.getPacketID(), modification);
		try {
			if (!modification) {
				ConnectionManager.getInstance().sendPacket(account, packet);
				return;
			}
			ConnectionManager.getInstance().sendRequest(account, packet,
					new OnResponseListener() {

						@Override
						public void onReceived(String account, String packetId,
								IQ iq) {
							if (iq instanceof Chat && ((Chat) iq).isValid())
								onChatReceived(account, (Chat) iq);
							else
								onError(account, packetId, iq);
						}

						@Override
						public void onError(String account, String packetId,
								IQ iq) {
							onModifiedAvailable(account);
						}

						@Override
						public void onTimeout(String account, String packetId) {
							onError(account, packetId, null);
						}

						@Override
						public void onDisconnect(String account, String packetId) {
						}

					});
		} catch (NetworkException e) {
		}
	}

	private void sendItemUpdate(String account, String user, SaveMode saveMode,
			OtrMode otrMode) throws NetworkException {
		Item extension = new Item();
		extension.setJid(user);
		extension.setOtr(otrMode);
		extension.setSave(saveMode);
		Pref packet = new Pref();
		packet.addItem(extension);
		packet.setType(Type.SET);
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	private void sendItemRemove(String account, String user)
			throws NetworkException {
		Item extension = new Item();
		extension.setJid(user);
		ItemRemove packet = new ItemRemove();
		packet.addItem(extension);
		packet.setType(Type.SET);
		ConnectionManager.getInstance().sendPacket(account, packet);
	}

	public void setOtrMode(String account, String user, OtrMode otrMode)
			throws NetworkException {
		ArchivePreference itemArchivePreference = getItemArchivePreference(
				account, user);
		if (itemArchivePreference != null
				&& itemArchivePreference.getOtrMode() == otrMode)
			return;
		ArchivePreference defaultArchivePreference = defaults.get(account);
		SaveMode userSaveMode = getUserSaveMode(account, user);
		if (userSaveMode == null) {
			if (otrMode == OtrMode.require)
				userSaveMode = SaveMode.fls;
			else
				userSaveMode = SaveMode.body;
		}
		if (itemArchivePreference == null) {
			if (defaultArchivePreference != null
					&& defaultArchivePreference.getOtrMode() == otrMode)
				return;
			else
				sendItemUpdate(account, user, userSaveMode, otrMode);
		} else {
			if (defaultArchivePreference != null
					&& defaultArchivePreference.getOtrMode() == otrMode)
				sendItemRemove(account, user);
			else
				sendItemUpdate(account, user, userSaveMode, otrMode);
		}
	}

	/**
	 * @param account
	 * @param user
	 * @return Selected OTR mode or <code>null</code> if there is no either user
	 *         settings either default settings.
	 */
	public OtrMode getOtrMode(String account, String user) {
		ArchivePreference archivePreference = getItemArchivePreference(account,
				user);
		if (archivePreference == null)
			archivePreference = defaults.get(account);
		if (archivePreference == null)
			return null;
		return archivePreference.getOtrMode();
	}

	/**
	 * @param account
	 * @param user
	 * @param session
	 * @return <code>null</code> if there is no session, user and default
	 *         settings.
	 */
	public SaveMode getSaveMode(String account, String user, String session) {
		SaveMode sessionSaveMode = sessionSaves.get(account, session);
		if (sessionSaveMode != null)
			return sessionSaveMode;
		return getUserSaveMode(account, user);
	}

	private ArchivePreference getItemArchivePreference(String account,
			String user) {
		Map<MatchMode, Map<String, ArchivePreference>> map = items.get(account);
		if (map == null)
			return null;
		ArchivePreference result = map.get(MatchMode.exect).get(user);
		if (result != null)
			return result;
		result = map.get(MatchMode.bare).get(Jid.getBareAddress(user));
		if (result != null)
			return result;
		return map.get(MatchMode.domain).get(Jid.getServer(user));
	}

	private void sendSessionUpdate(String account, String session,
			SaveMode saveMode) throws NetworkException {
		Session extension = new Session();
		extension.setThread(session);
		extension.setTimeout(SESSION_TIMEOUT);
		extension.setSave(saveMode);
		Pref packet = new Pref();
		packet.addSession(extension);
		packet.setType(Type.SET);
		ConnectionManager.getInstance().sendPacket(account, packet);
		sessionSaves.put(account, session, saveMode);
	}

	private void sendSessionRemove(String account, String session)
			throws NetworkException {
		Session extension = new Session();
		extension.setThread(session);
		SessionRemove packet = new SessionRemove();
		packet.addSession(extension);
		packet.setType(Type.SET);
		ConnectionManager.getInstance().sendPacket(account, packet);
		sessionSaves.remove(account, session);
	}

	private SaveMode getUserSaveMode(String account, String user) {
		ArchivePreference archivePreference = getItemArchivePreference(account,
				user);
		if (archivePreference != null)
			return archivePreference.getSaveMode();
		Boolean save = saves.get(account);
		if (save != null)
			return save ? SaveMode.body : SaveMode.fls;
		archivePreference = defaults.get(account);
		if (archivePreference != null)
			return archivePreference.getSaveMode();
		return null;
	}

	public void setSaveMode(String account, String user, String session,
			SaveMode saveMode) throws NetworkException {
		if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.server)
			return;
		SaveMode sessionSaveMode = sessionSaves.get(account, session);
		if (sessionSaveMode == saveMode)
			return;
		SaveMode userSaveMode = getUserSaveMode(account, user);
		if (sessionSaveMode == null) {
			if (userSaveMode == saveMode)
				return;
			else
				sendSessionUpdate(account, session, saveMode);
		} else {
			if (userSaveMode == saveMode)
				sendSessionRemove(account, session);
			else
				sendSessionUpdate(account, session, saveMode);
		}
	}

	public boolean isModificationsSucceed(String account) {
		ModificationStorage modificationStorage = modificationStorages
				.get(account);
		if (modificationStorage == null)
			return false;
		return modificationStorage.isSucceed();
	}

	@Override
	public void onAccountArchiveModeChanged(AccountItem accountItem) {
		availableArchiveRequestProvider.remove(accountItem.getAccount());
	}

}
