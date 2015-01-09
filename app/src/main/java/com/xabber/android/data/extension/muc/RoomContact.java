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

import android.graphics.drawable.Drawable;

import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.AbstractContact;

/**
 * Represents information about room in contact list.
 * 
 * {@link #getUser()} will be bare jid.
 * 
 * @author alexander.ivanov
 * 
 */
public class RoomContact extends AbstractContact {

	private final RoomChat roomItem;

	public RoomContact(RoomChat roomChat) {
		super(roomChat.getAccount(), roomChat.getUser());
		this.roomItem = roomChat;
	}

	@Override
	public String getStatusText() {
		return roomItem.getSubject();
	}

	@Override
	public StatusMode getStatusMode() {
		return roomItem.getState().toStatusMode();
	}

	@Override
	public Drawable getAvatar() {
		return AvatarManager.getInstance().getRoomAvatar(user);
	}

	@Override
	public Drawable getAvatarForContactList() {
		return AvatarManager.getInstance().getRoomAvatarForContactList(user);
	}

	@Override
	public boolean isConnected() {
		return roomItem.getState() == RoomState.available;
	}
}
