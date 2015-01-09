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
package com.xabber.android.data.connection;

import java.util.HashMap;
import java.util.Map.Entry;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;

public class ReconnectionManager implements OnConnectionListener,
		OnConnectedListener, OnAccountRemovedListener, OnTimerListener {

	/**
	 * Intervals in seconds to be used for attempt to reconnect. First value
	 * will be used on first attempt. Next value will be used if reconnection
	 * fails. Last value will be used if there is no more values in array.
	 */
	private final static int RECONNECT_AFTER[] = new int[] { 2, 10, 30, 60 };

	/**
	 * Managed connections.
	 */
	private final HashMap<ConnectionItem, ReconnectionInfo> connections;

	private final static ReconnectionManager instance;

	static {
		instance = new ReconnectionManager(Application.getInstance());
		Application.getInstance().addManager(instance);
	}

	public static ReconnectionManager getInstance() {
		return instance;
	}

	private ReconnectionManager(Application application) {
		connections = new HashMap<ConnectionItem, ReconnectionInfo>();
	}

	@Override
	public void onTimer() {
		for (Entry<ConnectionItem, ReconnectionInfo> entry : connections
				.entrySet()) {
			ReconnectionInfo reconnectionInfo = entry.getValue();
			ConnectionItem connectionItem = entry.getKey();
			if (connectionItem.getState() == ConnectionState.waiting) {
				int reconnectAfter;
				if (reconnectionInfo.reconnectAttempts < RECONNECT_AFTER.length)
					reconnectAfter = RECONNECT_AFTER[reconnectionInfo.reconnectAttempts];
				else
					reconnectAfter = RECONNECT_AFTER[RECONNECT_AFTER.length - 1];
				if (reconnectionInfo.reconnectCounter >= reconnectAfter) {
					reconnectionInfo.reconnectCounter = 0;
					reconnectionInfo.reconnectAttempts += 1;
					connectionItem.updateConnection(false);
					if (connectionItem instanceof AccountItem)
						AccountManager.getInstance().onAccountChanged(
								((AccountItem) connectionItem).getAccount());
				} else {
					reconnectionInfo.reconnectCounter += 1;
				}
			} else {
				reconnectionInfo.reconnectCounter = 0;
			}
		}
	}

	@Override
	public void onConnection(ConnectionItem connection) {
		ReconnectionInfo info = connections.get(connection);
		if (info == null) {
			info = new ReconnectionInfo();
			connections.put(connection, info);
		}
		info.reconnectAttempts += 1;
		info.reconnectCounter = 0;
	}

	@Override
	public void onConnected(ConnectionItem connection) {
		ReconnectionInfo info = connections.get(connection);
		info.reconnectAttempts = 0;
		info.reconnectCounter = 0;
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		connections.remove(accountItem);
	}

	/**
	 * Information about reconnection attempts.
	 * 
	 * @author alexander.ivanov
	 * 
	 */
	private static class ReconnectionInfo {

		/**
		 * Number of attempts to reconnect without success.
		 */
		int reconnectAttempts = 0;

		/**
		 * Number of seconds passed from last reconnection.
		 */
		int reconnectCounter = 0;

	}

}
