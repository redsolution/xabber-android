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
package com.xabber.android.data.message;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.MUCUser;

import android.database.Cursor;
import android.os.Environment;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsShowStatusChange;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.account.OnAccountArchiveModeChangedListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.roster.OnStatusChangeListener;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.delay.Delay;

/**
 * Manage chats and its messages.
 * 
 * Warning: message processing using chat instances should be changed.
 * 
 * @author alexander.ivanov
 * 
 */
public class MessageManager implements OnLoadListener, OnPacketListener,
		OnDisconnectListener, OnAccountRemovedListener,
		OnRosterReceivedListener, OnAccountArchiveModeChangedListener,
		OnStatusChangeListener {

	/**
	 * Registered chats for bareAddresses in accounts.
	 */
	private final NestedMap<AbstractChat> chats;

	/**
	 * Visible chat.
	 * 
	 * Will be <code>null</code> if there is no one.
	 */
	private AbstractChat visibleChat;

	private final static MessageManager instance;

	static {
		instance = new MessageManager();
		Application.getInstance().addManager(instance);
	}

	public static MessageManager getInstance() {
		return instance;
	}

	private MessageManager() {
		chats = new NestedMap<AbstractChat>();
	}

	@Override
	public void onLoad() {
		final Set<BaseEntity> loadChats = new HashSet<BaseEntity>();
		Cursor cursor;
		cursor = MessageTable.getInstance().messagesToSend();
		try {
			if (cursor.moveToFirst()) {
				do {
					loadChats.add(new BaseEntity(MessageTable
							.getAccount(cursor), MessageTable.getUser(cursor)));
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded(loadChats);
			}
		});
	}

	private void onLoaded(Set<BaseEntity> loadChats) {
		for (BaseEntity baseEntity : loadChats)
			if (getChat(baseEntity.getAccount(),
					Jid.getBareAddress(baseEntity.getUser())) == null)
				createChat(baseEntity.getAccount(), baseEntity.getUser());
	}

	/**
	 * @param account
	 * @param user
	 * @return <code>null</code> if there is no such chat.
	 */
	public AbstractChat getChat(String account, String user) {
		return chats.get(account, user);
	}

	public Collection<AbstractChat> getChats() {
		return Collections.unmodifiableCollection(chats.values());
	}

	/**
	 * Creates and adds new regular chat to be managed.
	 * 
	 * @param account
	 * @param user
	 * @return
	 */
	private RegularChat createChat(String account, String user) {
		RegularChat chat = new RegularChat(account, Jid.getBareAddress(user));
		addChat(chat);
		return chat;
	}

	/**
	 * Adds chat to be managed.
	 * 
	 * @param chat
	 */
	public void addChat(AbstractChat chat) {
		if (getChat(chat.getAccount(), chat.getUser()) != null)
			throw new IllegalStateException();
		chats.put(chat.getAccount(), chat.getUser(), chat);
	}

	/**
	 * Removes chat from managed.
	 * 
	 * @param chat
	 */
	public void removeChat(AbstractChat chat) {
		chats.remove(chat.getAccount(), chat.getUser());
	}

	/**
	 * Sends message. Creates and registers new chat if necessary.
	 * 
	 * @param account
	 * @param user
	 * @param text
	 */
	public void sendMessage(String account, String user, String text) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			chat = createChat(account, user);
		MessageItem messageItem = chat.newMessage(text);
		chat.sendQueue(messageItem);
	}

	/**
	 * @param account
	 * @param user
	 * @return Where there is active chat.
	 */
	public boolean hasActiveChat(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return false;
		return chat.isActive();
	}

	/**
	 * @return Collection with active chats.
	 */
	public Collection<AbstractChat> getActiveChats() {
		Collection<AbstractChat> collection = new ArrayList<AbstractChat>();
		for (AbstractChat chat : chats.values())
			if (chat.isActive())
				collection.add(chat);
		return Collections.unmodifiableCollection(collection);
	}

	/**
	 * Returns existed chat or create new one.
	 * 
	 * @param account
	 * @param user
	 * @return
	 */
	public AbstractChat getOrCreateChat(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			chat = createChat(account, user);
		return chat;
	}

	/**
	 * Force open chat (make it active).
	 * 
	 * @param account
	 * @param user
	 */
	public void openChat(String account, String user) {
		getOrCreateChat(account, user).openChat();
	}

	/**
	 * Closes specified chat (make it inactive).
	 * 
	 * @param account
	 * @param user
	 */
	public void closeChat(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return;
		chat.closeChat();
	}

	public void requestToLoadLocalHistory(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			chat = createChat(account, user);
		chat.requestToLoadLocalHistory();
	}

	/**
	 * @param account
	 * @param user
	 * @return Last incoming message's text. Empty string if last message is
	 *         outgoing.
	 */
	public String getLastText(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return "";
		return chat.getLastText();
	}

	/**
	 * @param account
	 * @param user
	 * @return Time of last message in chat. Can be <code>null</code>.
	 */
	public Date getLastTime(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return null;
		return chat.getLastTime();
	}

	/**
	 * Sets currently visible chat.
	 * 
	 * @param account
	 * @param user
	 */
	public void setVisibleChat(String account, String user) {
		final boolean remove = !AccountManager.getInstance()
				.getArchiveMode(account).saveLocally();
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			chat = createChat(account, user);
		else {
			// Mark messages as read and them delete from db if necessary.
			final ArrayList<MessageItem> messageItems = new ArrayList<MessageItem>();
			for (MessageItem messageItem : chat.getMessages()) {
				if (!messageItem.isRead()) {
					messageItem.markAsRead();
					messageItems.add(messageItem);
				}
			}
			Application.getInstance().runInBackground(new Runnable() {
				@Override
				public void run() {
					Collection<Long> ids = getMessageIds(messageItems, remove);
					if (remove)
						MessageTable.getInstance().removeMessages(ids);
					else
						MessageTable.getInstance().markAsRead(ids);
				}
			});
		}
		visibleChat = chat;
	}

	/**
	 * All chats become invisible.
	 */
	public void removeVisibleChat() {
		visibleChat = null;
	}

	/**
	 * @param chat
	 * @return Whether specified chat is currently visible.
	 */
	boolean isVisibleChat(AbstractChat chat) {
		return visibleChat == chat;
	}

	/**
	 * Removes all messages from chat.
	 * 
	 * @param account
	 * @param user
	 */
	public void clearHistory(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return;
		chat.removeAllMessages();
		onChatChanged(chat.getAccount(), chat.getUser(), false);
	}

	/**
	 * Removes message from history.
	 * 
	 * @param messageItem
	 */
	public void removeMessage(MessageItem messageItem) {
		AbstractChat chat = messageItem.getChat();
		chat.removeMessage(messageItem);
		onChatChanged(chat.getAccount(), chat.getUser(), false);
	}

	/**
	 * @param account
	 * @param user
	 * @return List of messages or empty list.
	 */
	public Collection<MessageItem> getMessages(String account, String user) {
		AbstractChat chat = getChat(account, user);
		if (chat == null)
			return Collections.emptyList();
		return chat.getMessages();
	}

	/**
	 * Called on action settings change.
	 */
	public void onSettingsChanged() {
		ChatsShowStatusChange showStatusChange = SettingsManager
				.chatsShowStatusChange();
		Collection<BaseEntity> changedEntities = new ArrayList<BaseEntity>();
		for (AbstractChat chat : chats.values())
			if ((chat instanceof RegularChat && showStatusChange != ChatsShowStatusChange.always)
					|| (chat instanceof RoomChat && showStatusChange == ChatsShowStatusChange.never)) {
				// Remove actions with status change.
				ArrayList<MessageItem> remove = new ArrayList<MessageItem>();
				for (MessageItem messageItem : chat.getMessages())
					if (messageItem.getAction() != null
							&& messageItem.getAction().isStatusChage())
						remove.add(messageItem);
				if (remove.isEmpty())
					continue;
				for (MessageItem messageItem : remove)
					chat.removeMessage(messageItem);
				changedEntities.add(chat);
			}
		RosterManager.getInstance().onContactsChanged(changedEntities);
	}

	@Override
	public void onAccountArchiveModeChanged(AccountItem accountItem) {
		final ArchiveMode archiveMode = AccountManager.getInstance()
				.getArchiveMode(accountItem.getAccount());
		if (archiveMode.saveLocally())
			return;
		final String account = accountItem.getAccount();
		final ArrayList<MessageItem> removeMessageItems = new ArrayList<MessageItem>();
		for (AbstractChat chat : chats.getNested(account).values())
			for (MessageItem messageItem : chat.getMessages())
				if (archiveMode == ArchiveMode.dontStore
						|| ((messageItem.isRead() || archiveMode != ArchiveMode.unreadOnly) && messageItem
								.isSent()))
					removeMessageItems.add(messageItem);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				// If message was read or received after removeMessageItems
				// was created then it's ID will be not null. DB actions with
				// such message will have no effect as if it was removed.
				// History ids becomes invalid and will be cleared on next
				// history load.
				MessageTable.getInstance().removeMessages(
						getMessageIds(removeMessageItems, true));
				if (archiveMode == ArchiveMode.dontStore)
					MessageTable.getInstance().removeAccount(account);
				else if (archiveMode == ArchiveMode.unreadOnly)
					MessageTable.getInstance().removeReadAndSent(account);
				else
					MessageTable.getInstance().removeSent(account);
			}
		});
		AccountManager.getInstance().onAccountChanged(accountItem.getAccount());
	}

	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (bareAddress == null)
			return;
		if (packet instanceof Message
				&& MessageArchiveManager.getInstance().isModificationsSucceed(
						account)
				&& Delay.isOfflineMessage(Jid.getServer(account), packet))
			// Ignore offline message if modification from server side message
			// archive have been received.
			return;
		final String user = packet.getFrom();
		boolean processed = false;
		for (AbstractChat chat : chats.getNested(account).values())
			if (chat.onPacket(bareAddress, packet)) {
				processed = true;
				break;
			}
		if (getChat(account, user) != null)
			return;
		if (!processed && packet instanceof Message) {
			final Message message = (Message) packet;
			final String body = message.getBody();
			if (body == null)
				return;
			for (PacketExtension packetExtension : message.getExtensions())
				if (packetExtension instanceof MUCUser)
					return;
			createChat(account, user).onPacket(bareAddress, packet);
		}
	}

	@Override
	public void onRosterReceived(AccountItem accountItem) {
		String account = accountItem.getAccount();
		for (AbstractChat chat : chats.getNested(account).values())
			chat.onComplete();
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		for (AbstractChat chat : chats.getNested(account).values())
			chat.onDisconnect();
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		chats.clear(accountItem.getAccount());
	}

	/**
	 * Export chat to file with specified name.
	 * 
	 * @param account
	 * @param user
	 * @param fileName
	 * @throws NetworkException
	 */
	public File exportChat(String account, String user, String fileName)
			throws NetworkException {
		final File file = new File(Environment.getExternalStorageDirectory(),
				fileName);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			final String titleName = RosterManager.getInstance().getName(
					account, user)
					+ " (" + user + ")";
			out.write("<html><head><title>");
			out.write(StringUtils.escapeHtml(titleName));
			out.write("</title></head><body>");
			final AbstractChat abstractChat = getChat(account, user);
			if (abstractChat != null) {
				final boolean isMUC = abstractChat instanceof RoomChat;
				final String accountName = AccountManager.getInstance()
						.getNickName(account);
				final String userName = RosterManager.getInstance().getName(
						account, user);
				for (MessageItem messageItem : abstractChat.getMessages()) {
					if (messageItem.getAction() != null)
						continue;
					final String name;
					if (isMUC) {
						name = messageItem.getResource();
					} else {
						if (messageItem.isIncoming())
							name = userName;
						else
							name = accountName;
					}
					out.write("<b>");
					out.write(StringUtils.escapeHtml(name));
					out.write("</b>&nbsp;(");
					out.write(StringUtils.getDateTimeText(messageItem
							.getTimestamp()));
					out.write(")<br />\n<p>");
					out.write(StringUtils.escapeHtml(messageItem.getText()));
					out.write("</p><hr />\n");
				}
			}
			out.write("</body></html>");
			out.close();
		} catch (IOException e) {
			throw new NetworkException(R.string.FILE_NOT_FOUND);
		}
		return file;
	}

	/**
	 * Notifies registered {@link OnChatChangedListener}.
	 * 
	 * @param account
	 * @param user
	 * @param incoming
	 */
	public void onChatChanged(final String account, final String user,
			final boolean incoming) {
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (OnChatChangedListener onChatChangedListener : Application
						.getInstance().getUIListeners(
								OnChatChangedListener.class))
					onChatChangedListener
							.onChatChanged(account, user, incoming);
			}
		});
	}

	/**
	 * @param messageItems
	 * @param clearId
	 *            Whether message id must be set to the <code>null</code>.
	 * @return Collection with ids for specified messages.
	 */
	static Collection<Long> getMessageIds(Collection<MessageItem> messageItems,
			boolean clearId) {
		ArrayList<Long> ids = new ArrayList<Long>();
		for (MessageItem messageItem : messageItems) {
			Long id = messageItem.getId();
			if (id == null)
				continue;
			ids.add(id);
			if (clearId)
				messageItem.setId(null);
		}
		return ids;
	}

	private boolean isStatusTrackingEnabled(String account, String bareAddress) {
		if (SettingsManager.chatsShowStatusChange() != ChatsShowStatusChange.always)
			return false;
		AbstractChat abstractChat = getChat(account, bareAddress);
		return abstractChat != null && abstractChat instanceof RegularChat
				&& abstractChat.isStatusTrackingEnabled();
	}

	@Override
	public void onStatusChanged(String account, String bareAddress,
			String resource, String statusText) {
		if (isStatusTrackingEnabled(account, bareAddress))
			getChat(account, bareAddress).newAction(resource, statusText,
					ChatAction.status);
	}

	@Override
	public void onStatusChanged(String account, String bareAddress,
			String resource, StatusMode statusMode, String statusText) {
		if (isStatusTrackingEnabled(account, bareAddress))
			getChat(account, bareAddress).newAction(resource, statusText,
					ChatAction.getChatAction(statusMode));
	}

}
