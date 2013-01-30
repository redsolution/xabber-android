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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

public class ContactEditor extends GroupListActivity implements
		OnContactChangedListener, AdapterView.OnItemClickListener,
		OnAccountChangedListener {

	private String account;
	private String user;

	@Override
	protected void onInflate(Bundle savedInstanceState) {
		setContentView(R.layout.contact_editor);

		Intent intent = getIntent();
		account = ContactEditor.getAccount(intent);
		user = ContactEditor.getUser(intent);
		if (AccountManager.getInstance().getAccount(account) == null
				|| user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			finish();
			return;
		}
	}

	@Override
	Collection<String> getInitialGroups() {
		return RosterManager.getInstance().getGroups(account);
	}

	@Override
	Collection<String> getInitialSelected() {
		return RosterManager.getInstance().getGroups(account, user);
	}

	@Override
	protected void onResume() {
		super.onResume();
		((EditText) findViewById(R.id.contact_name)).setText(RosterManager
				.getInstance().getName(account, user));
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		update();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		try {
			String name = ((EditText) findViewById(R.id.contact_name))
					.getText().toString();
			RosterManager.getInstance().setNameAndGroup(account, user, name,
					getSelected());
		} catch (NetworkException e) {
			Application.getInstance().onError(e);
		}
	}

	private void update() {
		AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		ContactTitleInflater.updateTitle(findViewById(R.id.title), this,
				abstractContact);
		((TextView) findViewById(R.id.name)).setText(getString(
				R.string.contact_editor_title, abstractContact.getName()));
		((TextView) findViewById(R.id.status_text)).setText(user);
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> entities) {
		String thisBareAddress = Jid.getBareAddress(user);
		for (BaseEntity entity : entities)
			if (entity.equals(account, thisBareAddress)) {
				update();
				break;
			}
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		if (accounts.contains(account))
			update();
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		Intent intent = new EntityIntentBuilder(context, ContactEditor.class)
				.setAccount(account).setUser(user).build();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		return intent;
	}

	private static String getAccount(Intent intent) {
		return EntityIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
