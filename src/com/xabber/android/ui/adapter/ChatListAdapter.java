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
import android.widget.Toast;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ChatList;
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.androiddev.R;

/**
 * Adapter for {@link ChatList}.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatListAdapter extends BaseAdapter implements UpdatableAdapter {

	private final Activity activity;

	private final ArrayList<AbstractChat> abstractChats;

	private final AbstractAvatarInflaterHelper avatarInflaterHelper;

	public ChatListAdapter(Activity activity) {
		this.activity = activity;
		abstractChats = new ArrayList<AbstractChat>();
		avatarInflaterHelper = AbstractAvatarInflaterHelper
				.createAbstractContactInflaterHelper();
	}

	@Override
	public void onChange() {
		abstractChats.clear();
		abstractChats.addAll(MessageManager.getInstance().getActiveChats());
		Collections.sort(abstractChats, ChatComparator.CHAT_COMPARATOR);
		if (abstractChats.size() == 0) {
			Toast.makeText(activity, R.string.chat_list_is_empty,
					Toast.LENGTH_LONG).show();
			activity.finish();
			return;
		}
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return abstractChats.size();
	}

	@Override
	public Object getItem(int position) {
		return abstractChats.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.chat_list_item, parent, false);
		} else {
			view = convertView;
		}
		final AbstractChat abstractChat = (AbstractChat) getItem(position);
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(abstractChat.getAccount(),
						abstractChat.getUser());
		final ImageView colorView = (ImageView) view.findViewById(R.id.color);
		final ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
		final TextView nameView = (TextView) view.findViewById(R.id.name);
		final TextView textView = (TextView) view.findViewById(R.id.text);
		colorView.setImageLevel(abstractContact.getColorLevel());
		avatarView.setImageDrawable(abstractContact.getAvatar());
		avatarInflaterHelper.updateAvatar(avatarView, abstractContact);
		nameView.setText(abstractContact.getName());
		String statusText = MessageManager.getInstance().getLastText(
				abstractContact.getAccount(), abstractContact.getUser());
		textView.setText(statusText);
		boolean newMessages = NotificationManager.getInstance()
				.getNotificationMessageCount(abstractChat.getAccount(),
						abstractChat.getUser()) > 0;
		textView.setTextAppearance(activity,
				newMessages ? R.style.ChatList_Notification
						: R.style.ChatList_Normal);
		return view;
	}

}
