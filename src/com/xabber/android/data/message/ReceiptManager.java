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

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.xmpp.receipt.Received;
import com.xabber.xmpp.receipt.Request;

/**
 * Manage message receive receipts as well as error replies.
 * 
 * @author alexander.ivanov
 * 
 */
public class ReceiptManager implements OnPacketListener, OnDisconnectListener {

	/**
	 * Sent messages for packet ids in accounts.
	 */
	private final NestedMap<MessageItem> sent;

	private final static ReceiptManager instance;

	static {
		instance = new ReceiptManager();
		Application.getInstance().addManager(instance);

		Connection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					@Override
					public void connectionCreated(final Connection connection) {
						ServiceDiscoveryManager.getInstanceFor(connection)
								.addFeature("urn:xmpp:receipts");
					}
				});
	}

	public static ReceiptManager getInstance() {
		return instance;
	}

	private ReceiptManager() {
		sent = new NestedMap<MessageItem>();
	}

	/**
	 * Update outgoing message before sending.
	 * 
	 * @param abstractChat
	 * @param message
	 * @param messageItem
	 */
	public void updateOutgoingMessage(AbstractChat abstractChat,
			Message message, MessageItem messageItem) {
		sent.put(abstractChat.getAccount(), message.getPacketID(), messageItem);
		if (abstractChat instanceof RoomChat)
			return;
		message.addExtension(new Request());
	}

	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		final String user = packet.getFrom();
		if (user == null)
			return;
		if (!(packet instanceof Message))
			return;
		final Message message = (Message) packet;
		if (message.getType() == Message.Type.error) {
			final MessageItem messageItem = sent.remove(account,
					message.getPacketID());
			if (messageItem != null && !messageItem.isError()) {
				messageItem.markAsError();
				Application.getInstance().runInBackground(new Runnable() {
					@Override
					public void run() {
						if (messageItem.getId() != null)
							MessageTable.getInstance().markAsError(
									messageItem.getId());
					}
				});
				MessageManager.getInstance().onChatChanged(
						messageItem.getChat().getAccount(),
						messageItem.getChat().getUser(), false);
			}
		} else {
			for (PacketExtension packetExtension : message.getExtensions())
				if (packetExtension instanceof Received) {
					Received received = (Received) packetExtension;
					String id = received.getId();
					if (id == null)
						id = message.getPacketID();
					if (id == null)
						continue;
					final MessageItem messageItem = sent.remove(account, id);
					if (messageItem != null && !messageItem.isDelivered()) {
						messageItem.markAsDelivered();
						MessageManager.getInstance().onChatChanged(
								messageItem.getChat().getAccount(),
								messageItem.getChat().getUser(), false);
					}
				} else if (packetExtension instanceof Request) {
					String id = message.getPacketID();
					if (id == null)
						continue;
					Message receipt = new Message(user);
					receipt.addExtension(new Received(id));
					receipt.setThread(message.getThread());
					try {
						ConnectionManager.getInstance().sendPacket(account,
								receipt);
					} catch (NetworkException e) {
						LogManager.exception(this, e);
					}
				}
		}
	}

	@Override
	public void onDisconnect(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		sent.clear(account);
	}

}
