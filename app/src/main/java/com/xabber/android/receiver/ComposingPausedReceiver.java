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
package com.xabber.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.intent.EntityIntentBuilder;

/**
 * Receiver for scheduled pause of composing.
 * 
 * @author alexander.ivanov
 * 
 */
public class ComposingPausedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		ChatStateManager.getInstance().onPaused(intent, getAccount(intent),
				getUser(intent));
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ComposingPausedReceiver.class)
				.setAccount(account).setUser(user).build();
	}

	private static String getAccount(Intent intent) {
		return EntityIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
