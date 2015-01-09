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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

/**
 * Inflate view with contact's status mode.
 * 
 * @author alexander.ivanov
 * 
 */
public class StatusContactInflater extends BaseContactInflater {

	public StatusContactInflater(Activity activity) {
		super(activity);
	}

	@Override
	View createView(int position, ViewGroup parent) {
		return layoutInflater
				.inflate(R.layout.base_contact_item, parent, false);
	}

	@Override
	ViewHolder createViewHolder(int position, View view) {
		return new StatusContactInflater.ViewHolder(view);
	}

	@Override
	public void getView(View view, AbstractContact abstractContact) {
		super.getView(view, abstractContact);
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		viewHolder.statusMode.setImageLevel(abstractContact.getStatusMode()
				.getStatusLevel());
	}

	static class ViewHolder extends BaseContactInflater.ViewHolder {

		final ImageView statusMode;

		public ViewHolder(View view) {
			super(view);
			statusMode = (ImageView) view.findViewById(R.id.status_mode);
		}
	}

}
