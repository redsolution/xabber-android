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
package com.xabber.android.data.extension.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Feature;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnAuthorizedListener;
import com.xabber.android.data.connection.OnResponseListener;
import com.xabber.xmpp.address.Jid;

public class ServerInfoManager implements OnAuthorizedListener,
		OnResponseListener {

	/**
	 * Protocols supported by accounts.
	 */
	private final Map<String, Collection<String>> protocols;

	private final static ServerInfoManager instance;

	static {
		instance = new ServerInfoManager();
		Application.getInstance().addManager(instance);
	}

	public static ServerInfoManager getInstance() {
		return instance;
	}

	private ServerInfoManager() {
		super();
		protocols = new HashMap<String, Collection<String>>();
	}

	@Override
	public void onAuthorized(ConnectionItem connection) {
		if (connection instanceof AccountItem) {
			String account = ((AccountItem) connection).getAccount();
			if (protocols.get(account) == null) {
				DiscoverInfo packet = new DiscoverInfo();
				packet.setTo(Jid.getServer(account));
				packet.setType(Type.GET);
				try {
					ConnectionManager.getInstance().sendRequest(account,
							packet, this);
				} catch (NetworkException e) {
				}
				return;
			}
		}
		onAvailable(connection);
	}

	private void onAvailable(ConnectionItem connection) {
		for (OnServerInfoReceivedListener listener : Application.getInstance()
				.getManagers(OnServerInfoReceivedListener.class))
			listener.onServerInfoReceived(connection);
	}

	@Override
	public void onReceived(String account, String packetId, IQ iq) {
		if (!(iq instanceof DiscoverInfo)) {
			onError(account, packetId, iq);
			return;
		}
		ArrayList<String> features = new ArrayList<String>();
		DiscoverInfo discoverInfo = (DiscoverInfo) iq;
		Iterator<Feature> iterator = discoverInfo.getFeatures();
		while (iterator.hasNext())
			features.add(iterator.next().getVar());
		protocols.put(account, features);
		onAvailable(AccountManager.getInstance().getAccount(account));
	}

	@Override
	public void onError(String account, String packetId, IQ iq) {
		protocols.put(account, new ArrayList<String>());
		onAvailable(AccountManager.getInstance().getAccount(account));
	}

	@Override
	public void onTimeout(String account, String packetId) {
		onError(account, packetId, null);
	}

	@Override
	public void onDisconnect(String account, String packetId) {
	}

	public boolean isProtocolSupported(String account, String feature) {
		Collection<String> collection = protocols.get(account);
		if (collection == null)
			return false;
		return collection.contains(feature);
	}

}
