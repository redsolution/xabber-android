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
package com.xabber.android.data.extension.time;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.connection.OnResponseListener;
import com.xabber.android.data.extension.capability.OnServerInfoReceivedListener;
import com.xabber.android.data.extension.capability.ServerInfoManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.time.Time;

/**
 * Manage server time and response with local time.
 * 
 * @author alexander.ivanov
 * 
 */
public class TimeManager implements OnServerInfoReceivedListener,
		OnPacketListener, OnResponseListener {

	private static final String FEATURE = "urn:xmpp:time";

	/**
	 * Offset from server time per account.
	 */
	private final Map<String, Long> offsets;

	/**
	 * Time when request has been sent per account.
	 */
	private final Map<String, Date> sents;

	private final static TimeManager instance;

	static {
		instance = new TimeManager();
		Application.getInstance().addManager(instance);

		Connection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					@Override
					public void connectionCreated(final Connection connection) {
						ServiceDiscoveryManager.getInstanceFor(connection)
								.addFeature(FEATURE);
					}
				});
	}

	public static TimeManager getInstance() {
		return instance;
	}

	private TimeManager() {
		super();
		offsets = new HashMap<String, Long>();
		sents = new HashMap<String, Date>();
	}

	@Override
	public void onServerInfoReceived(ConnectionItem connection) {
		if (!(connection instanceof AccountItem)) {
			onAvailable(connection);
			return;
		}
		String account = ((AccountItem) connection).getAccount();
		if (ServerInfoManager.getInstance().isProtocolSupported(account,
				FEATURE)
				&& offsets.get(account) == null) {
			sents.put(account, new Date());
			Time packet = new Time();
			packet.setTo(Jid.getServer(account));
			packet.setType(Type.GET);
			try {
				ConnectionManager.getInstance().sendRequest(account, packet,
						this);
			} catch (NetworkException e) {
			}
			return;
		}
		onAvailable(connection);
	}

	@Override
	public void onPacket(ConnectionItem connection, final String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (!(packet instanceof Time))
			return;
		Time time = (Time) packet;
		if (time.getType() == Type.GET) {
			Time result = new Time();
			result.setType(Type.RESULT);
			result.setPacketID(time.getPacketID());
			result.setFrom(time.getTo());
			result.setTo(time.getFrom());
			Calendar calendar = Calendar.getInstance();
			result.setTzo((calendar.get(Calendar.ZONE_OFFSET) + calendar
					.get(Calendar.DST_OFFSET)) / 60000);
			result.setUtc(calendar.getTime());
			try {
				ConnectionManager.getInstance().sendPacket(account, result);
			} catch (NetworkException e) {
			}
		}
	}

	private void onAvailable(ConnectionItem connection) {
		for (OnTimeReceivedListener listener : Application.getInstance()
				.getManagers(OnTimeReceivedListener.class))
			listener.onTimeReceived(connection);
	}

	@Override
	public void onReceived(String account, String packetId, IQ iq) {
		if (!(iq instanceof Time) || !((Time) iq).isValid()) {
			onError(account, packetId, iq);
			return;
		}
		Date t1 = sents.remove(account);
		Date t2_3 = ((Time) iq).getUtc();
		Date t4 = ((Time) iq).getCreated();
		long offset = ((t2_3.getTime() - t1.getTime()) + (t2_3.getTime() - t4
				.getTime())) / 2;
		offsets.put(account, offset);
		onAvailable(AccountManager.getInstance().getAccount(account));
	}

	@Override
	public void onError(String account, String packetId, IQ iq) {
		onDisconnect(account, packetId);
		offsets.put(account, 0l);
		onAvailable(AccountManager.getInstance().getAccount(account));
	}

	@Override
	public void onTimeout(String account, String packetId) {
		onError(account, packetId, null);

	}

	@Override
	public void onDisconnect(String account, String packetId) {
		sents.remove(account);
	}

	/**
	 * Gets difference between local and server time.
	 * 
	 * @param account
	 * @return 0 if there is no information about offset.
	 */
	public long getServerTimeOffset(String account) {
		Long value = offsets.get(account);
		if (value == null)
			return 0l;
		return value;
	}

	public Date getServerTime(String account) {
		return new Date(new Date().getTime() + getServerTimeOffset(account));
	}

}
