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
package com.xabber.android.data.roster;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains information about resources for specified bare address.
 * 
 * @author alexander.ivanov
 * 
 */
class ResourceContainer {

	/**
	 * List of available resources for its STRING-PREPed resources.
	 */
	private final Map<String, ResourceItem> resourceItems;

	/**
	 * Best resource.
	 */
	private ResourceItem best;

	public ResourceContainer() {
		resourceItems = new HashMap<String, ResourceItem>();
		best = null;
	}

	/**
	 * @return Best resource according priority. <code>null</code> can be
	 *         returned.
	 */
	ResourceItem getBest() {
		return best;
	}

	/**
	 * Update information about best resource.
	 */
	void updateBest() {
		String bestKey = null;
		ResourceItem bestResource = null;
		for (Entry<String, ResourceItem> entry : resourceItems.entrySet()) {
			String key = entry.getKey();
			ResourceItem resource = entry.getValue();
			if (bestResource == null) {
				bestKey = key;
				bestResource = resource;
			} else {
				int result = resource.compareTo(bestResource);
				if (result > 0 || (result == 0 && key.compareTo(bestKey) > 0))
					bestResource = resource;
			}
		}
		this.best = bestResource;
	}

	/**
	 * @param resource
	 * @return Resource item for specified resource name. <code>null</code> can
	 *         be returned.
	 */
	ResourceItem get(String resource) {
		return resourceItems.get(resource);
	}

	/**
	 * Adds new resource item.
	 * 
	 * @param resource
	 * @param resourceItem
	 */
	void put(String resource, ResourceItem resourceItem) {
		resourceItems.put(resource, resourceItem);
	}

	/**
	 * Removes resource item.
	 * 
	 * @param resource
	 */
	void remove(String resource) {
		resourceItems.remove(resource);
	}

	/**
	 * @return Collection with available resources.
	 */
	Collection<ResourceItem> getResourceItems() {
		return Collections.unmodifiableCollection(resourceItems.values());
	}

}
