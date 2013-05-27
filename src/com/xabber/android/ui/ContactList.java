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

import java.util.ArrayList;
import java.util.Collection;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.adapter.AccountConfiguration;
import com.xabber.android.ui.adapter.AccountToggleAdapter;
import com.xabber.android.ui.adapter.ContactListAdapter;
import com.xabber.android.ui.adapter.GroupConfiguration;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment.OnChoosedListener;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.dialog.ContactIntegrationDialogFragment;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.dialog.GroupDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupRenameDialogFragment;
import com.xabber.android.ui.dialog.StartAtBootDialogFragment;
import com.xabber.android.ui.helper.ManagedListActivity;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.uri.XMPPUri;

/**
 * Main application activity.
 * 
 * @author alexander.ivanov
 * 
 */
public class ContactList extends ManagedListActivity implements
		OnContactChangedListener, OnAccountChangedListener,
		OnChatChangedListener, View.OnClickListener, ConfirmDialogListener,
		OnItemClickListener, OnLongClickListener, OnChoosedListener {

	/**
	 * Select contact to be invited to the room was requested.
	 */
	private static final String ACTION_ROOM_INVITE = "com.xabber.android.ui.ContactList.ACTION_ROOM_INVITE";

	private static final long CLOSE_ACTIVITY_AFTER_DELAY = 300;

	private static final String SAVED_ACTION = "com.xabber.android.ui.ContactList.SAVED_ACTION";
	private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.ContactList.SAVED_SEND_TEXT";

	private static final int OPTION_MENU_ADD_CONTACT_ID = 0x02;
	private static final int OPTION_MENU_STATUS_EDITOR_ID = 0x04;
	private static final int OPTION_MENU_PREFERENCE_EDITOR_ID = 0x05;
	private static final int OPTION_MENU_CHAT_LIST_ID = 0x06;
	private static final int OPTION_MENU_JOIN_ROOM_ID = 0x07;
	private static final int OPTION_MENU_EXIT_ID = 0x08;
	private static final int OPTION_MENU_SEARCH_ID = 0x0A;
	private static final int OPTION_MENU_CLOSE_CHATS_ID = 0x0B;

	private static final int CONTEXT_MENU_SHOW_OFFLINE_GROUP_ID = 0x40;

	private static final int DIALOG_CLOSE_APPLICATION_ID = 0x57;

	/**
	 * Adapter for contact list.
	 */
	private ContactListAdapter contactListAdapter;

	/**
	 * Adapter for account list.
	 */
	private AccountToggleAdapter accountToggleAdapter;

	/**
	 * Current action.
	 */
	private String action;

	/**
	 * Dialog related values.
	 */
	private String sendText;

	/**
	 * Title view.
	 */
	private View titleView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())
				|| Intent.ACTION_SEND.equals(getIntent().getAction())
				|| Intent.ACTION_SENDTO.equals(getIntent().getAction())
				|| Intent.ACTION_CREATE_SHORTCUT
						.equals(getIntent().getAction()))
			ActivityManager.getInstance().startNewTask(this);
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		setContentView(R.layout.contact_list);

		titleView = findViewById(android.R.id.title);

		ListView listView = getListView();
		listView.setOnItemClickListener(this);
		listView.setItemsCanFocus(true);

		registerForContextMenu(listView);
		contactListAdapter = new ContactListAdapter(this);
		setListAdapter(contactListAdapter);
		accountToggleAdapter = new AccountToggleAdapter(this, this,
				(LinearLayout) findViewById(R.id.account_list));

		View commonStatusText = findViewById(R.id.common_status_text);
		View commonStatusMode = findViewById(R.id.common_status_mode);

		TypedArray typedArray = obtainStyledAttributes(R.styleable.ContactList);
		ColorStateList textColorPrimary = typedArray
				.getColorStateList(R.styleable.ContactList_textColorPrimaryNoSelected);
		Drawable titleMainBackground = typedArray
				.getDrawable(R.styleable.ContactList_titleMainBackground);
		typedArray.recycle();

		((TextView) commonStatusText).setTextColor(textColorPrimary);
		titleView.setBackgroundDrawable(titleMainBackground);

		commonStatusText.setOnLongClickListener(this);
		commonStatusMode.setOnClickListener(this);
		commonStatusText.setOnClickListener(this);
		titleView.setOnClickListener(this);
		findViewById(R.id.button).setOnClickListener(this);
		findViewById(R.id.back_button).setOnClickListener(this);

		if (savedInstanceState != null) {
			sendText = savedInstanceState.getString(SAVED_SEND_TEXT);
			action = savedInstanceState.getString(SAVED_ACTION);
		} else {
			sendText = null;
			action = getIntent().getAction();
		}
		getIntent().setAction(null);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		action = getIntent().getAction();
		getIntent().setAction(null);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVED_ACTION, action);
		outState.putString(SAVED_SEND_TEXT, sendText);
	}

	/**
	 * Open chat with specified contact.
	 * 
	 * Show dialog to choose account if necessary.
	 * 
	 * @param user
	 * @param text
	 *            can be <code>null</code>.
	 */
	private void openChat(String user, String text) {
		String bareAddress = Jid.getBareAddress(user);
		ArrayList<BaseEntity> entities = new ArrayList<BaseEntity>();
		for (AbstractChat check : MessageManager.getInstance().getChats())
			if (check.isActive() && check.getUser().equals(bareAddress))
				entities.add(check);
		if (entities.size() == 1) {
			openChat(entities.get(0), text);
			return;
		}
		entities.clear();
		for (RosterContact check : RosterManager.getInstance().getContacts())
			if (check.isEnabled() && check.getUser().equals(bareAddress))
				entities.add(check);
		if (entities.size() == 1) {
			openChat(entities.get(0), text);
			return;
		}
		Collection<String> accounts = AccountManager.getInstance()
				.getAccounts();
		if (accounts.isEmpty())
			return;
		if (accounts.size() == 1) {
			openChat(new BaseEntity(accounts.iterator().next(), bareAddress),
					text);
			return;
		}
		AccountChooseDialogFragment.newInstance(bareAddress, text).show(
				getSupportFragmentManager(), "OPEN_WITH_ACCOUNT");
	}

	/**
	 * Open chat with specified contact and enter text to be sent.
	 * 
	 * @param baseEntity
	 * @param text
	 *            can be <code>null</code>.
	 */
	private void openChat(BaseEntity baseEntity, String text) {
		if (text == null)
			startActivity(ChatViewer.createSendIntent(this,
					baseEntity.getAccount(), baseEntity.getUser(), null));
		else
			startActivity(ChatViewer.createSendIntent(this,
					baseEntity.getAccount(), baseEntity.getUser(), text));
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateStatusBar();
		rebuildAccountToggler();

		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnChatChangedListener.class,
				this);
		contactListAdapter.onChange();

		if (ContactList.ACTION_ROOM_INVITE.equals(action)
				|| Intent.ACTION_SEND.equals(action)
				|| Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			if (Intent.ACTION_SEND.equals(action))
				sendText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			Toast.makeText(this, getString(R.string.select_contact),
					Toast.LENGTH_LONG).show();
		} else if (Intent.ACTION_VIEW.equals(action)) {
			action = null;
			Uri data = getIntent().getData();
			if (data != null && "xmpp".equals(data.getScheme())) {
				XMPPUri xmppUri;
				try {
					xmppUri = XMPPUri.parse(data);
				} catch (IllegalArgumentException e) {
					xmppUri = null;
				}
				if (xmppUri != null && "message".equals(xmppUri.getQueryType())) {
					ArrayList<String> texts = xmppUri.getValues("body");
					String text = null;
					if (texts != null && !texts.isEmpty())
						text = texts.get(0);
					openChat(xmppUri.getPath(), text);
				}
			}
		} else if (Intent.ACTION_SENDTO.equals(action)) {
			action = null;
			Uri data = getIntent().getData();
			if (data != null) {
				String path = data.getPath();
				if (path != null && path.startsWith("/"))
					openChat(path.substring(1), null);
			}
		}
		if (Application.getInstance().doNotify()) {
			if (SettingsManager.bootCount() > 2
					&& !SettingsManager.connectionStartAtBoot()
					&& !SettingsManager.startAtBootSuggested())
				StartAtBootDialogFragment.newInstance().show(
						getSupportFragmentManager(), "START_AT_BOOT");
			if (!SettingsManager.contactIntegrationSuggested()
					&& Application.getInstance().isContactsSupported()) {
				if (AccountManager.getInstance().getAllAccounts().isEmpty())
					SettingsManager.setContactIntegrationSuggested();
				else
					ContactIntegrationDialogFragment.newInstance().show(
							getSupportFragmentManager(), "CONTACT_INTEGRATION");
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterListeners();
	}

	private void unregisterListeners() {
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		Application.getInstance().removeUIListener(OnChatChangedListener.class,
				this);
		contactListAdapter.removeRefreshRequests();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, OPTION_MENU_ADD_CONTACT_ID, 0,
				getText(R.string.contact_add)).setIcon(
				R.drawable.ic_menu_invite);
		menu.add(0, OPTION_MENU_CLOSE_CHATS_ID, 0,
				getText(R.string.close_chats)).setIcon(
				R.drawable.ic_menu_end_conversation);
		menu.add(0, OPTION_MENU_PREFERENCE_EDITOR_ID, 0,
				getResources().getText(R.string.preference_editor)).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(0, OPTION_MENU_STATUS_EDITOR_ID, 0,
				getText(R.string.status_editor)).setIcon(
				R.drawable.ic_menu_notifications);
		menu.add(0, OPTION_MENU_EXIT_ID, 0, getText(R.string.exit)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, OPTION_MENU_JOIN_ROOM_ID, 0, getText(R.string.muc_add));
		menu.add(0, OPTION_MENU_SEARCH_ID, 0,
				getText(android.R.string.search_go));
		menu.add(0, OPTION_MENU_CHAT_LIST_ID, 0, getText(R.string.chat_list))
				.setIcon(R.drawable.ic_menu_friendslist);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case OPTION_MENU_ADD_CONTACT_ID:
			startActivity(ContactAdd.createIntent(this));
			return true;
		case OPTION_MENU_STATUS_EDITOR_ID:
			startActivity(StatusEditor.createIntent(this));
			return true;
		case OPTION_MENU_PREFERENCE_EDITOR_ID:
			startActivity(PreferenceEditor.createIntent(this));
			return true;
		case OPTION_MENU_CHAT_LIST_ID:
			startActivity(ChatList.createIntent(this));
			return true;
		case OPTION_MENU_JOIN_ROOM_ID:
			startActivity(MUCEditor.createIntent(this));
			return true;
		case OPTION_MENU_EXIT_ID:
			Application.getInstance().requestToClose();
			showDialog(DIALOG_CLOSE_APPLICATION_ID);
			unregisterListeners();
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					// Close activity if application was not killed yet.
					finish();
				}
			}, CLOSE_ACTIVITY_AFTER_DELAY);
			return true;
		case OPTION_MENU_SEARCH_ID:
			search();
			return true;
		case OPTION_MENU_CLOSE_CHATS_ID:
			for (AbstractChat chat : MessageManager.getInstance()
					.getActiveChats()) {
				MessageManager.getInstance().closeChat(chat.getAccount(),
						chat.getUser());
				NotificationManager.getInstance().removeMessageNotification(
						chat.getAccount(), chat.getUser());
			}
			contactListAdapter.onChange();
			return true;
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		if (view == getListView()) {
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			BaseEntity baseEntity = (BaseEntity) getListView()
					.getItemAtPosition(info.position);
			if (baseEntity == null)
				// Account toggler
				return;
			if (baseEntity instanceof AbstractContact) {
				createContactContextMenu((AbstractContact) baseEntity, menu);
			} else if (baseEntity instanceof AccountConfiguration) {
				createAccountContextMenu(baseEntity.getAccount(), menu);
			} else if (baseEntity instanceof GroupConfiguration) {
				createGroupContextMenu(baseEntity.getAccount(),
						baseEntity.getUser(), menu);
			}
		} else {
			// Account panel
			createAccountContextMenu(accountToggleAdapter.getItemForView(view),
					menu);
		}
	}

	private void createContactContextMenu(AbstractContact abstractContact,
			ContextMenu menu) {
		final String account = abstractContact.getAccount();
		final String user = abstractContact.getUser();
		menu.setHeaderTitle(abstractContact.getName());
		menu.add(R.string.chat_viewer).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						MessageManager.getInstance().openChat(account, user);
						startActivity(ChatViewer.createIntent(ContactList.this,
								account, user));
						return true;
					}
				});
		if (MUCManager.getInstance().hasRoom(account, user)) {
			if (!MUCManager.getInstance().inUse(account, user))
				menu.add(R.string.muc_edit).setIntent(
						MUCEditor.createIntent(this, account, user));
			menu.add(R.string.muc_delete).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							ContactDeleteDialogFragment.newInstance(
									account == GroupManager.NO_ACCOUNT ? null
											: account, user).show(
									getSupportFragmentManager(),
									"CONTACT_DELETE");
							return true;
						}

					});
			if (MUCManager.getInstance().isDisabled(account, user))
				menu.add(R.string.muc_join).setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								MUCManager.getInstance().joinRoom(account,
										user, true);
								return true;
							}

						});
			else
				menu.add(R.string.muc_leave).setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								MUCManager.getInstance().leaveRoom(account,
										user);
								MessageManager.getInstance().closeChat(account,
										user);
								NotificationManager.getInstance()
										.removeMessageNotification(account,
												user);
								contactListAdapter.onChange();
								return true;
							}

						});
		} else {
			menu.add(R.string.contact_viewer).setIntent(
					ContactViewer.createIntent(this, account, user));
			menu.add(R.string.contact_editor).setIntent(
					ContactEditor.createIntent(this, account, user));
			menu.add(R.string.contact_delete).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							ContactDeleteDialogFragment.newInstance(
									account == GroupManager.NO_ACCOUNT ? null
											: account, user).show(
									getSupportFragmentManager(),
									"CONTACT_DELETE");
							return true;
						}

					});
			if (MessageManager.getInstance().hasActiveChat(account, user))
				menu.add(R.string.close_chat).setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								MessageManager.getInstance().closeChat(account,
										user);
								NotificationManager.getInstance()
										.removeMessageNotification(account,
												user);
								contactListAdapter.onChange();
								return true;
							}

						});
			if (abstractContact.getStatusMode() == StatusMode.unsubscribed)
				menu.add(R.string.request_subscription)
						.setOnMenuItemClickListener(
								new MenuItem.OnMenuItemClickListener() {

									@Override
									public boolean onMenuItemClick(MenuItem item) {
										try {
											PresenceManager.getInstance()
													.requestSubscription(
															account, user);
										} catch (NetworkException e) {
											Application.getInstance()
													.onError(e);
										}
										return true;
									}

								});
		}
		if (PresenceManager.getInstance().hasSubscriptionRequest(account, user)) {
			menu.add(R.string.accept_subscription).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							try {
								PresenceManager.getInstance()
										.acceptSubscription(account, user);
							} catch (NetworkException e) {
								Application.getInstance().onError(e);
							}
							startActivity(ContactEditor.createIntent(
									ContactList.this, account, user));
							return true;
						}

					});
			menu.add(R.string.discard_subscription).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							try {
								PresenceManager.getInstance()
										.discardSubscription(account, user);
							} catch (NetworkException e) {
								Application.getInstance().onError(e);
							}
							return true;
						}

					});
		}
	}

	private void createGroupContextMenu(final String account,
			final String group, ContextMenu menu) {
		menu.setHeaderTitle(GroupManager.getInstance().getGroupName(account,
				group));
		if (group != GroupManager.ACTIVE_CHATS && group != GroupManager.IS_ROOM) {
			menu.add(R.string.group_rename).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							GroupRenameDialogFragment.newInstance(
									account == GroupManager.NO_ACCOUNT ? null
											: account,
									group == GroupManager.NO_GROUP ? null
											: group)
									.show(getSupportFragmentManager(),
											"GROUP_RENAME");
							return true;
						}
					});
			if (group != GroupManager.NO_GROUP)
				menu.add(R.string.group_remove).setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								GroupDeleteDialogFragment
										.newInstance(
												account == GroupManager.NO_ACCOUNT ? null
														: account, group).show(
												getSupportFragmentManager(),
												"GROUP_DELETE");
								return true;
							}
						});
		}
		createOfflineModeContextMenu(account, group, menu);
	}

	private void createAccountContextMenu(final String account, ContextMenu menu) {
		menu.setHeaderTitle(AccountManager.getInstance()
				.getVerboseName(account));
		AccountItem accountItem = AccountManager.getInstance().getAccount(
				account);
		ConnectionState state = accountItem.getState();
		if (state == ConnectionState.waiting)
			menu.add(R.string.account_reconnect).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							if (AccountManager.getInstance()
									.getAccount(account).updateConnection(true))
								AccountManager.getInstance().onAccountChanged(
										account);
							return true;
						}

					});
		menu.add(R.string.status_editor).setIntent(
				StatusEditor.createIntent(this, account));
		menu.add(R.string.account_editor).setIntent(
				AccountEditor.createIntent(this, account));
		if (state.isConnected()) {
			menu.add(R.string.contact_viewer).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							String user = AccountManager.getInstance()
									.getAccount(account).getRealJid();
							if (user == null)
								Application.getInstance().onError(
										R.string.NOT_CONNECTED);
							else {
								startActivity(ContactViewer.createIntent(
										ContactList.this, account, user));
							}
							return true;
						}

					});
			menu.add(R.string.contact_add).setIntent(
					ContactAdd.createIntent(this, account));
		}
		if (SettingsManager.contactsShowAccounts())
			createOfflineModeContextMenu(account, null, menu);
	}

	private void createOfflineModeContextMenu(String account, String group,
			ContextMenu menu) {
		SubMenu mapMode = menu.addSubMenu(getResources().getText(
				R.string.show_offline_settings));
		mapMode.setHeaderTitle(R.string.show_offline_settings);
		MenuItem always = mapMode.add(CONTEXT_MENU_SHOW_OFFLINE_GROUP_ID, 0, 0,
				getText(R.string.show_offline_always))
				.setOnMenuItemClickListener(
						new OfflineModeClickListener(account, group,
								ShowOfflineMode.always));
		MenuItem normal = mapMode.add(CONTEXT_MENU_SHOW_OFFLINE_GROUP_ID, 0, 0,
				getText(R.string.show_offline_normal))
				.setOnMenuItemClickListener(
						new OfflineModeClickListener(account, group,
								ShowOfflineMode.normal));
		MenuItem never = mapMode.add(CONTEXT_MENU_SHOW_OFFLINE_GROUP_ID, 0, 0,
				getText(R.string.show_offline_never))
				.setOnMenuItemClickListener(
						new OfflineModeClickListener(account, group,
								ShowOfflineMode.never));
		mapMode.setGroupCheckable(CONTEXT_MENU_SHOW_OFFLINE_GROUP_ID, true,
				true);
		ShowOfflineMode showOfflineMode = GroupManager.getInstance()
				.getShowOfflineMode(account,
						group == null ? GroupManager.IS_ACCOUNT : group);
		if (showOfflineMode == ShowOfflineMode.always)
			always.setChecked(true);
		else if (showOfflineMode == ShowOfflineMode.normal)
			normal.setChecked(true);
		else if (showOfflineMode == ShowOfflineMode.never)
			never.setChecked(true);
		else
			throw new IllegalStateException();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		switch (id) {
		case DIALOG_CLOSE_APPLICATION_ID:
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog
					.setMessage(getString(R.string.application_state_closing));
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			progressDialog.setIndeterminate(true);
			return progressDialog;
		default:
			return null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_SEARCH:
			search();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.common_status_mode:
			startActivity(StatusEditor.createIntent(this));
			break;
		case R.id.button: // Hint button
			switch ((Integer) view.getTag()) {
			case R.string.application_action_no_online:
				SettingsManager.setContactsShowOffline(true);
				contactListAdapter.onChange();
				break;
			case R.string.application_action_no_contacts:
				startActivity(ContactAdd.createIntent(this));
				break;
			case R.string.application_action_waiting:
				ConnectionManager.getInstance().updateConnections(true);
				break;
			case R.string.application_action_offline:
				AccountManager.getInstance().setStatus(StatusMode.available,
						null);
				break;
			case R.string.application_action_disabled:
				startActivity(AccountList.createIntent(this));
				break;
			case R.string.application_action_empty:
				startActivity(AccountAdd.createIntent(this));
				break;
			default:
				break;
			}
			updateStatusBar();
			break;
		case R.id.back_button: // Xabber icon button
		case R.id.common_status_text:
		case android.R.id.title:
			scrollUp();
			break;
		default:
			String account = accountToggleAdapter.getItemForView(view);
			if (account == null) // Check for tap on account in the title
				break;
			ListView listView = getListView();
			if (!SettingsManager.contactsShowAccounts()) {
				if (AccountManager.getInstance().getAccounts().size() < 2) {
					scrollUp();
				} else {
					if (account.equals(AccountManager.getInstance()
							.getSelectedAccount()))
						SettingsManager.setContactsSelectedAccount("");
					else
						SettingsManager.setContactsSelectedAccount(account);
					rebuildAccountToggler();
					contactListAdapter.onChange();
					stopMovement();
				}
			} else {
				long count = listView.getCount();
				for (int position = 0; position < (int) count; position++) {
					BaseEntity baseEntity = (BaseEntity) listView
							.getItemAtPosition(position);
					if (baseEntity != null
							&& baseEntity instanceof AccountConfiguration
							&& baseEntity.getAccount().equals(account)) {
						listView.setSelection(position);
						stopMovement();
						break;
					}
				}
			}
			break;
		}
	}

	/**
	 * Stop fling scrolling.
	 */
	private void stopMovement() {
		getListView().onTouchEvent(
				MotionEvent.obtain(SystemClock.uptimeMillis(),
						SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL,
						0, 0, 0));
	}

	/**
	 * Scroll to the top of contact list.
	 */
	private void scrollUp() {
		ListView listView = getListView();
		if (listView.getCount() > 0)
			listView.setSelection(0);
		stopMovement();
	}

	@Override
	public boolean onLongClick(View view) {
		switch (view.getId()) {
		case R.id.common_status_text:
			startActivity(StatusEditor.createIntent(this));
			return true;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Object object = parent.getAdapter().getItem(position);
		if (object == null) {
			// Account toggler
		} else if (object instanceof AbstractContact) {
			AbstractContact abstractContact = (AbstractContact) object;
			if (ACTION_ROOM_INVITE.equals(action)) {
				action = null;
				Intent intent = getIntent();
				String account = getRoomInviteAccount(intent);
				String user = getRoomInviteUser(intent);
				if (account != null && user != null)
					try {
						MUCManager.getInstance().invite(account, user,
								abstractContact.getUser());
					} catch (NetworkException e) {
						Application.getInstance().onError(e);
					}
				finish();
			} else if (Intent.ACTION_SEND.equals(action)) {
				action = null;
				startActivity(ChatViewer.createSendIntent(this,
						abstractContact.getAccount(),
						abstractContact.getUser(), sendText));
				finish();
			} else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
				Intent intent = new Intent();
				intent.putExtra(
						Intent.EXTRA_SHORTCUT_INTENT,
						ChatViewer.createClearTopIntent(this,
								abstractContact.getAccount(),
								abstractContact.getUser()));
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
						abstractContact.getName());
				Bitmap bitmap;
				if (MUCManager.getInstance()
						.hasRoom(abstractContact.getAccount(),
								abstractContact.getUser()))
					bitmap = AvatarManager.getInstance().getRoomBitmap(
							abstractContact.getUser());
				else
					bitmap = AvatarManager.getInstance().getUserBitmap(
							abstractContact.getUser());
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, AvatarManager
						.getInstance().createShortcutBitmap(bitmap));
				setResult(RESULT_OK, intent);
				finish();
			} else {
				startActivity(ChatViewer
						.createIntent(this, abstractContact.getAccount(),
								abstractContact.getUser()));
			}
		} else if (object instanceof GroupConfiguration) {
			GroupConfiguration groupConfiguration = (GroupConfiguration) object;
			contactListAdapter.setExpanded(groupConfiguration.getAccount(),
					groupConfiguration.getUser(),
					!groupConfiguration.isExpanded());
		}
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> addresses) {
		contactListAdapter.refreshRequest();
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		accountToggleAdapter.onChange();
		contactListAdapter.refreshRequest();
	}

	@Override
	public void onChatChanged(String account, String user, boolean incoming) {
		if (incoming)
			contactListAdapter.refreshRequest();
	}

	@Override
	public void onChoosed(String account, String user, String text) {
		openChat(new BaseEntity(account, user), text);
	}

	@Override
	public void onAccept(DialogBuilder dialogBuilder) {
	}

	@Override
	public void onDecline(DialogBuilder dialogBuilder) {
	}

	@Override
	public void onCancel(DialogBuilder dialogBuilder) {
	}

	private void updateStatusBar() {
		String statusText = SettingsManager.statusText();
		StatusMode statusMode = SettingsManager.statusMode();
		if ("".equals(statusText))
			statusText = getString(statusMode.getStringID());
		((TextView) findViewById(R.id.common_status_text)).setText(statusText);
		((ImageView) findViewById(R.id.common_status_mode))
				.setImageLevel(statusMode.getStatusLevel());
	}

	private void rebuildAccountToggler() {
		updateStatusBar();
		accountToggleAdapter.rebuild();
		if (SettingsManager.contactsShowPanel()
				&& accountToggleAdapter.getCount() > 0)
			titleView.setVisibility(View.VISIBLE);
		else
			titleView.setVisibility(View.GONE);
	}

	/**
	 * Show search dialog.
	 */
	private void search() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null)
			inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,
					0);
	}

	private class OfflineModeClickListener implements
			MenuItem.OnMenuItemClickListener {

		private final String account;
		private final String group;
		private final ShowOfflineMode mode;

		public OfflineModeClickListener(String account, String group,
				ShowOfflineMode mode) {
			super();
			this.account = account;
			this.group = group;
			this.mode = mode;
		}

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			GroupManager.getInstance().setShowOfflineMode(account,
					group == null ? GroupManager.IS_ACCOUNT : group, mode);
			contactListAdapter.onChange();
			return true;
		}

	}

	public static Intent createPersistentIntent(Context context) {
		Intent intent = new Intent(context, ContactList.class);
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.LAUNCHER");
		intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, ContactList.class);
	}

	public static Intent createRoomInviteIntent(Context context,
			String account, String room) {
		Intent intent = new EntityIntentBuilder(context, ContactList.class)
				.setAccount(account).setUser(room).build();
		intent.setAction(ACTION_ROOM_INVITE);
		return intent;
	}

	private static String getRoomInviteAccount(Intent intent) {
		return EntityIntentBuilder.getAccount(intent);
	}

	private static String getRoomInviteUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
