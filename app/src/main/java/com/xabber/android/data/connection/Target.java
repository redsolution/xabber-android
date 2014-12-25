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

import org.xbill.DNS.SRVRecord;

/**
 * Single target and information about its addresses.
 * 
 * @author alexander.ivanov
 * 
 */
public class Target implements Comparable<Target> {

	private final String host;
	private final int port;
	private final int priority;
	private final int weight;

	public Target(SRVRecord srvRecord) {
		super();
		this.host = srvRecord.getTarget().toString();
		this.port = srvRecord.getPort();
		this.priority = srvRecord.getPriority();
		this.weight = Math.max(0, srvRecord.getWeight());
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	int getPriority() {
		return priority;
	}

	int getWeight() {
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + priority;
		result = prime * result + weight;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Target other = (Target) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (priority != other.priority)
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}

	@Override
	public int compareTo(Target another) {
		int result = priority - another.priority;
		if (result == 0)
			return another.weight - weight;
		return result;
	}

	@Override
	public String toString() {
		return priority + " " + weight + " " + port + " " + host;
	}

}
