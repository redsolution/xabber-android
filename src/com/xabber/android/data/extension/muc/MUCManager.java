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
package com.xabber.android.data.extension.muc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import android.database.Cursor;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.muc.MUC;

/**
 * Manage multi user chats.
 * 
 * Warning: We are going to remove SMACK components.
 * 
 * @author alexander.ivanov
 * 
 */
public class MUCManager implements OnLoadListener, OnPacketListener {

	private final EntityNotificationProvider<RoomInvite> inviteProvider;

	private final EntityNotificationProvider<RoomAuthorizationError> authorizationErrorProvider;

	private final static MUCManager instance;

	static {
		instance = new MUCManager();
		Application.getInstance().addManager(instance);
	}

	public static MUCManager getInstance() {
		return instance;
	}

	private MUCManager() {
		inviteProvider = new EntityNotificationProvider<RoomInvite>(
				R.drawable.ic_stat_subscribe);
		authorizationErrorProvider = new EntityNotificationProvider<RoomAuthorizationError>(
				R.drawable.ic_stat_auth_failed);
	}

	@Override
	public void onLoad() {
		final Collection<RoomChat> roomChats = new ArrayList<RoomChat>();
		final Collection<RoomChat> needJoins = new ArrayList<RoomChat>();
		Cursor cursor = RoomTable.getInstance().list();
		try {
			if (cursor.moveToFirst()) {
				do {
					RoomChat roomChat = new RoomChat(
							RoomTable.getAccount(cursor),
							RoomTable.getRoom(cursor),
							RoomTable.getNickname(cursor),
							RoomTable.getPassword(cursor));
					if (RoomTable.needJoin(cursor))
						needJoins.add(roomChat);
					roomChats.add(roomChat);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded(roomChats, needJoins);
			}
		});
	}

	private void onLoaded(Collection<RoomChat> roomChats,
			Collection<RoomChat> needJoins) {
		for (RoomChat roomChat : roomChats) {
			AbstractChat abstractChat = MessageManager.getInstance().getChat(
					roomChat.getAccount(), roomChat.getUser());
			if (abstractChat != null)
				MessageManager.getInstance().removeChat(abstractChat);
			MessageManager.getInstance().addChat(roomChat);
			if (needJoins.contains(roomChat))
				roomChat.setState(RoomState.waiting);
		}
		NotificationManager.getInstance().registerNotificationProvider(
				inviteProvider);
		NotificationManager.getInstance().registerNotificationProvider(
				authorizationErrorProvider);
	}

	/**
	 * @param account
	 * @param room
	 * @return <code>null</code> if does not exists.
	 */
	private RoomChat getRoomChat(String account, String room) {
		AbstractChat chat = MessageManager.getInstance().getChat(account, room);
		if (chat != null && chat instanceof RoomChat)
			return (RoomChat) chat;
		return null;
	}

	/**
	 * @param account
	 * @param room
	 * @return Whether there is such room.
	 */
	public boolean hasRoom(String account, String room) {
		return getRoomChat(account, room) != null;
	}

