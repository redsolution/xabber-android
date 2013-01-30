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
import java.util.ArrayList;
import java.util.List;

/**
 * Addresses holder for the host.
 * 
 * @author alexander.ivanov
 * 
 */
public class Host extends AbstractPool<InetAddress, InetAddress> {

	public Host() {
		super();
	}

	@Override
	InetAddress convert(InetAddress value) {
		return value;
	}

	@Override
	void update(List<InetAddress> items) {
		List<InetAddress> exists = new ArrayList<InetAddress>(pool);
		exists.addAll(used);
		for (InetAddress item : items)
			if (!exists.remove(item))
				pool.add(item);
		pool.removeAll(exists);
		used.removeAll(exists);
	}

}
