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
import java.util.Collections;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.SavedStatus;
import com.xabber.androiddev.R;

/**
 * Adapter for saved statuses.
 * 
 * @author alexander.ivanov
 * 
 */
public class StatusEditorAdapter extends BaseAdapter implements
		UpdatableAdapter {

	private final Activity activity;
	private final ArrayList<SavedStatus> statuses;

	public StatusEditorAdapter(Activity activity) {
		super();
		this.activity = activity;
		statuses = new ArrayList<SavedStatus>();
	}

	@Override
	public int getCount() {
		return statuses.size();
	}

	@Override
	public Object getItem(int position) {
		return statuses.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.status_editor_item, parent, false);
		} else {
			view = convertView;
		}
		final SavedStatus status = (SavedStatus) getItem(position);
		((ImageView) view.findViewById(R.id.icon)).setImageLevel(status
				.getStatusMode().getStatusLevel());
		String text = status.getStatusText();
		if ("".equals(text))
			text = activity.getString(R.string.empty_status);
		((TextView) view.findViewById(R.id.name)).setText(text);
		return view;
	}

	@Override
	public void onChange() {
		statuses.clear();
		statuses.addAll(AccountManager.getInstance().getSavedStatuses());
		Collections.sort(statuses);
		notifyDataSetChanged();
	}
}
