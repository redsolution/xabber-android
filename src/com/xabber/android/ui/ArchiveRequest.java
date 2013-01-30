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
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.helper.ManagedDialog;
import com.xabber.androiddev.R;

/**
 * Dialog with request to enable message archive.
 * 
 * @author alexander.ivanov
 * 
 */
public class ArchiveRequest extends ManagedDialog {

	private String account;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findViewById(android.R.id.button3).setVisibility(View.GONE);
		setDialogMessage(R.string.archive_available_request_message);
		account = getAccount(getIntent());
		if (AccountManager.getInstance().getAccount(account) == null) {
			Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
			finish();
			return;
		}
	}

	@Override
	public void onAccept() {
		super.onAccept();
		AccountManager.getInstance()
				.setArchiveMode(account, ArchiveMode.server);
		finish();
	}

	@Override
	public void onDecline() {
		super.onDecline();
		AccountManager.getInstance().setArchiveMode(account, ArchiveMode.local);
		finish();
	}

	public static Intent createIntent(Context context, String account) {
		return new AccountIntentBuilder(context, ArchiveRequest.class)
				.setAccount(account).build();
	}

	private static String getAccount(Intent intent) {
		return AccountIntentBuilder.getAccount(intent);
	}

}
