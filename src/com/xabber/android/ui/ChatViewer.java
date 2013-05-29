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
package com.xabber.android.ui;

import java.util.Collection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsHideKeyboard;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.adapter.OnTextChangedListener;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.android.ui.widget.PageSwitcher.OnSelectListener;
import com.xabber.androiddev.R;

/**
 * Chat activity.
 * 
 * Warning: {@link PageSwitcher} is to be removed and related implementation is
 * to be fixed.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatViewer extends ManagedActivity implements
		View.OnClickListener, View.OnKeyListener, OnSelectListener,
		OnChatChangedListener, OnContactChangedListener,
		OnAccountChangedListener, OnEditorActionListener, OnTextChangedListener {

	/**
	 * Attention request.
	 */
	private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";

	private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_ACCOUNT";
	private static final String SAVED_USER = "com.xabber.android.ui.ChatViewer.SAVED_USER";
	private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.ChatViewer.EXIT_ON_SEND";

	private ChatViewerAdapter chatViewerAdapter;
	private PageSwitcher pageSwitcher;

	private String actionWithAccount;
	private String actionWithUser;
	private View actionWithView;

	private boolean exitOnSend;

	private boolean isVisible;

	private boolean skipOnTextChanges;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		Intent intent = getIntent();
		String account = getAccount(intent);
		String user = getUser(intent);
		if (PageSwitcher.LOG)
			LogManager.i(this, "Intent: " + account + ":" + user);
		if (account == null || user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			finish();
			return;
		}
		if (hasAttention(intent))
			AttentionManager.getInstance().removeAccountNotifications(account,
					user);
		actionWithAccount = null;
		actionWithUser = null;
		actionWithView = null;

		setContentView(R.layout.chat_viewer);
		chatViewerAdapter = new ChatViewerAdapter(this, account, user);
		pageSwitcher = (PageSwitcher) findViewById(R.id.switcher);
		pageSwitcher.setAdapter(chatViewerAdapter);
		pageSwitcher.setOnSelectListener(this);

		if (savedInstanceState != null) {
			actionWithAccount = savedInstanceState.getString(SAVED_ACCOUNT);
			actionWithUser = savedInstanceState.getString(SAVED_USER);
			exitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);
		}
		if (actionWithAccount == null)
			actionWithAccount = account;
		if (actionWithUser == null)
			actionWithUser = user;

		selectChat(actionWithAccount, actionWithUser);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnChatChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		chatViewerAdapter.onChange();
		if (actionWithView != null)
			chatViewerAdapter.onChatChange(actionWithView, false);
		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction())) {
			String additional = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (additional != null) {
				intent.removeExtra(Intent.EXTRA_TEXT);
				exitOnSend = true;
				if (actionWithView != null)
					insertText(additional);
			}
		}
		isVisible = true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (PageSwitcher.LOG)
			LogManager.i(this, "onSave: " + actionWithAccount + ":"
					+ actionWithUser);
		outState.putString(SAVED_ACCOUNT, actionWithAccount);
		outState.putString(SAVED_USER, actionWithUser);
		outState.putBoolean(SAVED_EXIT_ON_SEND, exitOnSend);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(OnChatChangedListener.class,
				this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		MessageManager.getInstance().removeVisibleChat();
		pageSwitcher.saveState();
		isVisible = false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (isFinishing())
			return;

		String account = getAccount(intent);
		String user = getUser(intent);
		if (account == null || user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			return;
		}
		if (hasAttention(intent))
			AttentionManager.getInstance().removeAccountNotifications(account,
					user);

		chatViewerAdapter.onChange();
		if (!selectChat(account, user))
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		chatViewerAdapter.onPrepareOptionsMenu(actionWithView, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		pageSwitcher.stopMovement();

		ListView listView = (ListView) actionWithView
				.findViewById(android.R.id.list);
		final MessageItem message = (MessageItem) listView.getAdapter()
				.getItem(info.position);
		if (message != null && message.getAction() != null)
			return;
		if (message.isError()) {
			menu.add(R.string.message_repeat).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							sendMessage(message.getText());
							return true;
						}

					});
		}
		menu.add(R.string.message_quote).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						insertText("> " + message.getText() + "\n");
						return true;
					}

				});
		menu.add(R.string.message_copy).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
								.setText(message.getSpannable());
						return true;
					}

				});
		menu.add(R.string.message_remove).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						MessageManager.getInstance().removeMessage(message);
						chatViewerAdapter.onChatChange(actionWithView, false);
						return true;
					}

				});
	}

	/**
	 * Insert additional text to the input.
	 * 
	 * @param additional
	 */
	private void insertText(String additional) {
		EditText editView = (EditText) actionWithView
				.findViewById(R.id.chat_input);
		String source = editView.getText().toString();
		int selection = editView.getSelectionEnd();
		if (selection == -1)
			selection = source.length();
		else if (selection > source.length())
			selection = source.length();
		String before = source.substring(0, selection);
		String after = source.substring(selection);
		if (before.length() > 0 && !before.endsWith("\n"))
			additional = "\n" + additional;
		editView.setText(before + additional + after);
		editView.setSelection(selection + additional.length());
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.chat_send:
			sendMessage();
			break;
		case R.id.title:
			ListView listView = (ListView) actionWithView
					.findViewById(android.R.id.list);
			int size = listView.getCount();
			if (size > 0)
				listView.setSelection(size - 1);
		default:
			break;
		}
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER
				&& SettingsManager.chatsSendByEnter()) {
			sendMessage();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			close();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	void close() {
		finish();
		if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
			ActivityManager.getInstance().clearStack(false);
			if (!ActivityManager.getInstance().hasContactList(this))
				startActivity(ContactList.createIntent(this));
		}
	}

	@Override
	public void onTextChanged(EditText editText, CharSequence text) {
		if (skipOnTextChanges)
			return;
		ChatStateManager.getInstance().onComposing(actionWithAccount,
				actionWithUser, text);
	}

	@Override
	public void onSelect() {
		BaseEntity contactItem = (BaseEntity) pageSwitcher.getSelectedItem();
		actionWithAccount = contactItem.getAccount();
		actionWithUser = contactItem.getUser();
		if (PageSwitcher.LOG)
			LogManager.i(this, "onSelect: " + actionWithAccount + ":"
					+ actionWithUser);
		actionWithView = pageSwitcher.getSelectedView();
		if (isVisible)
			MessageManager.getInstance().setVisibleChat(actionWithAccount,
					actionWithUser);
		MessageArchiveManager.getInstance().requestHistory(
				actionWithAccount,
				actionWithUser,
				0,
				MessageManager.getInstance()
						.getChat(actionWithAccount, actionWithUser)
						.getRequiredMessageCount());
		NotificationManager.getInstance().removeMessageNotification(
				actionWithAccount, actionWithUser);
	}

	@Override
	public void onUnselect() {
		actionWithAccount = null;
		actionWithUser = null;
		actionWithView = null;
		if (PageSwitcher.LOG)
			LogManager.i(this, "onUnselect");
	}

	private void sendMessage() {
		if (actionWithView == null)
			return;
		EditText editView = (EditText) actionWithView
				.findViewById(R.id.chat_input);
		String text = editView.getText().toString();
		int start = 0;
		int end = text.length();
		while (start < end
				&& (text.charAt(start) == ' ' || text.charAt(start) == '\n'))
			start += 1;
		while (start < end
				&& (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\n'))
			end -= 1;
		text = text.substring(start, end);
		if ("".equals(text))
			return;
		skipOnTextChanges = true;
		editView.setText("");
		skipOnTextChanges = false;
		sendMessage(text);
		if (exitOnSend)
			close();
		if (SettingsManager.chatsHideKeyboard() == ChatsHideKeyboard.always
				|| (getResources().getBoolean(R.bool.landscape) && SettingsManager
						.chatsHideKeyboard() == ChatsHideKeyboard.landscape)) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editView.getWindowToken(), 0);
		}
	}

	private void sendMessage(String text) {
		MessageManager.getInstance().sendMessage(actionWithAccount,
				actionWithUser, text);
		chatViewerAdapter.onChatChange(actionWithView, false);
	}

	@Override
	public void onChatChanged(final String account, final String user,
			final boolean incoming) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && baseEntity.equals(account, user)) {
			chatViewerAdapter.onChatChange(pageSwitcher.getSelectedView(),
					incoming);
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && baseEntity.equals(account, user)) {
			chatViewerAdapter.onChatChange(pageSwitcher.getVisibleView(),
					incoming);
			return;
		}
		// Search for chat in adapter.
		final int count = chatViewerAdapter.getCount();
		for (int index = 0; index < count; index++)
			if (((BaseEntity) chatViewerAdapter.getItem(index)).equals(account,
					user))
				return;
		// New chat.
		chatViewerAdapter.onChange();
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> entities) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && entities.contains(baseEntity)) {
			chatViewerAdapter.onChange();
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && entities.contains(baseEntity)) {
			chatViewerAdapter.onChange();
			return;
		}
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
			chatViewerAdapter.onChange();
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
			chatViewerAdapter.onChange();
			return;
		}
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			sendMessage();
			return true;
		}
		return false;
	}

	private boolean selectChat(String account, String user) {
		for (int position = 0; position < chatViewerAdapter.getCount(); position++)
			if (((BaseEntity) chatViewerAdapter.getItem(position)).equals(
					account, user)) {
				if (PageSwitcher.LOG)
					LogManager.i(this, "setSelection: " + position + ", "
							+ account + ":" + user);
				pageSwitcher.setSelection(position);
				return true;
			}
		if (PageSwitcher.LOG)
			LogManager.i(this, "setSelection: not found, " + account + ":"
					+ user);
		return false;
	}

	public int getChatCount() {
		return chatViewerAdapter.getCount();
	}

	public int getChatPosition(String account, String user) {
		return chatViewerAdapter.getPosition(account, user);
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ChatViewer.class)
				.setAccount(account).setUser(user).build();
	}

	public static Intent createClearTopIntent(Context context, String account,
			String user) {
		Intent intent = createIntent(context, account, user);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	/**
	 * Create intent to send message.
	 * 
	 * Contact list will not be shown on when chat will be closed.
	 * 
	 * @param context
	 * @param account
	 * @param user
	 * @param text
	 *            if <code>null</code> then user will be able to send a number
	 *            of messages. Else only one message can be send.
	 * @return
	 */
	public static Intent createSendIntent(Context context, String account,
			String user, String text) {
		Intent intent = ChatViewer.createIntent(context, account, user);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		return intent;
	}

	public static Intent createAttentionRequestIntent(Context context,
			String account, String user) {
		Intent intent = ChatViewer.createClearTopIntent(context, account, user);
		intent.setAction(ACTION_ATTENTION);
		return intent;
	}

	private static String getAccount(Intent intent) {
		String value = EntityIntentBuilder.getAccount(intent);
		if (value != null)
			return value;
		// Backward compatibility.
		return intent.getStringExtra("com.xabber.android.data.account");
	}

	private static String getUser(Intent intent) {
		String value = EntityIntentBuilder.getUser(intent);
		if (value != null)
			return value;
		// Backward compatibility.
		return intent.getStringExtra("com.xabber.android.data.user");
	}

	private static boolean hasAttention(Intent intent) {
		return ACTION_ATTENTION.equals(intent.getAction());
	}

}
