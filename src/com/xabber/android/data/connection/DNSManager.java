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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.xbill.DNS.Address;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;

/**
 * Manage SRV records.
 * 
 * @author alexander.ivanov
 * 
 */
public class DNSManager {

	private static final DNSManager instance;

	static {
		instance = new DNSManager();
		Application.getInstance().addManager(instance);
	}

	public static DNSManager getInstance() {
		return instance;
	}

	/**
	 * Map with SRV records for each FQDN.
	 */
	private final Map<String, SRVContainer> srvs;

	/**
	 * Map with Addresses for each host.
	 */
	private final Map<String, Host> hosts;

	/**
	 * Whether resolver update has been requested.
	 */
	private boolean resolverUpdateRequested;

	/**
	 * Lock to update resolver only once per request.
	 */
	private final Object resolverUpdateLock;

	private DNSManager() {
		srvs = new HashMap<String, SRVContainer>();
		hosts = new HashMap<String, Host>();
		resolverUpdateLock = new Object();
		resolverUpdateRequested = true;
	}

	/**
	 * Requests to update resolver before the next SRV resolving.
	 */
	public void requestResolverUpdate() {
		resolverUpdateRequested = true;
	}

	/**
	 * Updates DNS server on demand.
	 */
	private void updateDNSServer() {
		synchronized (resolverUpdateLock) {
			if (!resolverUpdateRequested)
				return;
			resolverUpdateRequested = false;
			ResolverConfig.refresh();
			ExtendedResolver resolver;
			try {
				resolver = new ExtendedResolver();
			} catch (UnknownHostException e) {
				LogManager.exception(this, e);
				return;
			}
			for (Resolver check : resolver.getResolvers())
				if (check instanceof SimpleResolver) {
					LogManager.i(check, "Current timeout is "
							+ ((SimpleResolver) check).getTimeout());
					((SimpleResolver) check).setTimeout(30);
					LogManager.i(check, "new value is "
							+ ((SimpleResolver) check).getTimeout());
				} else {
					LogManager.i(this, "Not simple resolver!!!?" + check);
				}
			synchronized (Lookup.class) {
				Lookup.setDefaultResolver(resolver);
				Lookup.setDefaultSearchPath(ResolverConfig.getCurrentConfig()
						.searchPath());
			}
			String message = "DNS servers:\n";
			if (ResolverConfig.getCurrentConfig().servers() == null)
				message += ResolverConfig.getCurrentConfig().servers();
			else
				for (String server : ResolverConfig.getCurrentConfig()
						.servers())
					message += server + "\n";
			LogManager.i(this, message);
		}
	}

	/**
	 * Requests records for the given resource name.
	 * 
	 * @param name
	 * @return
	 */
	private Record[] getRecords(String name) {
		try {
			return new Lookup(name, Type.SRV).run();
		} catch (TextParseException e) {
		} catch (NullPointerException e) {
		} catch (ExceptionInInitializerError e) {
		}
		return null;
	}

	/**
	 * Requests SRV records for specified server.
	 * 
	 * @param fqdn
	 * @return
	 */
	public Record[] fetchRecords(String fqdn) {
		updateDNSServer();
		Record recs[] = getRecords("_xmpp-client._tcp." + fqdn);
		if (recs != null)
			return recs;
		return getRecords("_jabber._tcp." + fqdn);
	}

	/**
	 * Updates information about server when {@link #fetchRecords(String)} has
	 * been completed.
	 * 
	 * @param fqdn
	 * @param records
	 */
	public void onRecordsReceived(String fqdn, Record[] records) {
		SRVContainer entity = srvs.get(fqdn);
		if (entity == null) {
			entity = new SRVContainer();
			srvs.put(fqdn, entity);
		}
		entity.update(records);
		String message = "Update records for " + fqdn + ":\n";
		if (records == null)
			message += records;
		else
			for (Record record : records)
				message += record + "\n";
		LogManager.i(this, message);
	}

	/**
	 * Requests addresses for specified target.
	 * 
	 * @param target
	 * @return
	 */
	public InetAddress[] fetchAddresses(String host) {
		updateDNSServer();
		try {
			// TODO: AAAA support.
			return Address.getAllByName(host);
		} catch (UnknownHostException e) {
		}
		return null;
	}

	/**
	 * Updates information about servers when {@link #fetchAddresses(String)}
	 * has been completed.
	 * 
	 * @param host
	 * @param addresses
	 */
	public void onAddressesReceived(String host, InetAddress[] addresses) {
		Host entity = hosts.get(host);
		if (entity == null) {
			entity = new Host();
			hosts.put(host, entity);
		}
		entity.update(addresses);
		String message = "Update address for " + host + ":\n";
		if (addresses == null)
			message += addresses;
		else
			for (InetAddress record : addresses)
				message += record.toString() + "\n";
		LogManager.i(this, message);
	}

	/**
	 * Returns current target to be used.
	 * 
	 * @return <code>null</code> if there is no available targets.
	 */
	public Target getCurrentTarget(String fqdn) {
		SRVContainer srv = srvs.get(fqdn);
		if (srv == null)
			return null;
		return srv.getCurrent();
	}

	/**
	 * Returns first value from the pool. Make it as used. Reset pool if it is
	 * empty.
	 * 
	 * @param server
	 * @return <code>null</code> if pool was empty.
	 */
	public Target getNextTarget(String server) {
		SRVContainer srv = srvs.get(server);
		if (srv == null)
			return null;
		return srv.getNext();
	}

	/**
	 * Returns first value from the pool. Make it as used. Reset pool if it is
	 * empty.
	 * 
	 * @param host
	 * @return <code>null</code> if pool was empty.
	 */
	public InetAddress getNextAddress(String host) {
		Host entity = hosts.get(host);
		if (entity == null)
			return null;
		return entity.getNext();
	}

}
