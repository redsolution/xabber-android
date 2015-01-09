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
package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.widget.ListView;

/**
 * Enable smooth scrollbar depend on number of entities.
 * 
 * @author alexander.ivanov
 * 
 * @param <Inflater>
 */
public abstract class SmoothContactAdapter<Inflater extends BaseContactInflater>
		extends BaseContactAdapter<Inflater> {

	/**
	 * Minimum number of item when smooth scroll bar will be disabled.
	 */
	private static final int SMOOTH_SCROLLBAR_LIMIT = 20;

	/**
	 * Managed list view.
	 */
	ListView listView;

	public SmoothContactAdapter(Activity activity, ListView listView,
			Inflater inflater) {
		super(activity, inflater);
		this.listView = listView;
	}

	@Override
	public void onChange() {
		super.onChange();
		listView.setSmoothScrollbarEnabled(baseEntities.size() < SMOOTH_SCROLLBAR_LIMIT);
	}

}
