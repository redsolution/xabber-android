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
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.AccountAddFragment;

public class AccountAddActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    public static Intent createIntent(Context context) {
        return new Intent(context, AccountAddActivity.class);
    }

    public static Intent createAuthenticatorResult(AccountJid account) {
        return new AccountIntentBuilder(null, null).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.activity_with_toolbar_and_container);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.fragment_container, AccountAddFragment.newInstance()).commit();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_add_account);
        toolbar.getMenu().findItem(R.id.action_add_account).setIcon(null);
        toolbar.setOnMenuItemClickListener(this);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.toolbar_add_account, menu);
        menu.findItem(R.id.action_add_account).setIcon(null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_account:
                ((AccountAddFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).addAccount();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }
}
