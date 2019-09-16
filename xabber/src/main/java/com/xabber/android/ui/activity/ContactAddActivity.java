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
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.ContactAddFragment;
import com.xabber.android.ui.helper.ContactAdder;
import com.xabber.android.ui.helper.ToolbarHelper;

public class ContactAddActivity extends ManagedActivity implements ContactAddFragment.Listener {

    private BarPainter barPainter;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    public static Intent createIntent(Context context) {
        return createIntent(context, null);
    }

    public static Intent createIntent(Context context, AccountJid account) {
        return createIntent(context, account, null);
    }

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        return new EntityIntentBuilder(context, ContactAddActivity.class).setAccount(account).setUser(user).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    private static String getContact(Intent intent){
        String contact;
        Bundle bundle = intent.getExtras();
        contact = bundle.get("contact").toString();
        return contact;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_progress_and_container);

        toolbar = ToolbarHelper.setUpDefaultToolbar(this, null, R.drawable.ic_clear_white_24dp);
        toolbar.inflateMenu(R.menu.toolbar_add_contact);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        progressBar = findViewById(R.id.toolbarProgress);
        barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        Intent intent = getIntent();

        if(intent.hasExtra("contact")) {
            if (savedInstanceState == null) {
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, ContactAddFragment.newInstance(getAccount(intent), getUser(intent), getContact(intent)))
                        .commit();
            }
        }else if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, ContactAddFragment.newInstance(getAccount(intent), getUser(intent)))
                    .commit();
        }

    }

    private void addContact() {
        ((ContactAdder) getFragmentManager().findFragmentById(R.id.fragment_container)).addContact();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.toolbar_add_contact, menu);
        toolbar.getMenu().findItem(R.id.action_add_account).setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                addContact();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAccountSelected(AccountJid account) {
        barPainter.updateWithAccountName(account);
    }

    @Override
    public void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            toolbar.getMenu().findItem(R.id.action_add_contact).setVisible(!show);
        }
    }

    public void toolbarSetEnabled(boolean active){
        if (active) toolbar.getMenu().findItem(R.id.action_add_contact).setEnabled(true);
        else toolbar.getMenu().findItem(R.id.action_add_contact).setEnabled(false);
    }

}
