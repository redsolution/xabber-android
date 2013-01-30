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

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.StatusMode;
import com.xabber.androiddev.R;

/**
 * Adapter for available status modes.
 * 
 * @author alexander.ivanov
 * 
 */
public class StatusModeAdapter extends BaseAdapter {
	private final Activity activity;
	private final ArrayList<StatusMode> statusModes;

	public StatusModeAdapter(Activity activity) {
		super();
		this.activity = activity;
		statusModes = new ArrayList<StatusMode>();
		statusModes.add(StatusMode.chat);
		statusModes.add(StatusMode.available);
		statusModes.add(StatusMode.away);
		statusModes.add(StatusMode.xa);
		statusModes.add(StatusMode.dnd);
		// statusModes.add(StatusMode.invisible);
		statusModes.add(StatusMode.unavailable);
	}

	@Override
	public int getCount() {
		return statusModes.size();
	}

	@Override
	public Object getItem(int position) {
		return statusModes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private void updateView(int position, View view) {
		StatusMode statusMode = (StatusMode) getItem(position);
		((ImageView) view.findViewById(R.id.icon)).setImageLevel(statusMode
				.getStatusLevel());
		((TextView) view.findViewById(R.id.name)).setText(statusMode
				.getStringID());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.status_mode_item, parent, false);
		} else {
			view = convertView;
		}
		updateView(position, view);
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.status_mode_dropdown, parent, false);
		} else {
			view = convertView;
		}
		updateView(position, view);
		return view;
	}
}
