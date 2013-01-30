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
import java.util.Collections;
import java.util.HashSet;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.SubscriptionRequest;
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.android.ui.dialog.ConfirmDialogBuilder;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.androiddev.R;

public class ContactAdd extends GroupListActivity implements
		View.OnClickListener, ConfirmDialogListener, OnItemSelectedListener {

	/**
	 * Action for subscription request to be show.
	 * 
	 * Clear action on dialog dismiss.
	 */
	private static final String ACTION_SUBSCRIPTION_REQUEST = "com.xabber.android.data.SUBSCRIPTION_REQUEST";

	private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ContactAdd.SAVED_ACCOUNT";
	private static final String SAVED_USER = "com.xabber.android.ui.ContactAdd.SAVED_USER";
	private static final String SAVED_NAME = "com.xabber.android.ui.ContactAdd.SAVED_NAME";

	private static final int DIALOG_SUBSCRIPTION_REQUEST_ID = 0x20;

	private String account;
	private String user;

	private SubscriptionRequest subscriptionRequest;

	/**
	 * Views
	 */
	private Spinner accountView;
	private EditText userView;
	private EditText nameView;

	@Override
	protected void onInflate(Bundle savedInstanceState) {
		setContentView(R.layout.contact_add);

		ListView listView = getListView();
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.contact_add_header, listView,
				false);
		listView.addHeaderView(view, null, false);

		accountView = (Spinner) view.findViewById(R.id.contact_account);
		accountView.setAdapter(new AccountChooseAdapter(this));
		accountView.setOnItemSelectedListener(this);
		userView = (EditText) view.findViewById(R.id.contact_user);
		nameView = (EditText) view.findViewById(R.id.contact_name);
		((Button) view.findViewById(R.id.ok)).setOnClickListener(this);

		String name;
		Intent intent = getIntent();
		if (savedInstanceState != null) {
			account = savedInstanceState.getString(SAVED_ACCOUNT);
			user = savedInstanceState.getString(SAVED_USER);
			name = savedInstanceState.getString(SAVED_NAME);
		} else {
			account = getAccount(intent);
			user = getUser(intent);
			if (account == null || user == null)
				name = null;
			else {
				name = RosterManager.getInstance().getName(account, user);
				if (user.equals(name))
					name = null;
			}
		}
		if (account == null) {
			Collection<String> accounts = AccountManager.getInstance()
					.getAccounts();
			if (accounts.size() == 1)
				account = accounts.iterator().next();
		}
		if (account != null) {
			for (int position = 0; position < accountView.getCount(); position++)
				if (account.equals(accountView.getItemAtPosition(position))) {
					accountView.setSelection(position);
					break;
				}
		}
		if (user != null)
			userView.setText(user);
		if (name != null)
			nameView.setText(name);
		if (ACTION_SUBSCRIPTION_REQUEST.equals(intent.getAction())) {
			subscriptionRequest = PresenceManager.getInstance()
					.getSubscriptionRequest(account, user);
			if (subscriptionRequest == null) {
				Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
				finish();
				return;
			}
		} else {
			subscriptionRequest = null;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVED_ACCOUNT,
				(String) accountView.getSelectedItem());
		outState.putString(SAVED_USER, userView.getText().toString());
		outState.putString(SAVED_NAME, nameView.getText().toString());
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (subscriptionRequest != null)
			showDialog(DIALOG_SUBSCRIPTION_REQUEST_ID);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.ok:
			String user = userView.getText().toString();
			if ("".equals(user)) {
				Toast.makeText(this, getString(R.string.EMPTY_USER_NAME),
						Toast.LENGTH_LONG).show();
				return;
			}
			String account = (String) accountView.getSelectedItem();
			if (account == null) {
				Toast.makeText(this, getString(R.string.EMPTY_ACCOUNT),
						Toast.LENGTH_LONG).show();
				return;
			}
			try {
				RosterManager.getInstance().createContact(account, user,
						nameView.getText().toString(), getSelected());
				PresenceManager.getInstance()
						.requestSubscription(account, user);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
				finish();
				return;
			}
			MessageManager.getInstance().openChat(account, user);
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		if (dialog != null)
			return dialog;
		switch (id) {
		case DIALOG_SUBSCRIPTION_REQUEST_ID:
			return new ConfirmDialogBuilder(this,
					DIALOG_SUBSCRIPTION_REQUEST_ID, this).setMessage(
					subscriptionRequest.getConfirmation()).create();
		default:
			return null;
		}
	}

	@Override
	public void onAccept(DialogBuilder dialogBuilder) {
		super.onAccept(dialogBuilder);
		switch (dialogBuilder.getDialogId()) {
		case DIALOG_SUBSCRIPTION_REQUEST_ID:
			try {
				PresenceManager.getInstance().acceptSubscription(
						subscriptionRequest.getAccount(),
						subscriptionRequest.getUser());
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			getIntent().setAction(null);
			break;
		}
	}

	@Override
	public void onDecline(DialogBuilder dialogBuilder) {
		super.onDecline(dialogBuilder);
		switch (dialogBuilder.getDialogId()) {
		case DIALOG_SUBSCRIPTION_REQUEST_ID:
			try {
				PresenceManager.getInstance().discardSubscription(
						subscriptionRequest.getAccount(),
						subscriptionRequest.getUser());
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			finish();
			break;
		}
	}

	@Override
	public void onCancel(DialogBuilder dialogBuilder) {
		super.onCancel(dialogBuilder);
		switch (dialogBuilder.getDialogId()) {
		case DIALOG_SUBSCRIPTION_REQUEST_ID:
			finish();
			break;
		}
	}

	@Override
	Collection<String> getInitialGroups() {
		String account = (String) accountView.getSelectedItem();
		if (account == null)
			return Collections.emptyList();
		return RosterManager.getInstance().getGroups(account);
	}

	@Override
	Collection<String> getInitialSelected() {
		return Collections.emptyList();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		String account = (String) accountView.getSelectedItem();
		if (account == null) {
			onNothingSelected(parent);
		} else {
			HashSet<String> groups = new HashSet<String>(RosterManager
					.getInstance().getGroups(account));
			groups.addAll(getSelected());
			setGroups(groups, getSelected());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		setGroups(getSelected(), getSelected());
	}

	public static Intent createIntent(Context context) {
		return createIntent(context, null);
	}

	private static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ContactAdd.class)
				.setAccount(account).setUser(user).build();
	}

	public static Intent createIntent(Context context, String account) {
		return createIntent(context, account, null);
	}

	public static Intent createSubscriptionIntent(Context context,
			String account, String user) {
		Intent intent = createIntent(context, account, user);
		intent.setAction(ACTION_SUBSCRIPTION_REQUEST);
		return intent;
	}

	private static String getAccount(Intent intent) {
		return EntityIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
