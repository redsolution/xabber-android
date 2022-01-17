/*
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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.AccountAddFragment;

public class AccountAddActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    Toolbar toolbar;

    public static Intent createIntent(Context context) {
        return new Intent(context, AccountAddActivity.class);
    }

    public static Intent createAuthenticatorResult(AccountJid account) {
        Intent intent = new Intent();
        intent.putExtra("com.xabber.android.data.INTENT_ACCOUNT_JID_KEY", (Parcelable) account);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_with_toolbar_and_container);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.content_container,
                    AccountAddFragment.newInstance()).commit();
        }
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.toolbar_add_account);
        toolbar.getMenu().findItem(R.id.action_add_account).setIcon(null);

        toolbar.getMenu().findItem(R.id.action_add_account).setEnabled(false);
        View view = toolbar.findViewById(R.id.action_add_account);
        if (view != null && view instanceof TextView){
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                ((TextView)view).setTextColor(getResources().getColor(R.color.grey_900));
            } else {
                ((TextView)view).setTextColor(Color.WHITE);
            }
        }
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
        if (item.getItemId() == R.id.action_add_account) {
            ((AccountAddFragment) getFragmentManager()
                    .findFragmentById(R.id.content_container)).addAccount();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    public void toolbarSetEnabled(boolean active){
        toolbar.getMenu().findItem(R.id.action_add_account).setEnabled(active);
    }

}
