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
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsDivide;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

/**
 * Adapter for the list of messages in the chat.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatMessageAdapter extends BaseAdapter implements UpdatableAdapter {

	private static final int TYPE_MESSAGE = 0;
	private static final int TYPE_HINT = 1;
	private static final int TYPE_EMPTY = 2;

	private final Activity activity;
	private String account;
	private String user;
	private boolean isMUC;
	private List<MessageItem> messages;

	/**
	 * Message font appearance.
	 */
	private final int appearanceStyle;

	/**
	 * Divider between header and body.
	 */
	private final String divider;

	/**
	 * Text with extra information.
	 */
	private String hint;

	public ChatMessageAdapter(Activity activity) {
		this.activity = activity;
		messages = Collections.emptyList();
		account = null;
		user = null;
		hint = null;
		appearanceStyle = SettingsManager.chatsAppearanceStyle();
		ChatsDivide chatsDivide = SettingsManager.chatsDivide();
		if (chatsDivide == ChatsDivide.always
				|| (chatsDivide == ChatsDivide.portial && !activity
						.getResources().getBoolean(R.bool.landscape)))
			divider = "\n";
		else
			divider = " ";
	}

	@Override
	public int getCount() {
		return messages.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		if (position < messages.size())
			return messages.get(position);
		else
			return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public int getItemViewType(int position) {
		if (position < messages.size())
			return TYPE_MESSAGE;
		else
			return hint == null ? TYPE_EMPTY : TYPE_HINT;
	}

	private void append(SpannableStringBuilder builder, CharSequence text,
			CharacterStyle span) {
		int start = builder.length();
		builder.append(text);
		builder.setSpan(span, start, start + text.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final int type = getItemViewType(position);
		final View view;
		if (convertView == null) {
			final int resource;
			if (type == TYPE_MESSAGE)
				resource = R.layout.chat_viewer_message;
			else if (type == TYPE_HINT)
				resource = R.layout.chat_viewer_info;
			else if (type == TYPE_EMPTY)
				resource = R.layout.chat_viewer_empty;
			else
				throw new IllegalStateException();
			view = activity.getLayoutInflater()
					.inflate(resource, parent, false);
			if (type == TYPE_MESSAGE)
				((TextView) view.findViewById(R.id.text)).setTextAppearance(
						activity, appearanceStyle);
		} else
			view = convertView;

		if (type == TYPE_EMPTY)
			return view;

		if (type == TYPE_HINT) {
			TextView textView = ((TextView) view.findViewById(R.id.info));
			textView.setText(hint);
			textView.setTextAppearance(activity, R.style.ChatInfo_Warning);
			return view;
		}

		final MessageItem messageItem = (MessageItem) getItem(position);
		final String name;
		final String account = messageItem.getChat().getAccount();
		final String user = messageItem.getChat().getUser();
		final String resource = messageItem.getResource();
		final boolean incoming = messageItem.isIncoming();
		if (isMUC) {
			name = resource;
		} else {
			if (incoming)
				name = RosterManager.getInstance().getName(account, user);
			else
				name = AccountManager.getInstance().getNickName(account);
		}
		if (incoming) {
			if (view.getBackground() == null)
				view.setBackgroundResource(R.drawable.chat_bg);
			view.getBackground().setLevel(
					AccountManager.getInstance().getColorLevel(account));
		} else {
			view.setBackgroundDrawable(null);
		}
		Spannable text = messageItem.getSpannable();
		TextView textView = (TextView) view.findViewById(R.id.text);
		ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
		ChatAction action = messageItem.getAction();
		String time = StringUtils.getSmartTimeText(messageItem.getTimestamp());
		SpannableStringBuilder builder = new SpannableStringBuilder();
		if (action == null) {
			int messageResource = R.drawable.ic_message_delivered;
			if (!incoming) {
				if (messageItem.isError())
					messageResource = R.drawable.ic_message_has_error;
				else if (!messageItem.isSent())
					messageResource = R.drawable.ic_message_not_sent;
				else if (!messageItem.isDelivered())
					messageResource = R.drawable.ic_message_not_delivered;
			}
			append(builder, " ", new ImageSpan(activity, messageResource));
			append(builder, " ", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			append(builder, time, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Time));
			append(builder, " ", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			append(builder, name, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Name));
			append(builder, divider, new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			Date timeStamp = messageItem.getDelayTimestamp();
			if (timeStamp != null) {
				String delay = activity.getString(
						incoming ? R.string.chat_delay : R.string.chat_typed,
						StringUtils.getSmartTimeText(timeStamp));
				append(builder, delay, new TextAppearanceSpan(activity,
						R.style.ChatHeader_Delay));
				append(builder, divider, new TextAppearanceSpan(activity,
						R.style.ChatHeader));
			}
			if (messageItem.isUnencypted()) {
				append(builder,
						activity.getString(R.string.otr_unencrypted_message),
						new TextAppearanceSpan(activity,
								R.style.ChatHeader_Delay));
				append(builder, divider, new TextAppearanceSpan(activity,
						R.style.ChatHeader));
			}
			Emoticons.getSmiledText(activity.getApplication(), text);
			if (messageItem.getTag() == null)
				builder.append(text);
			else
				append(builder, text, new TextAppearanceSpan(activity,
						R.style.ChatRead));
		} else {
			append(builder, time, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Time));
			append(builder, " ", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			text = Emoticons.newSpannable(action.getText(activity, name,
					text.toString()));
			Emoticons.getSmiledText(activity.getApplication(), text);
			append(builder, text, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Delay));
		}
		textView.setText(builder);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		if (SettingsManager.chatsShowAvatars()) {
			avatarView.setVisibility(View.VISIBLE);
			if (!incoming
					|| (isMUC && MUCManager.getInstance()
							.getNickname(account, user)
							.equalsIgnoreCase(resource))) {
				avatarView.setImageDrawable(AvatarManager.getInstance()
						.getAccountAvatar(account));
			} else {
				if (isMUC) {
					if ("".equals(resource)) {
						avatarView.setImageDrawable(AvatarManager.getInstance()
								.getRoomAvatar(user));
					} else {
						avatarView.setImageDrawable(AvatarManager.getInstance()
								.getOccupantAvatar(user + "/" + resource));
					}
				} else {
					avatarView.setImageDrawable(AvatarManager.getInstance()
							.getUserAvatar(user));
				}
			}
			((RelativeLayout.LayoutParams) textView.getLayoutParams()).addRule(
					RelativeLayout.RIGHT_OF, R.id.avatar);
		} else {
			avatarView.setVisibility(View.GONE);
			((RelativeLayout.LayoutParams) textView.getLayoutParams()).addRule(
					RelativeLayout.RIGHT_OF, 0);
		}
		return view;
	}

	public String getAccount() {
		return account;
	}

	public String getUser() {
		return user;
	}

	/**
	 * Changes managed chat.
	 * 
	 * @param account
	 * @param user
	 */
	public void setChat(String account, String user) {
		this.account = account;
		this.user = user;
		this.isMUC = MUCManager.getInstance().hasRoom(account, user);
		onChange();
	}

	@Override
	public void onChange() {
		messages = new ArrayList<MessageItem>(MessageManager.getInstance()
				.getMessages(account, user));
		hint = getHint();
		notifyDataSetChanged();
	}

	/**
	 * @return New hint.
	 */
	private String getHint() {
		AccountItem accountItem = AccountManager.getInstance().getAccount(
				account);
		boolean online;
		if (accountItem == null)
			online = false;
		else
			online = accountItem.getState().isConnected();
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		if (!online) {
			if (abstractContact instanceof RoomContact)
				return activity.getString(R.string.muc_is_unavailable);
			else
				return activity.getString(R.string.account_is_offline);
		} else if (!abstractContact.getStatusMode().isOnline()) {
			if (abstractContact instanceof RoomContact)
				return activity.getString(R.string.muc_is_unavailable);
			else
				return activity.getString(R.string.contact_is_offline,
						abstractContact.getName());
		}
		return null;
	}

	/**
	 * Contact information has been changed. Renews hint and updates data if
	 * necessary.
	 */
	public void updateInfo() {
		String info = getHint();
		if (this.hint == info || (this.hint != null && this.hint.equals(info)))
			return;
		this.hint = info;
		notifyDataSetChanged();
	}

}
