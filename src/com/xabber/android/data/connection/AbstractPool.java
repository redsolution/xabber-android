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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Address pool.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractPool<Item, Source> {

	protected final Queue<Item> pool;
	protected final Queue<Item> used;

	public AbstractPool() {
		this.pool = new LinkedList<Item>();
		this.used = new LinkedList<Item>();
	}

	/**
	 * Updates pool with specified values.
	 * 
	 * @param values
	 *            <code>null</code> if no changes in the pool should be made.
	 */
	synchronized public void update(Source[] values) {
		if (values == null)
			return;
		List<Item> items = new ArrayList<Item>();
		for (Source value : values) {
			Item item = convert(value);
			if (item != null)
				items.add(item);
		}
		update(items);
	}

	/**
	 * @param value
	 * @return Converted value or <code>null</code> if value shouldn't be used.
	 */
	abstract Item convert(Source value);

	/**
	 * Checks and updates pool with items.
	 * 
	 * @param items
	 */
	abstract void update(List<Item> items);

	/**
	 * Returns first value from the pool. Make it as used. Reset pool if it is
	 * empty.
	 * 
	 * @return <code>null</code> if pool was empty.
	 */
	synchronized public Item getNext() {
		if (pool.isEmpty()) {
			pool.addAll(used);
			used.clear();
			return null;
		}
		Item address = pool.remove();
		used.add(address);
		return address;
	}

}
