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
import android.widget.ImageView;

import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

/**
 * Inflate view with contact's client software.
 * 
 * @author alexander.ivanov
 * 
 */
public class ClientContactInflater extends StatusContactInflater {

	public ClientContactInflater(Activity activity) {
		super(activity);
	}

	@Override
	ViewHolder createViewHolder(int position, View view) {
		return new ClientContactInflater.ViewHolder(view);
	}

	@Override
	public void getView(View view, AbstractContact abstractContact) {
		super.getView(view, abstractContact);
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		ClientSoftware clientSoftware = abstractContact.getClientSoftware();
		if (clientSoftware == ClientSoftware.unknown)
			viewHolder.clientSoftware.setVisibility(View.INVISIBLE);
		else {
			viewHolder.clientSoftware.setVisibility(View.VISIBLE);
			viewHolder.clientSoftware.setImageLevel(clientSoftware.ordinal());
		}
	}

	static class ViewHolder extends StatusContactInflater.ViewHolder {

		final ImageView clientSoftware;

		public ViewHolder(View view) {
			super(view);
			clientSoftware = (ImageView) view
					.findViewById(R.id.client_software);
		}

	}

}
