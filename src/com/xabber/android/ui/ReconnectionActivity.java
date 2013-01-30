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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.connection.ConnectionManager;

/**
 * Activity launched from notification bar to reconnect disconnected accounts.
 * 
 * @author alexander.ivanov
 * 
 */
public class ReconnectionActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LogManager.i(this, "onReconnect");
		ConnectionManager.getInstance().updateConnections(false);
		startActivity(ContactList.createPersistentIntent(this));
		finish();
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, ReconnectionActivity.class);
	}

}