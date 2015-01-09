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

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

/**
 * Adapter for the list of chat pages.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatViewerAdapter extends BaseAdapter implements SaveStateAdapter,
		UpdatableAdapter {

	private final Activity activity;

	/**
	 * Intent sent while opening chat activity.
	 */
	private final AbstractChat intent;

	/**
	 * Position to insert intent.
	 */
	private final int intentPosition;

	private ArrayList<AbstractChat> activeChats;

	/**
	 * Listener for click on title bar and send button.
	 */
	private OnClickListener onClickListener;

	/**
	 * Listener for key press in edit view.
	 */
	private OnKeyListener onKeyListener;

	/**
	 * Listener for actions in edit view.
	 */
	private OnEditorActionListener onEditorActionListener;

	/**
	 * Listener for context menu in message list.
	 */
	private OnCreateContextMenuListener onCreateContextMenuListener;

	/**
	 * Listen for text to be changed.
	 */
	private OnTextChangedListener onTextChangedListener;

	private final AbstractAvatarInflaterHelper avatarInflaterHelper;

	private final Animation shake;

	public ChatViewerAdapter(Activity activity, String account, String user) {
		this.activity = activity;
		avatarInflaterHelper = AbstractAvatarInflaterHelper
				.createAbstractContactInflaterHelper();
		activeChats = new ArrayList<AbstractChat>();
		intent = MessageManager.getInstance().getOrCreateChat(account,
				Jid.getBareAddress(user));
		Collection<? extends BaseEntity> activeChats = MessageManager
				.getInstance().getActiveChats();
		if (activeChats.contains(intent))
			intentPosition = -1;
		else
			intentPosition = activeChats.size();
		onClickListener = null;
		onKeyListener = null;
		onEditorActionListener = null;
		onCreateContextMenuListener = null;
		onTextChangedListener = null;
		shake = AnimationUtils.loadAnimation(activity, R.anim.shake);
		onChange();
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	public OnKeyListener getOnKeyListener() {
		return onKeyListener;
	}

	public void setOnKeyListener(OnKeyListener onKeyListener) {
		this.onKeyListener = onKeyListener;
	}

	public OnEditorActionListener getOnEditorActionListener() {
		return onEditorActionListener;
	}

	public void setOnEditorActionListener(
			OnEditorActionListener onEditorActionListener) {
		this.onEditorActionListener = onEditorActionListener;
	}

	public OnCreateContextMenuListener getOnCreateContextMenuListener() {
		return onCreateContextMenuListener;
	}

	public void setOnCreateContextMenuListener(
			OnCreateContextMenuListener onCreateContextMenuListener) {
		this.onCreateContextMenuListener = onCreateContextMenuListener;
	}

	public OnTextChangedListener getOnTextChangedListener() {
		return onTextChangedListener;
	}

	public void setOnTextChangedListener(
			OnTextChangedListener onTextChangedListener) {
		this.onTextChangedListener = onTextChangedListener;
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
		final ChatViewHolder chatViewHolder;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.chat_viewer_item, parent, false);
			ChatMessageAdapter chatMessageAdapter = new ChatMessageAdapter(
					activity);
			chatViewHolder = new ChatViewHolder(view, chatMessageAdapter);
			chatViewHolder.list.setAdapter(chatViewHolder.chatMessageAdapter);
			chatViewHolder.send.setOnClickListener(onClickListener);
			chatViewHolder.title.setOnClickListener(onClickListener);
			chatViewHolder.input.setOnKeyListener(onKeyListener);
			chatViewHolder.input
					.setOnEditorActionListener(onEditorActionListener);
			chatViewHolder.input.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (onTextChangedListener != null)
						onTextChangedListener.onTextChanged(
								chatViewHolder.input, s);
				}

			});
			chatViewHolder.list
					.setOnCreateContextMenuListener(onCreateContextMenuListener);
			view.setTag(chatViewHolder);
		} else {
			view = convertView;
			chatViewHolder = (ChatViewHolder) view.getTag();
		}
		final String account = chat.getAccount();
		final String user = chat.getUser();
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);

		if (chat.equals(chatViewHolder.chatMessageAdapter.getAccount(),
				chatViewHolder.chatMessageAdapter.getUser())) {
			chatViewHolder.chatMessageAdapter.updateInfo();
		} else {
			if (chatViewHolder.chatMessageAdapter.getAccount() != null
					&& chatViewHolder.chatMessageAdapter.getUser() != null)
				saveState(view);
			if (PageSwitcher.LOG)
				LogManager.i(this, "Load " + view + " for "
						+ chatViewHolder.chatMessageAdapter.getUser() + " in "
						+ chatViewHolder.chatMessageAdapter.getAccount());
			OnTextChangedListener temp = onTextChangedListener;
			onTextChangedListener = null;
			chatViewHolder.input.setText(ChatManager.getInstance()
					.getTypedMessage(account, user));
			chatViewHolder.input.setSelection(ChatManager.getInstance()
					.getSelectionStart(account, user), ChatManager
					.getInstance().getSelectionEnd(account, user));
			onTextChangedListener = temp;
			chatViewHolder.chatMessageAdapter.setChat(account, user);
			chatViewHolder.list.setAdapter(chatViewHolder.list.getAdapter());
		}

		chatViewHolder.page.setText(activity.getString(R.string.chat_page,
				position + 1, getCount()));
		ContactTitleInflater.updateTitle(chatViewHolder.title, activity, abstractContact);
		avatarInflaterHelper.updateAvatar(chatViewHolder.avatar,
				abstractContact);
		SecurityLevel securityLevel = OTRManager.getInstance()
				.getSecurityLevel(chat.getAccount(), chat.getUser());
		SecurityOtrMode securityOtrMode = SettingsManager.securityOtrMode();
		if (securityLevel == SecurityLevel.plain
				&& (securityOtrMode == SecurityOtrMode.disabled || securityOtrMode == SecurityOtrMode.manual)) {
			chatViewHolder.security.setVisibility(View.GONE);
		} else {
			chatViewHolder.security.setVisibility(View.VISIBLE);
			chatViewHolder.security
					.setImageLevel(securityLevel.getImageLevel());
		}
		return view;
	}

	@Override
	public void saveState(View view) {
		ChatViewHolder chatViewHolder = (ChatViewHolder) view.getTag();
		if (PageSwitcher.LOG)
			LogManager.i(this, "Save " + view + " for "
					+ chatViewHolder.chatMessageAdapter.getUser() + " in "
					+ chatViewHolder.chatMessageAdapter.getAccount());
		ChatManager.getInstance().setTyped(
				chatViewHolder.chatMessageAdapter.getAccount(),
				chatViewHolder.chatMessageAdapter.getUser(),
				chatViewHolder.input.getText().toString(),
				chatViewHolder.input.getSelectionStart(),
				chatViewHolder.input.getSelectionEnd());
	}

	/**
	 * Must be called on changes in chat (message sent, received, etc.).
	 */
	public void onChatChange(View view, boolean incomingMessage) {
		ChatViewHolder holder = (ChatViewHolder) view.getTag();
		if (incomingMessage)
			holder.nameHolder.startAnimation(shake);
		holder.chatMessageAdapter.onChange();
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

	private static class ChatViewHolder {

		final TextView page;
		final View title;
		final View nameHolder;
		final ImageView avatar;
		final ImageView security;
		final View send;
		final EditText input;
		final ListView list;
		final ChatMessageAdapter chatMessageAdapter;

		public ChatViewHolder(View view, ChatMessageAdapter chatMessageAdapter) {
			page = (TextView) view.findViewById(R.id.chat_page);
			title = view.findViewById(R.id.title);
			nameHolder = title.findViewById(R.id.name_holder);
			avatar = (ImageView) title.findViewById(R.id.avatar);
			security = (ImageView) title.findViewById(R.id.security);
			send = view.findViewById(R.id.chat_send);
			input = (EditText) view.findViewById(R.id.chat_input);
			list = (ListView) view.findViewById(android.R.id.list);
			this.chatMessageAdapter = chatMessageAdapter;
		}

	}

}
