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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.widget.BaseAdapter;

import com.xabber.android.ui.helper.BaseListEditor;

/**
 * This class manage abstract list for {@link BaseListEditor}.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class BaseListEditorAdapter<T> extends BaseAdapter implements
		UpdatableAdapter {

	private final Activity activity;
	private final List<T> tags;

	public BaseListEditorAdapter(Activity activity) {
		super();
		this.activity = activity;
		this.tags = new ArrayList<T>();
	}

	protected Activity getActivity() {
		return activity;
	}

	@Override
	public int getCount() {
		return tags.size();
	}

	@Override
	public T getItem(int position) {
		return tags.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public void onChange() {
		tags.clear();
		tags.addAll(getTags());
		notifyDataSetChanged();
	}

	protected abstract Collection<T> getTags();

}
