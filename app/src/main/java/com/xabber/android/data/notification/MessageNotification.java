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
package com.xabber.android.data.notification;

import java.util.Date;

import com.xabber.android.data.entity.BaseEntity;

/**
 * Notification for the contact.
 * 
 * @author alexander.ivanov
 * 
 */
public class MessageNotification extends BaseEntity {

	/**
	 * Text of the last message.
	 */
	private String text;

	/**
	 * Timestamp of the last message.
	 */
	private Date timestamp;

	/**
	 * Number of messages.
	 */
	private int count;

	public MessageNotification(String account, String user, String text,
			Date timestamp, int count) {
		super(account, user);
		this.text = text;
		this.timestamp = timestamp;
		this.count = count;
	}

	public String getText() {
		return text;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getCount() {
		return count;
	}

	public void addMessage(String text) {
		this.text = text;
		this.timestamp = new Date();
		this.count += 1;
	}

}
