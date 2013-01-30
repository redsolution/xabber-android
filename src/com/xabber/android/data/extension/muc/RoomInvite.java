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

import android.content.Intent;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.notification.EntityNotificationItem;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.MUCEditor;
import com.xabber.androiddev.R;

/**
 * Invite to join the room.
 * 
 * @author alexander.ivanov
 * 
 */
public class RoomInvite extends BaseEntity implements EntityNotificationItem {

	/**
	 * JID of entity that sent an invitation.
	 */
	private final String inviter;

	/**
	 * Text of invitation.
	 */
	private final String reason;

	/**
	 * Password to be used in connection.
	 */
	private final String password;

	public RoomInvite(String account, String user, String inviter,
			String reason, String password) {
		super(account, user);
		this.inviter = inviter;
		this.reason = reason == null ? "" : reason;
		this.password = password == null ? "" : password;
	}

	@Override
	public Intent getIntent() {
		return MUCEditor.createInviteIntent(Application.getInstance(), account,
				user);
	}

	@Override
	public String getText() {
		return Application.getInstance().getString(R.string.muc_invite_message);
	}

	@Override
	public String getTitle() {
		return user;
	}

	/**
	 * @return Text for the confirmation.
	 */
	public String getConfirmation() {
		String accountName = AccountManager.getInstance().getVerboseName(
				account);
		String inviterName = RosterManager.getInstance().getName(account,
				inviter);
		if (reason == null || "".equals(reason))
			return Application.getInstance()
					.getString(R.string.muc_invite_confirm, accountName,
							inviterName, user);
		else
			return Application.getInstance().getString(
					R.string.muc_invite_confirm_reason, accountName,
					inviterName, user, reason);
	}

	public String getInviter() {
		return inviter;
	}

	public String getReason() {
		return reason;
	}

	public String getPassword() {
		return password;
	}

}
