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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleActionBarInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.xmpp.address.Jid;

import java.util.Collection;

public class GroupEditor extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener {

    ContactTitleActionBarInflater contactTitleActionBarInflater;
    private String account;
    private String user;

    public static Intent createIntent(Context context, String account, String user) {
        Intent intent = new EntityIntentBuilder(context, GroupEditor.class)
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);

        contactTitleActionBarInflater = new ContactTitleActionBarInflater(this, (Toolbar) findViewById(R.id.toolbar_default));
        contactTitleActionBarInflater.setUpActionBarView();

        Intent intent = getIntent();
        account = GroupEditor.getAccount(intent);
        user = GroupEditor.getUser(intent);

        if (AccountManager.getInstance().getAccount(account) == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, GroupEditorFragment.newInstance(account, user)).commit();
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
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);
        contactTitleActionBarInflater.update(abstractContact);
        contactTitleActionBarInflater.setStatusText(user);
        contactTitleActionBarInflater.hideStatusIcon();
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        String thisBareAddress = Jid.getBareAddress(user);
        for (BaseEntity entity : entities) {
            if (entity.equals(account, thisBareAddress)) {
                update();
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account)) {
            update();
        }
    }

}
