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

import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.xmpp.address.Jid;

/**
 * Adapter for the list of chat pages.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatViewerAdapter extends BaseAdapter implements SaveStateAdapter,
		UpdatableAdapter {

	private final FragmentActivity activity;

	/**
	 * Intent sent while opening chat activity.
	 */
	private final AbstractChat intent;

	/**
	 * Position to insert intent.
	 */
	private final int intentPosition;

	private ArrayList<AbstractChat> activeChats;

	public ChatViewerAdapter(FragmentActivity activity, String account,
			String user) {
		this.activity = activity;
		activeChats = new ArrayList<AbstractChat>();
		intent = MessageManager.getInstance().getOrCreateChat(account,
				Jid.getBareAddress(user));
		Collection<? extends BaseEntity> activeChats = MessageManager
				.getInstance().getActiveChats();
		if (activeChats.contains(intent))
			intentPosition = -1;
		else
			intentPosition = activeChats.size();
		onChange();
	}

	@Override
	public int getCount() {
		return activeChats.size();
	}

	@Override
	public Object getItem(int position) {
		return activeChats.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		final AbstractChat chat = (AbstractChat) getItem(position);
		ChatViewerFragment fragment;
		if (convertView == null) {
			fragment = new ChatViewerFragment(activity);
			view = fragment.getView();
			view.setTag(fragment);
		} else {
			view = convertView;
			fragment = (ChatViewerFragment) view.getTag();
		}
		fragment.setChat(chat);
		return view;
	}

	@Override
	public void saveState(View view) {
		((ChatViewerFragment) view.getTag()).saveState();
	}

	@Override
	public void hidePages(View view) {
		((ChatViewerFragment) view.getTag()).hidePages();
	}

	@Override
	public void showPages(View view) {
		((ChatViewerFragment) view.getTag()).showPages();
	}

	/**
	 * Must be called on changes in chat (message sent, received, etc.).
	 */
	public void onChatChange(View view, boolean incomingMessage) {
		((ChatViewerFragment) view.getTag()).onChatChange(incomingMessage);
	}

	/**
	 * Must be called on changes in chat (message sent, received, etc.).
	 */
	public void onPrepareOptionsMenu(View view, Menu menu) {
		((ChatViewerFragment) view.getTag()).onPrepareOptionsMenu(menu);
	}

	@Override
	public void onChange() {
		activeChats = new ArrayList<AbstractChat>(MessageManager.getInstance()
				.getActiveChats());
		if (intentPosition != -1) {
			int index = activeChats.indexOf(intent);
			AbstractChat chat;
			if (index == -1)
				chat = intent;
			else
				chat = activeChats.remove(index);
			activeChats.add(Math.min(intentPosition, activeChats.size()), chat);
		}
		notifyDataSetChanged();
	}

	public int getPosition(String account, String user) {
		for (int position = 0; position < activeChats.size(); position++)
			if (activeChats.get(position).equals(account, user))
				return position;
		return -1;
	}

}
