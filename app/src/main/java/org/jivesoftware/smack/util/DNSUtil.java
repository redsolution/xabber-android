/**
 * $Revision: 1456 $
 * $Date: 2005-06-01 22:04:54 -0700 (Wed, 01 Jun 2005) $
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smack.util;

import java.util.Map;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Utilty class to perform DNS lookups for XMPP services.
 * 
 * @author Matt Tucker
 */
public class DNSUtil {

	/**
	 * Create a cache to hold the 100 most recently accessed DNS lookups for a
	 * period of 10 minutes.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, HostAddress> ccache = new Cache(100, 1000 * 60 * 10);
	@SuppressWarnings("unchecked")
	private static Map<String, HostAddress> scache = new Cache(100, 1000 * 60 * 10);

	private static HostAddress resolveSRV(String domain) {
		String bestHost = null;
		int bestPort = -1;
		int bestPriority = Integer.MAX_VALUE;
		int bestWeight = 0;
		Lookup lookup;
		try {
			lookup = new Lookup(domain, Type.SRV);
			Record recs[] = lookup.run();
			if (recs == null) { return null; }
			for (Record rec : recs) {
				SRVRecord record = (SRVRecord) rec;
				if (record != null && record.getTarget() != null) {
					int weight = (int) (record.getWeight() * record.getWeight() * Math
							.random());
					if (record.getPriority() < bestPriority) {
						bestPriority = record.getPriority();
						bestWeight = weight;
						bestHost = record.getTarget().toString();
						bestPort = record.getPort();
					} else if (record.getPriority() == bestPriority) {
						if (weight > bestWeight) {
							bestPriority = record.getPriority();
							bestWeight = weight;
							bestHost = record.getTarget().toString();
							bestPort = record.getPort();
						}
					}
                                }
			}
		} catch (TextParseException e) {
		} catch (NullPointerException e) {
                }
		if (bestHost == null) {
			return null;
		}
		// Host entries in DNS should end with a ".".
		if (bestHost.endsWith(".")) {
			bestHost = bestHost.substring(0, bestHost.length() - 1);
		}
		return new HostAddress(bestHost, bestPort);
	}

	/**
	 * Returns the host name and port that the specified XMPP server can be
	 * reached at for client-to-server communication. A DNS lookup for a SRV
	 * record in the form "_xmpp-client._tcp.example.com" is attempted,
	 * according to section 14.4 of RFC 3920. If that lookup fails, a lookup in
	 * the older form of "_jabber._tcp.example.com" is attempted since servers
	 * that implement an older version of the protocol may be listed using that
	 * notation. If that lookup fails as well, it's assumed that the XMPP server
	 * lives at the host resolved by a DNS lookup at the specified domain on the
	 * default port of 5222.
	 * <p>
	 * 
	 * As an example, a lookup for "example.com" may return
	 * "im.example.com:5269".
	 * 
	 * Note on SRV record selection. We now check priority and weight, but we
	 * still don't do this correctly. The missing behavior is this: if we fail
	 * to reach a host based on its SRV record then we need to select another
	 * host from the other SRV records. In Smack 3.1.1 we're not going to be
	 * able to do the major system redesign to correct this.
	 * 
	 * @param domain
	 *            the domain.
	 * @return a HostAddress, which encompasses the hostname and port that the
	 *         XMPP server can be reached at for the specified domain.
	 */
	public static HostAddress resolveXMPPDomain(String domain) {
		// Return item from cache if it exists.
		synchronized (ccache) {
			if (ccache.containsKey(domain)) {
				HostAddress address = (HostAddress) ccache.get(domain);
				if (address != null) {
					return address;
				}
			}
		}
		HostAddress result = resolveSRV("_xmpp-client._tcp." + domain);
		if (result == null) {
			result = resolveSRV("_jabber._tcp." + domain);
		}
		if (result == null) {
			result = new HostAddress(domain, 5222);
		}
		// Add item to cache.
		synchronized (ccache) {
			ccache.put(domain, result);
		}
		return result;
	}

	/**
	 * Returns the host name and port that the specified XMPP server can be
	 * reached at for server-to-server communication. A DNS lookup for a SRV
	 * record in the form "_xmpp-server._tcp.example.com" is attempted,
	 * according to section 14.4 of RFC 3920. If that lookup fails, a lookup in
	 * the older form of "_jabber._tcp.example.com" is attempted since servers
	 * that implement an older version of the protocol may be listed using that
	 * notation. If that lookup fails as well, it's assumed that the XMPP server
	 * lives at the host resolved by a DNS lookup at the specified domain on the
	 * default port of 5269.
	 * <p>
	 * 
	 * As an example, a lookup for "example.com" may return
	 * "im.example.com:5269".
	 * 
	 * @param domain
	 *            the domain.
	 * @return a HostAddress, which encompasses the hostname and port that the
	 *         XMPP server can be reached at for the specified domain.
	 */
	public static HostAddress resolveXMPPServerDomain(String domain) {
		// Return item from cache if it exists.
		synchronized (scache) {
			if (scache.containsKey(domain)) {
				HostAddress address = (HostAddress) scache.get(domain);
				if (address != null) {
					return address;
				}
			}
		}
		HostAddress result = resolveSRV("_xmpp-server._tcp." + domain);
		if (result == null) {
			result = resolveSRV("_jabber._tcp." + domain);
		}
		if (result == null) {
			result = new HostAddress(domain, 5269);
		}
		// Add item to cache.
		synchronized (scache) {
			scache.put(domain, result);
		}
		return result;
	}

	/**
	 * Encapsulates a hostname and port.
	 */
	public static class HostAddress {

		private String host;
		private int port;

		private HostAddress(String host, int port) {
			this.host = host;
			this.port = port;
		}

		/**
		 * Returns the hostname.
		 * 
		 * @return the hostname.
		 */
		public String getHost() {
			return host;
		}

		/**
		 * Returns the port.
		 * 
		 * @return the port.
		 */
		public int getPort() {
			return port;
		}

		public String toString() {
			return host + ":" + port;
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof HostAddress)) {
				return false;
			}

			final HostAddress address = (HostAddress) o;

			if (!host.equals(address.host)) {
				return false;
			}
			return port == address.port;
		}
	}
}
