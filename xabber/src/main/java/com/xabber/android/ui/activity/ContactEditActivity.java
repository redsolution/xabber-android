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
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.ContactEditFragment;

import org.jxmpp.jid.BareJid;

import java.util.Collection;

public class ContactEditActivity extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener, Toolbar.OnMenuItemClickListener {

    private AccountJid account;
    private UserJid user;
    private Toolbar toolbar;
    private BarPainter barPainter;

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ContactEditActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    private static AccountJid getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.inflateMenu(R.menu.toolbar_save);
        toolbar.setTitle(R.string.contact_title);
        TextView tvSave = (TextView) findViewById(R.id.action_save);

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
            tvSave.setTextColor(getResources().getColor(R.color.grey_900));
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
            tvSave.setTextColor(getResources().getColor(R.color.white));
        }
        tvSave.setPadding(tvSave.getPaddingLeft(), tvSave.getPaddingTop(), tvSave.getPaddingRight() + 20, tvSave.getPaddingBottom());
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        toolbarSetEnabled(false);

        barPainter = new BarPainter(this, toolbar);

        Intent intent = getIntent();
        account = ContactEditActivity.getAccount(intent);
        user = ContactEditActivity.getUser(intent);

        update();

        if (AccountManager.getInstance().getAccount(account) == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, ContactEditFragment.newInstance(account, user)).commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
    }

    private void update() {
        barPainter.updateWithAccountName(account);
    }

    public void toolbarSetEnabled(boolean active){
        toolbar.getMenu().findItem(R.id.action_save).setEnabled(active);
        View view = findViewById(R.id.action_save);
        ((TextView)view).setTextColor(((TextView) view).getTextColors().withAlpha(active ? 255 : 127));
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        BareJid thisBareAddress = user.getBareJid();
        for (BaseEntity entity : entities) {
            if (entity.equals(account, thisBareAddress)) {
                update();
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            update();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                ((ContactEditFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container)).saveChanges();
                finish();
        }
        return false;
    }
}
