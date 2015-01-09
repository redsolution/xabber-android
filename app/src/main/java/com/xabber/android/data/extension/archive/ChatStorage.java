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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.archive.AbstractMessage;
import com.xabber.xmpp.archive.Chat;
import com.xabber.xmpp.archive.From;

/**
 * Collect messages for the chat collection received from the message archive.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatStorage {

	/**
	 * Whether received changes has been applied.
	 */
	private boolean applied;

	/**
	 * Whether all messages have been received.
	 */
	private boolean received;

	/**
	 * Last received chat version.
	 */
	private Integer version;

	/**
	 * Accumulated timestamp.
	 */
	private Date timestamp;

	/**
	 * Received messages.
	 */
	private final Collection<MessageItem> items;

	public ChatStorage(Date timestamp) {
		super();
		applied = false;
		received = false;
		version = null;
		items = new ArrayList<MessageItem>();
		this.timestamp = timestamp;
	}

	public boolean isReceived() {
		return received;
	}

	public void onItemsReceived(Integer version) {
		this.version = version;
		received = true;
	}

	public boolean hasVersion(Integer version) {
		return this.version != null && version != null
				&& this.version.equals(version);
	}

	public Collection<MessageItem> getItems() {
		return Collections.unmodifiableCollection(items);
	}

	public void addItem(AbstractChat abstractChat, Chat chat,
			AbstractMessage message, long offset) {
		boolean incoming = message instanceof From;
		if (message.getUtc() == null)
			timestamp = new Date(timestamp.getTime() + message.getSecs() * 1000);
		else
			timestamp = message.getUtc();
		String body = message.getBody();
		net.java.otr4j.io.messages.AbstractMessage otrMessage;
		try {
			otrMessage = SerializationUtils.toMessage(body);
		} catch (IOException e) {
			return;
		}
		if (otrMessage != null) {
			if (otrMessage.messageType != net.java.otr4j.io.messages.AbstractMessage.MESSAGE_PLAINTEXT)
				return;
			body = ((PlainTextMessage) otrMessage).cleanText;
		}
		MessageItem messageItem = new MessageItem(abstractChat,
				chat.getStartString(),
				Jid.getResource(chat.getWith()), body, null,
				new Date(timestamp.getTime() - offset), null, incoming, true,
				true, false, true, false, false);
		items.add(messageItem);
	}

	public boolean isApplied() {
		return applied;
	}

	/**
	 * Received messages has been added to the history and can be cleared.
	 */
	public void onApplied() {
		items.clear();
		applied = true;
	}

}
