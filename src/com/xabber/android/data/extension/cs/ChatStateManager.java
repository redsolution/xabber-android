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
package com.xabber.android.data.extension.cs;

import java.util.Calendar;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.ChatStateExtension;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.ComposingPausedReceiver;
import com.xabber.xmpp.address.Jid;

/**
 * Provide information about chat state.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatStateManager implements OnDisconnectListener,
		OnPacketListener, OnCloseListener {

	private final static ChatStateManager instance;

	private static final int PAUSE_TIMEOUT = 30 * 1000;

	private static final long REMOVE_STATE_DELAY = 10 * 1000;

	static {
		instance = new ChatStateManager();
		Application.getInstance().addManager(instance);

		Connection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					@Override
					public void connectionCreated(final Connection connection) {
						ServiceDiscoveryManager
								.getInstanceFor(connection)
								.addFeature(
										"http://jabber.org/protocol/chatstates");
					}
				});
	}

	public static ChatStateManager getInstance() {
		return instance;
	}

	/**
	 * Chat states for lower cased resource for bareAddress in account.
	 */
	private final NestedNestedMaps<String, ChatState> chatStates;

	/**
	 * Cleaners for chat states for lower cased resource for bareAddress in
	 * account.
	 */
	private final NestedNestedMaps<String, Runnable> stateCleaners;

	/**
	 * Information about chat state notification support for lower cased
	 * resource for bareAddress in account.
	 */
	private final NestedNestedMaps<String, Boolean> supports;

	/**
	 * Sent chat state notifications for bareAddress in account.
	 */
	private final NestedMap<ChatState> sent;

	/**
	 * Scheduled pause intents for bareAddress in account.
	 */
	private final NestedMap<PendingIntent> pauseIntents;

	/**
	 * Alarm manager.
	 */
	private final AlarmManager alarmManager;

	/**
	 * Handler for clear states on timeout.
	 */
	private final Handler handler;

	private ChatStateManager() {
		chatStates = new NestedNestedMaps<String, ChatState>();
		stateCleaners = new NestedNestedMaps<String, Runnable>();
		supports = new NestedNestedMaps<String, Boolean>();
		sent = new NestedMap<ChatState>();
		pauseIntents = new NestedMap<PendingIntent>();
		alarmManager = (AlarmManager) Application.getInstance()
				.getSystemService(Context.ALARM_SERVICE);
		handler = new Handler();
	}

	/**
	 * Returns best information chat state for specified bare address.
	 * 
	 * @param account
	 * @param bareAddress
	 * @return <code>null</code> if there is no available information.
	 */
	public ChatState getChatState(String account, String bareAddress) {
		Map<String, ChatState> map = chatStates.get(account, bareAddress);
		if (map == null)
			return null;
		ChatState chatState = null;
		for (ChatState check : map.values())
			if (chatState == null || check.compareTo(chatState) < 0)
				chatState = check;
		return chatState;
	}

	/**
	 * Whether sending chat notification for specified chat is supported.
	 * 
	 * @param chat
	 * @param outgoingMessage
	 * @return
	 */
	private boolean isSupported(AbstractChat chat, boolean outgoingMessage) {
		if (chat instanceof RoomChat)
			return false;
		String to = chat.getTo();
		String bareAddress = Jid.getBareAddress(to);
		String resource = Jid.getResource(to);
		Map<String, Boolean> map = supports.get(chat.getAccount(), bareAddress);
		if (map != null) {
			if (!"".equals(resource)) {
				Boolean value = map.get(resource);
				if (value != null)
					return value;
			} else {
				if (outgoingMessage)
					return true;
				for (Boolean value : map.values())
					if (value != null && value)
						return true;
			}
		}
		return outgoingMessage;
	}

	/**
	 * Update outgoing message before sending.
	 * 
	 * @param chat
	 * @param message
	 */
	public void updateOutgoingMessage(AbstractChat chat, Message message) {
		if (!isSupported(chat, true))
			return;
		message.addExtension(new ChatStateExtension(ChatState.active));
		sent.put(chat.getAccount(), chat.getUser(), ChatState.active);
		cancelPauseIntent(chat.getAccount(), chat.getUser());
	}

	/**
	 * Update chat state information and send message if necessary.
	 * 
	 * @param account
	 * @param user
	 * @param chatState
	 */
	private void updateChatState(String account, String user,
			ChatState chatState) {
		if (!SettingsManager.chatsStateNotification()
				|| sent.get(account, user) == chatState)
			return;
		AbstractChat chat = MessageManager.getInstance().getChat(account, user);
		if (chat == null || !isSupported(chat, false))
			return;
		sent.put(chat.getAccount(), chat.getUser(), chatState);
		Message message = new Message();
		message.setType(chat.getType());
		message.setTo(chat.getTo());
		message.addExtension(new ChatStateExtension(chatState));
		try {
			ConnectionManager.getInstance().sendPacket(account, message);
		} catch (NetworkException e) {
			// Just ignore it.
		}
	}

	/**
	 * Cancel pause intent from the schedule.
	 * 
	 * @param account
	 * @param user
	 */
	private void cancelPauseIntent(String account, String user) {
		PendingIntent pendingIntent = pauseIntents.remove(account, user);
		if (pendingIntent != null)
			alarmManager.cancel(pendingIntent);
	}

	/**
	 * Must be call each time user change text message.
	 * 
	 * @param accunt
	 * @param user
	 */
	public void onComposing(String account, String user, CharSequence text) {
		cancelPauseIntent(account, user);
		if (text.length() == 0) {
			updateChatState(account, user, ChatState.active);
			return;
		} else
			updateChatState(account, user, ChatState.composing);
		Intent intent = ComposingPausedReceiver.createIntent(
				Application.getInstance(), account, user);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(
				Application.getInstance(), 0, intent, 0);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.MILLISECOND, PAUSE_TIMEOUT);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
				pendingIntent);
		pauseIntents.put(account, user, pendingIntent);
	}

	public void onPaused(Intent intent, String account, String user) {
		if (account == null || user == null)
			return;
		updateChatState(account, user, ChatState.paused);
		pauseIntents.remove(account, user);
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		chatStates.clear(account);
		for (Map<String, Runnable> map : stateCleaners.getNested(account)
				.values())
			for (Runnable runnable : map.values())
				handler.removeCallbacks(runnable);
		stateCleaners.clear(account);
		supports.clear(account);
		sent.clear(account);
		for (PendingIntent pendingIntent : pauseIntents.getNested(account)
				.values())
			alarmManager.cancel(pendingIntent);
		pauseIntents.clear(account);
	}

	private void removeCallback(String account, String bareAddress,
			String resource) {
		Runnable runnable = stateCleaners
				.remove(account, bareAddress, resource);
		if (runnable != null)
			handler.removeCallbacks(runnable);
	}

	@Override
	public void onPacket(ConnectionItem connection, final String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		final String resource = Jid.getResource(packet.getFrom());
		if (resource == null)
			return;
		final String account = ((AccountItem) connection).getAccount();
		if (packet instanceof Presence) {
			Presence presence = (Presence) packet;
			if (presence.getType() != Type.unavailable)
				return;
			chatStates.remove(account, bareAddress, resource);
			removeCallback(account, bareAddress, resource);
			supports.remove(account, bareAddress, resource);
		} else if (packet instanceof Message) {
			boolean support = false;
			for (PacketExtension extension : packet.getExtensions())
				if (extension instanceof ChatStateExtension) {
					removeCallback(account, bareAddress, resource);
					ChatState chatState = ((ChatStateExtension) extension)
							.getState();
					chatStates.put(account, bareAddress, resource, chatState);
					if (chatState != ChatState.active) {
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								if (this != stateCleaners.get(account,
										bareAddress, resource))
									return;
								chatStates.remove(account, bareAddress,
										resource);
								removeCallback(account, bareAddress, resource);
								RosterManager.getInstance().onContactChanged(
										account, bareAddress);
							}
						};
						handler.postDelayed(runnable, REMOVE_STATE_DELAY);
						stateCleaners.put(account, bareAddress, resource,
								runnable);
					}
					RosterManager.getInstance().onContactChanged(account,
							bareAddress);
					support = true;
					break;
				}
			Message message = (Message) packet;
			if (message.getType() != Message.Type.chat
					&& message.getType() != Message.Type.groupchat)
				return;
			if (support)
				supports.put(account, bareAddress, resource, true);
			else if (supports.get(account, bareAddress, resource) == null)
				// Disable only if there no information about support.
				supports.put(account, bareAddress, resource, false);
		}
	}

	@Override
	public void onClose() {
		for (PendingIntent pendingIntent : pauseIntents.values())
			alarmManager.cancel(pendingIntent);
		pauseIntents.clear();
	}

}
