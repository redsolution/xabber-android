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
package com.xabber.android.data.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Three level nested map.
 * 
 * Required inner maps will be created if necessary.
 * 
 * @author alexander.ivanov
 * 
 * @param <Key>
 * @param <Value>
 */
public class NestedNestedMaps<Key, Value> extends NestedMap<Map<Key, Value>> {

	public Value get(String first, String second, Key third) {
		Map<Key, Value> map = get(first, second);
		if (map == null)
			return null;
		return map.get(third);
	}

	synchronized public void put(String first, String second, Key third,
			Value value) {
		Map<Key, Value> map = get(first, second);
		if (map == null) {
			map = new HashMap<Key, Value>();
			put(first, second, map);
		}
		map.put(third, value);
	}

	synchronized public Value remove(String first, String second, Key third) {
		Map<Key, Value> map = get(first, second);
		if (map == null)
			return null;
		Value value = map.remove(third);
		if (map.isEmpty())
			remove(first, second);
		return value;
	}

}
