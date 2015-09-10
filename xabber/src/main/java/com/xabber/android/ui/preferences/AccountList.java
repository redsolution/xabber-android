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
package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.ui.AccountAdd;
import com.xabber.android.ui.StatusEditor;
import com.xabber.android.ui.adapter.AccountListAdapter;
import com.xabber.android.ui.adapter.BaseListEditorAdapter;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;

import java.util.Collection;

public class AccountList extends BaseListEditor<String> implements OnAccountChangedListener {

    private static final int CONTEXT_MENU_VIEW_ACCOUNT_ID = 0x20;
    private static final int CONTEXT_MENU_STATUS_EDITOR_ID = 0x30;

    public static Intent createIntent(Context context) {
        return new Intent(context, AccountList.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle(PreferenceSummaryHelper.getPreferenceTitle(getString(R.string.preference_accounts)));
    }

    @Override
    protected int getOptionsMenuId() {
        return R.menu.add_account;
    }

    @Override
    protected int getAddActionId() {
        return R.id.action_add_account;
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
        return getString(R.string.account_delete_confirm, AccountManager.getInstance().getVerboseName(actionWith));
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
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu, String actionWith) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(actionWith);
        menu.setHeaderTitle(AccountManager.getInstance().getVerboseName(actionWith));
        if (accountItem.isEnabled()) {
            menu.add(0, CONTEXT_MENU_STATUS_EDITOR_ID, 0, getResources()
                    .getText(R.string.status_editor));
        }
        menu.add(0, CONTEXT_MENU_VIEW_ACCOUNT_ID, 0, getString(R.string.account_editor));
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
        adapter.onChange();
    }

    @Override
    protected String getSavedValue(Bundle bundle, String key) {
        return bundle.getString(key);
    }

    @Override
    protected void putSavedValue(Bundle bundle, String key, String actionWith) {
        bundle.putString(key, actionWith);
    }

}
