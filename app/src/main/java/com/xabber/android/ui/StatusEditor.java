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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.SavedStatus;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.adapter.StatusEditorAdapter;
import com.xabber.android.ui.adapter.StatusModeAdapter;
import com.xabber.android.ui.helper.ActionBarPainter;
import com.xabber.android.ui.helper.ManagedListActivity;
import com.xabber.androiddev.R;

public class StatusEditor extends ManagedListActivity implements OnItemClickListener, Toolbar.OnMenuItemClickListener {

    private static final String SAVED_TEXT = "com.xabber.android.ui.StatusEditor.SAVED_TEXT";
    private static final String SAVED_MODE = "com.xabber.android.ui.StatusEditor.SAVED_MODE";

    private String account;
    private Spinner statusModeView;
    private EditText statusTextView;

    private SavedStatus actionWithItem;
    private StatusEditorAdapter adapter;
    private View savedStatusesTextView;
    private Toolbar bottomToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        actionWithItem = null;

        setContentView(R.layout.status_editor);
        setSupportActionBar((Toolbar) findViewById(R.id.top_toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
        getSupportActionBar().setTitle(null);

        bottomToolbar = (Toolbar) findViewById(R.id.bottom_toolbar);
        bottomToolbar.inflateMenu(R.menu.clear_status_history);
        bottomToolbar.setOnMenuItemClickListener(this);

        Intent intent = getIntent();
        account = StatusEditor.getAccount(intent);
        if (account != null) {
            ActionBarPainter actionBarPainter = new ActionBarPainter(this);
            actionBarPainter.updateWithAccountName(account);
            bottomToolbar.setBackgroundColor(actionBarPainter.getAccountColor(account));
        }

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        adapter = new StatusEditorAdapter(this);
        setListAdapter(adapter);

        statusTextView = (EditText) findViewById(R.id.status_text);
        statusModeView = (Spinner) findViewById(R.id.status_icon);
        statusModeView.setAdapter(new StatusModeAdapter(this));

        savedStatusesTextView = findViewById(R.id.saved_statuses_textview);

        StatusMode statusMode;
        String statusText;
        if (savedInstanceState == null) {
            if (account == null) {
                statusMode = SettingsManager.statusMode();
                statusText = SettingsManager.statusText();
            } else {
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem == null) {
                    Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
                    finish();
                    return;
                }
                statusMode = accountItem.getFactualStatusMode();
                statusText = accountItem.getStatusText();
            }
        } else {
            statusMode = StatusMode.valueOf(savedInstanceState.getString(SAVED_MODE));
            statusText = savedInstanceState.getString(SAVED_TEXT);
        }
        showStatus(statusMode, statusText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        StatusMode statusMode = (StatusMode) statusModeView.getSelectedItem();
        outState.putString(SAVED_MODE, statusMode.name());
        outState.putString(SAVED_TEXT, statusTextView.getText().toString());
    }

    private void setStatus(StatusMode statusMode, String statusText) {
        AccountManager accountManager = AccountManager.getInstance();
        if (account != null) {
            accountManager.setStatus(account, statusMode, statusText);
        } else {
            accountManager.setStatus(statusMode, statusText);
        }
    }

    private void showStatus(StatusMode statusMode, String statusText) {
        for (int index = 0; index < statusModeView.getCount(); index++) {
            if (statusMode == statusModeView.getAdapter().getItem(index)) {
                statusModeView.setSelection(index);
            }
        }
        statusTextView.setText(statusText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.onChange();
        setStatusHistoryVisibility();
    }

    private void setStatusHistoryVisibility() {
        boolean isHistoryEmpty = AccountManager.getInstance().getSavedStatuses().isEmpty();
        int visibility = isHistoryEmpty ? View.GONE : View.VISIBLE;

        getListView().setVisibility(visibility);
        savedStatusesTextView.setVisibility(visibility);
        bottomToolbar.setVisibility(visibility);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.set_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_change_status:
                changeStatus();
                return true;

            case R.id.action_clear_status_history:
                AccountManager.getInstance().clearSavedStatuses();
                adapter.onChange();
                setStatusHistoryVisibility();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        actionWithItem = (SavedStatus) getListView().getItemAtPosition(info.position);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.status_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_select_status:
                setStatus(actionWithItem.getStatusMode(), actionWithItem.getStatusText());
                finish();
                return true;
            case R.id.action_edit_status:
                showStatus(actionWithItem.getStatusMode(), actionWithItem.getStatusText());
                return true;
            case R.id.action_remove_status:
                AccountManager.getInstance().removeSavedStatus(actionWithItem);
                adapter.onChange();
                setStatusHistoryVisibility();
                return true;
        }
        return false;
    }

    private void changeStatus() {
        StatusMode statusMode = (StatusMode) statusModeView.getSelectedItem();
        String statusText = statusTextView.getText().toString();
        setStatus(statusMode, statusText);
        finish();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SavedStatus savedStatus = (SavedStatus) parent.getAdapter().getItem(position);
        setStatus(savedStatus.getStatusMode(), savedStatus.getStatusText());
        finish();
    }

    public static Intent createIntent(Context context) {
        return StatusEditor.createIntent(context, null);
    }

    public static Intent createIntent(Context context, String account) {
        return new AccountIntentBuilder(context, StatusEditor.class).setAccount(account).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return onOptionsItemSelected(menuItem);
    }
}
