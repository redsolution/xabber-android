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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ContactListFragment.OnContactClickListener;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment.OnChoosedListener;
import com.xabber.android.ui.dialog.ContactIntegrationDialogFragment;
import com.xabber.android.ui.dialog.StartAtBootDialogFragment;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.uri.XMPPUri;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Main application activity.
 * 
 * @author alexander.ivanov
 * 
 */
public class ContactList extends ManagedActivity implements OnChoosedListener, OnContactClickListener {

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

	private static final int DIALOG_CLOSE_APPLICATION_ID = 0x57;

	private static final String CONTACT_LIST_TAG = "CONTACT_LIST";

	/**
	 * Current action.
	 */
	private String action;

	/**
	 * Dialog related values.
	 */
	private String sendText;

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

		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.add(R.id.container, new ContactListFragment(),
				CONTACT_LIST_TAG);
		fragmentTransaction.commit();

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
        switch (action) {
            case ContactList.ACTION_ROOM_INVITE:
            case Intent.ACTION_SEND:
            case Intent.ACTION_CREATE_SHORTCUT:
                if (Intent.ACTION_SEND.equals(action))
                    sendText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                Toast.makeText(this, getString(R.string.select_contact),
                        Toast.LENGTH_LONG).show();
                break;
            case Intent.ACTION_VIEW: {
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
                break;
            }
            case Intent.ACTION_SENDTO: {
                action = null;
                Uri data = getIntent().getData();
                if (data != null) {
                    String path = data.getPath();
                    if (path != null && path.startsWith("/"))
                        openChat(path.substring(1), null);
                }
                break;
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
			getContactListFragment().unregisterListeners();
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
			getContactListFragment().getAdapter().onChange();
			return true;
		}
		return false;
	}

	private ContactListFragment getContactListFragment() {
		return (ContactListFragment) getSupportFragmentManager()
				.findFragmentByTag(CONTACT_LIST_TAG);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
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
	public void onContactClick(AbstractContact abstractContact) {
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
					abstractContact.getAccount(), abstractContact.getUser(),
					sendText));
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
			if (MUCManager.getInstance().hasRoom(abstractContact.getAccount(),
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
			startActivity(ChatViewer.createIntent(this,
					abstractContact.getAccount(), abstractContact.getUser()));
		}
	}

	@Override
	public void onChoosed(String account, String user, String text) {
		openChat(new BaseEntity(account, user), text);
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
