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
import android.view.ContextMenu;
import android.view.MenuItem;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.ui.adapter.AccountListAdapter;
import com.xabber.android.ui.adapter.BaseListEditorAdapter;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.helper.BaseListEditor;
import com.xabber.androiddev.R;

public class AccountList extends BaseListEditor<String> implements
		OnAccountChangedListener {

	private static final int CONTEXT_MENU_VIEW_ACCOUNT_ID = 0x20;
	private static final int CONTEXT_MENU_STATUS_EDITOR_ID = 0x30;

	@Override
	protected int getAddTextResourceId() {
		return R.string.account_add;
	}

	@Override
	protected Intent getAddIntent() {
		return AccountAdd.createIntent(this);
	}

	@Override
	protected Intent getEditIntent(String actionWith) {
		return AccountEditor.createIntent(this, actionWith);
	}

	@Override
	protected int getRemoveTextResourceId() {
		return R.string.account_delete;
	}

	@Override
	protected String getRemoveConfirmation(String actionWith) {
		return getString(R.string.account_delete_confirm, AccountManager
				.getInstance().getVerboseName(actionWith));
	}

	@Override
	protected void removeItem(String actionWith) {
		AccountManager.getInstance().removeAccount(actionWith);
	}

	@Override
	protected BaseListEditorAdapter<String> createListAdapter() {
		return new AccountListAdapter(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
	}

	@Override
	protected void onCreateContextMenu(ContextMenu menu, String actionWith) {
		final AccountItem accountItem = AccountManager.getInstance()
				.getAccount(actionWith);
		menu.setHeaderTitle(AccountManager.getInstance().getVerboseName(
				actionWith));
		if (accountItem.isEnabled()) {
			menu.add(0, CONTEXT_MENU_STATUS_EDITOR_ID, 0, getResources()
					.getText(R.string.status_editor));
		}
		menu.add(0, CONTEXT_MENU_VIEW_ACCOUNT_ID, 0,
				getString(R.string.account_editor));
		super.onCreateContextMenu(menu, actionWith);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item))
			return true;
		if (item.getItemId() == CONTEXT_MENU_VIEW_ACCOUNT_ID) {
			startActivity(getEditIntent(getActionWith()));
			return true;
		} else if (item.getItemId() == CONTEXT_MENU_STATUS_EDITOR_ID) {
			startActivity(StatusEditor.createIntent(this, getActionWith()));
			return true;
		}
		return false;
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		((UpdatableAdapter) getListAdapter()).onChange();
	}

	@Override
	protected String getSavedValue(Bundle bundle, String key) {
		return bundle.getString(key);
	}

	@Override
	protected void putSavedValue(Bundle bundle, String key, String actionWith) {
		bundle.putString(key, actionWith);
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, AccountList.class);
	}

}
