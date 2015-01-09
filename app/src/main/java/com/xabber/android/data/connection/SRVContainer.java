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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;

/**
 * Holder for SRV records for the same server.
 * 
 * @author alexander.ivanov
 * 
 */
class SRVContainer extends AbstractPool<Target, Record> {

	private final Random random;

	SRVContainer() {
		super();
		random = new Random();
	}

	@Override
	Target convert(Record value) {
		if (value instanceof SRVRecord)
			return new Target((SRVRecord) value);
		return null;
	}

	/**
	 * @param items
	 * @return Whether passed targets differs from previous one.
	 */
	private boolean hasChanges(Collection<Target> items) {
		Collection<Target> exists = new ArrayList<Target>(pool);
		exists.addAll(used);
		for (Target target : items)
			if (!exists.remove(target))
				return true;
		return !exists.isEmpty();
	}

	@Override
	void update(List<Target> items) {
		if (!hasChanges(items) && !pool.isEmpty())
			return;
		pool.clear();
		used.clear();
		Collections.sort(items);
		int[] values = new int[items.size()];
		while (!items.isEmpty()) {
			int sum = 0;
			int index = 0;
			int priority = items.get(0).getPriority();
			while (index < items.size()) {
				Target target = items.get(index);
				if (priority != target.getPriority())
					break;
				sum += target.getWeight();
				values[index] = sum;
				index += 1;
			}
			int value = random.nextInt(sum + 1);
			for (index = 0; index < items.size(); index++)
				if (values[index] >= value)
					break;
			Target selected = items.remove(index);
			pool.add(selected);
		}
	}

	/**
	 * @return Current first target or <code>null</code> if pool is empty.
	 */
	synchronized public Target getCurrent() {
		return pool.peek();
	}

}
