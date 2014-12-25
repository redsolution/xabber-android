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
package com.xabber.android.data.extension.otr;

import android.content.Intent;

import com.xabber.android.data.Application;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.notification.EntityNotificationItem;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.QuestionViewer;
import com.xabber.androiddev.R;

public class SMRequest extends BaseEntity implements EntityNotificationItem {

	private final String question;

	public SMRequest(String account, String user, String question) {
		super(account, user);
		this.question = question;
	}

	@Override
	public Intent getIntent() {
		return QuestionViewer.createIntent(
				Application.getInstance(), account, user, question != null,
				true, question);
	}

	@Override
	public String getTitle() {
		return Application.getInstance().getString(R.string.otr_verification);
	}

	@Override
	public String getText() {
		return RosterManager.getInstance().getName(account, user);
	}

}
