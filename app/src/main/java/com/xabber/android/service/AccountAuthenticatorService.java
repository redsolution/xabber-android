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
package com.xabber.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.xabber.android.data.account.AccountAuthenticator;

/**
 * Service required for system contact list integration.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountAuthenticatorService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(
				android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			return AccountAuthenticator.getInstance().getIBinder();
		else
			return null;
	}

}
