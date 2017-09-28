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
package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.AccountListAdapter;
import com.xabber.android.ui.adapter.AccountListReorderAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.AccountDeleteDialog;
import com.xabber.android.ui.widget.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccountListActivity extends ManagedActivity implements OnAccountChangedListener,
        AccountListAdapter.Listener, Toolbar.OnMenuItemClickListener {

    private AccountListReorderAdapter accountListAdapter;
    private BarPainter barPainter;
    private ItemTouchHelper touchHelper;
    private Toolbar toolbar;
    private TextView tvSummary;

    public static Intent createIntent(Context context) {
        return new Intent(context, AccountListActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_list);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(AccountListActivity.this);
            }
        });
        toolbar.setTitle(R.string.title_reordering_account);
        toolbar.inflateMenu(R.menu.toolbar_account_list);
        toolbar.setOnMenuItemClickListener(this);

        barPainter = new BarPainter(this, toolbar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.account_list_recycler_view);
        tvSummary = (TextView) findViewById(R.id.tvSummary);

        accountListAdapter = new AccountListReorderAdapter(this, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(accountListAdapter);

        ItemTouchHelper.Callback callback =
                new SimpleItemTouchHelperCallback(accountListAdapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    private void update() {
        List<AccountItem> accountItems = new ArrayList<>();
        for (AccountItem accountItem : AccountManager.getInstance().getAllAccountItems()) {
            accountItems.add(accountItem);
        }

        accountListAdapter.setAccountItems(accountItems);

        barPainter.setDefaultColor();

        // set margin for textView
        int height = 52 * (accountItems.size() + 1) + 16;
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (height * scale + 0.5f);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, pixels, 16, 0);
        tvSummary.setLayoutParams(params);
    }

    public void updateAccountOrder() {
        if (accountListAdapter != null) {
            int order = 1;
            for (AccountItem account : accountListAdapter.getItems())
                AccountManager.getInstance().setOrder(account.getAccount(), order++);

            XabberAccountManager.getInstance().setLastOrderChangeTimestampIsNow();
            if (XabberAccountManager.getInstance().getAccount() != null)
                XabberAccountManager.getInstance().updateAccountSettings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_account_edit_status:
                return true;

            case R.id.action_account_edit:
                return true;
            case R.id.action_account_delete:
                return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    @Override
    public void onAccountClick(AccountJid account) {
        startActivity(AccountActivity.createIntent(this, account));
    }

    @Override
    public void onEditAccountStatus(AccountItem accountItem) {
        startActivity(StatusEditActivity.createIntent(this, accountItem.getAccount()));
    }

    @Override
    public void onEditAccount(AccountItem accountItem) {
        startActivity(AccountActivity.createIntent(this, accountItem.getAccount()));
    }

    @Override
    public void onDeleteAccount(AccountItem accountItem) {
        AccountDeleteDialog.newInstance(accountItem.getAccount()).show(getFragmentManager(),
                AccountDeleteDialog.class.getName());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_done) {
            updateAccountOrder();
            finish();
            return true;
        }

        return false;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }
}