	/**
	 * @param account
	 * @param room
	 * @return nickname or empty string if room does not exists.
	 */
	public String getNickname(String account, String room) {
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null)
			return "";
		return roomChat.getNickname();
	}

	/**
	 * @param account
	 * @param room
	 * @return password or empty string if room does not exists.
	 */
	public String getPassword(String account, String room) {
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null)
			return "";
		return roomChat.getPassword();
	}

	/**
	 * @param account
	 * @param room
	 * @return list of occupants or empty list.
	 */
	public Collection<Occupant> getOccupants(String account, String room) {
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null)
			return Collections.emptyList();
		return roomChat.getOccupants();
	}

	/**
	 * @param account
	 * @param room
	 * @return <code>null</code> if there is no such invite.
	 */
	public RoomInvite getInvite(String account, String room) {
		return inviteProvider.get(account, room);
	}

	public void removeInvite(RoomInvite abstractRequest) {
		inviteProvider.remove(abstractRequest);
	}

	public void removeRoom(final String account, final String room) {
		removeInvite(getInvite(account, room));
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null)
			return;
		leaveRoom(account, room);
		MessageManager.getInstance().removeChat(roomChat);
		RosterManager.getInstance().onContactChanged(account, room);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				RoomTable.getInstance().remove(account, room);
			}
		});
	}

	/**
	 * Creates or updates existed room.
	 * 
	 * @param account
	 * @param room
	 * @param nickname
	 * @param password
	 */
	public void createRoom(String account, String room, String nickname,
			String password, boolean join) {
		removeInvite(getInvite(account, room));
		AbstractChat chat = MessageManager.getInstance().getChat(account, room);
		RoomChat roomChat;
		if (chat == null || !(chat instanceof RoomChat)) {
			if (chat != null)
				MessageManager.getInstance().removeChat(chat);
			roomChat = new RoomChat(account, room, nickname, password);
			MessageManager.getInstance().addChat(roomChat);
		} else {
			roomChat = (RoomChat) chat;
			roomChat.setNickname(nickname);
			roomChat.setPassword(password);
		}
		requestToWriteRoom(account, room, nickname, password, join);
		if (join)
			joinRoom(account, room, true);
	}

	private void requestToWriteRoom(final String account, final String room,
			final String nickname, final String password, final boolean join) {
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				RoomTable.getInstance().write(account, room, nickname,
						password, join);
			}
		});
	}

	/**
	 * @param account
	 * @param room
	 * @return Whether room is disabled.
	 */
	public boolean isDisabled(final String account, final String room) {
		RoomChat roomChat = getRoomChat(account, room);
		return roomChat == null || roomChat.getState() == RoomState.unavailable;
	}

	/**
	 * @param account
	 * @param room
	 * @return Whether connected is establish or connection is in progress.
	 */
	public boolean inUse(final String account, final String room) {
		RoomChat roomChat = getRoomChat(account, room);
		return roomChat != null && roomChat.getState().inUse();
	}

	/**
	 * Requests to join to the room.
	 * 
	 * @param account
	 * @param room
	 * @param requested
	 *            Whether user request to join the room.
	 */
	public void joinRoom(final String account, final String room,
			boolean requested) {
		final XMPPConnection xmppConnection;
		final RoomChat roomChat;
		final String nickname;
		final String password;
		final Thread thread;
		roomChat = getRoomChat(account, room);
		if (roomChat == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			return;
		}
		RoomState state = roomChat.getState();
		if (state == RoomState.available || state == RoomState.occupation) {
			Application.getInstance().onError(R.string.ALREADY_JOINED);
			return;
		}
		if (state == RoomState.creating || state == RoomState.joining) {
			Application.getInstance().onError(R.string.ALREADY_IN_PROGRESS);
			return;
		}
		nickname = roomChat.getNickname();
		password = roomChat.getPassword();
		requestToWriteRoom(account, room, nickname, password, true);
		ConnectionThread connectionThread = AccountManager.getInstance()
				.getAccount(account).getConnectionThread();
		if (connectionThread == null) {
			Application.getInstance().onError(R.string.NOT_CONNECTED);
			return;
		}
		xmppConnection = connectionThread.getXMPPConnection();
		if (xmppConnection == null) {
			Application.getInstance().onError(R.string.NOT_CONNECTED);
			return;
		}
		final MultiUserChat multiUserChat;
		try {
			multiUserChat = new MultiUserChat(xmppConnection, room);
		} catch (IllegalStateException e) {
			Application.getInstance().onError(R.string.NOT_CONNECTED);
			return;
		}
		roomChat.setState(RoomState.joining);
		roomChat.setMultiUserChat(multiUserChat);
		roomChat.setRequested(requested);
		thread = new Thread("Join to room " + room + " from " + account) {
			@Override
			public void run() {
				try {
					if (roomChat.getMultiUserChat() != multiUserChat)
						return;
					multiUserChat.join(nickname, password);
					Application.getInstance().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (roomChat.getMultiUserChat() != multiUserChat)
								return;
							if (roomChat.getState() == RoomState.joining)
								roomChat.setState(RoomState.occupation);
							removeAuthorizationError(account, room);
							RosterManager.getInstance().onContactChanged(
									account, room);
						}
					});
					return;
				} catch (final XMPPException e) {
					Application.getInstance().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (roomChat.getMultiUserChat() != multiUserChat)
								return;
							roomChat.setState(RoomState.error);
							addAuthorizationError(account, room);
							if (e.getXMPPError() != null
									&& e.getXMPPError().getCode() == 409)
								Application.getInstance().onError(
										R.string.NICK_ALREADY_USED);
							else if (e.getXMPPError() != null
									&& e.getXMPPError().getCode() == 401)
								Application.getInstance().onError(
										R.string.AUTHENTICATION_FAILED);
							else
								Application.getInstance().onError(
										R.string.NOT_CONNECTED);
							RosterManager.getInstance().onContactChanged(
									account, room);
						}
					});
					return;
				} catch (IllegalStateException e) {
				} catch (RuntimeException e) {
					LogManager.exception(this, e);
				} catch (Exception e) {
					LogManager.exception(this, e);
				}
				Application.getInstance().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (roomChat.getMultiUserChat() != multiUserChat)
							return;
						roomChat.setState(RoomState.waiting);
						Application.getInstance().onError(
								R.string.NOT_CONNECTED);
						RosterManager.getInstance().onContactChanged(account,
								room);
					}
				});
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	public void leaveRoom(String account, String room) {
		final MultiUserChat multiUserChat;
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null)
			return;
		multiUserChat = roomChat.getMultiUserChat();
		roomChat.setState(RoomState.unavailable);
		roomChat.setRequested(false);
		roomChat.newAction(roomChat.getNickname(), null, ChatAction.leave);
		requestToWriteRoom(account, room, roomChat.getNickname(),
				roomChat.getPassword(), false);
		if (multiUserChat != null) {
			Thread thread = new Thread("Leave to room " + room + " from "
					+ account) {
				@Override
				public void run() {
					try {
						multiUserChat.leave();
					} catch (IllegalStateException e) {
						// Do nothing
					}
				}
			};
			thread.setDaemon(true);
			thread.start();
		}
		RosterManager.getInstance().onContactChanged(account, room);
	}

	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (bareAddress == null || !(packet instanceof Message))
			return;
		Message message = (Message) packet;
		if (message.getType() != Message.Type.normal
				&& message.getType() != Message.Type.chat)
			return;
		MUCUser mucUser = MUC.getMUCUserExtension(packet);
		if (mucUser == null || mucUser.getInvite() == null)
			return;
		RoomChat roomChat = getRoomChat(account, bareAddress);
		if (roomChat == null || !roomChat.getState().inUse()) {
			String inviter = mucUser.getInvite().getFrom();
			if (inviter == null)
				inviter = bareAddress;
			inviteProvider.add(new RoomInvite(account, bareAddress, inviter,
					mucUser.getInvite().getReason(), mucUser.getPassword()),
					true);
		}
	}

	/**
	 * Sends invitation.
	 * 
	 * @param account
	 * @param room
	 * @param user
	 * @throws NetworkException
	 */
	public void invite(String account, String room, String user)
			throws NetworkException {
		RoomChat roomChat = getRoomChat(account, room);
		if (roomChat == null || roomChat.getState() != RoomState.available) {
			Application.getInstance().onError(R.string.NOT_CONNECTED);
			return;
		}
		Message message = new Message(room);
		MUCUser mucUser = new MUCUser();
		MUCUser.Invite invite = new MUCUser.Invite();
		invite.setTo(user);
		invite.setReason("");
		mucUser.setInvite(invite);
		message.addExtension(mucUser);
		ConnectionManager.getInstance().sendPacket(account, message);
		roomChat.putInvite(message.getPacketID(), user);
		roomChat.newAction(roomChat.getNickname(), user, ChatAction.invite_sent);
	}

	public void removeAuthorizationError(String account, String room) {
		authorizationErrorProvider.remove(account, room);
	}

	public void addAuthorizationError(String account, String room) {
		authorizationErrorProvider.add(
				new RoomAuthorizationError(account, room), null);
	}

}
