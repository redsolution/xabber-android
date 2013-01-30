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
import android.content.res.TypedArray;
import android.view.View;

import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

/**
 * Inflate view with contact's last message or status text as well as active
 * chat badge.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatContactInflater extends ClientContactInflater {

	/**
	 * Name's normal color.
	 */
	private final int textColorPrimary;

	/**
	 * Status's normal color.
	 */
	private final int textColorSecondary;

	public ChatContactInflater(Activity activity) {
		super(activity);
		TypedArray typedArray;
		typedArray = activity.getTheme().obtainStyledAttributes(
				new int[] { android.R.attr.textColorPrimary,
						android.R.attr.textColorSecondary, });
		textColorPrimary = typedArray.getColor(0, 0);
		textColorSecondary = typedArray.getColor(1, 0);
		typedArray.recycle();
	}

	@Override
	ViewHolder createViewHolder(int position, View view) {
		return new ViewHolder(view);
	}

	@Override
	String getStatusText(AbstractContact abstractContact) {
		if (MessageManager.getInstance().hasActiveChat(
				abstractContact.getAccount(), abstractContact.getUser()))
			return MessageManager.getInstance().getLastText(
					abstractContact.getAccount(), abstractContact.getUser());
		else
			return super.getStatusText(abstractContact);
	}

	@Override
	public void getView(View view, AbstractContact abstractContact) {
		super.getView(view, abstractContact);
		final ViewHolder contactViewHolder = (ViewHolder) view.getTag();
		if (MessageManager.getInstance().hasActiveChat(
				abstractContact.getAccount(), abstractContact.getUser())) {
			contactViewHolder.panel
					.setBackgroundResource(R.drawable.active_chat);
			contactViewHolder.name.setTextColor(activity.getResources()
					.getColor(android.R.color.primary_text_light));
			contactViewHolder.status.setTextColor(activity.getResources()
					.getColor(android.R.color.secondary_text_light));
		} else {
			contactViewHolder.panel.setBackgroundDrawable(null);
			contactViewHolder.name.setTextColor(textColorPrimary);
			contactViewHolder.status.setTextColor(textColorSecondary);
		}
	}

	static class ViewHolder extends ClientContactInflater.ViewHolder {

		final View panel;

		public ViewHolder(View view) {
			super(view);
			panel = view.findViewById(R.id.panel);
		}

	}

}
