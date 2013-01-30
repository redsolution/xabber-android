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
package com.xabber.android.data.extension.avatar;

import java.util.HashMap;
import java.util.Map;

import android.content.res.TypedArray;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLowMemoryListener;

/**
 * Set of default avatars.
 * 
 * Calculation takes not much time, so we don't use locks.
 * 
 * @author alexander.ivanov
 * 
 */
public class BaseAvatarSet implements OnLowMemoryListener {
	/**
	 * Default resources.
	 */
	private final int[] AVATARS;

	/**
	 * Map with resource ids for specified uses.
	 */
	private final Map<String, Integer> resources;

	public BaseAvatarSet(Application application, int array, int defaultDrawable) {
		TypedArray defaultAvatars = application.getResources()
				.obtainTypedArray(array);
		AVATARS = new int[defaultAvatars.length()];
		for (int index = 0; index < defaultAvatars.length(); index++)
			AVATARS[index] = defaultAvatars.getResourceId(index,
					defaultDrawable);
		defaultAvatars.recycle();
		resources = new HashMap<String, Integer>();
	}

	/**
	 * Calculate avatar index for specified user.
	 * 
	 * @param user
	 * @return
	 */
	protected int getIndex(String user) {
		return user.hashCode();
	}

	/**
	 * Get drawable with default avatar of user.
	 * 
	 * @param user
	 * @return
	 */
	public int getResourceId(String user) {
		Integer resource = resources.get(user);
		if (resource == null) {
			resource = getElement(getIndex(user), AVATARS);
			resources.put(user, resource);
		}
		return resource;
	}

	/**
	 * Gets element from array by index.
	 * 
	 * @param index
	 * @param array
	 * @return Always return element even if array's length is less then index.
	 */
	private int getElement(int index, int[] array) {
		index = index % array.length;
		if (index < 0)
			index += array.length;
		return array[index];
	}

	@Override
	public void onLowMemory() {
		resources.clear();
	}
}
