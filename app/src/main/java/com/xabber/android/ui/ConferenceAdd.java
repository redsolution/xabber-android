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
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;

import java.util.Collection;

public class ConferenceAdd extends ManagedActivity implements ConferenceAddFragment.Listener {

    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.MUCEditor.SAVED_ACCOUNT";
    private static final String SAVED_ROOM = "com.xabber.android.ui.MUCEditor.SAVED_ROOM";

    private BarPainter barPainter;
    private String account;
    private String room;

    public static Intent createIntent(Context context) {
        return ConferenceAdd.createIntent(context, null, null);
    }

    public static Intent createIntent(Context context, String account,
                                      String room) {
        return new EntityIntentBuilder(context, ConferenceAdd.class).setAccount(account).setUser(room).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_with_toolbar_and_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        setTitle(null);

        setSupportActionBar(toolbar);

        barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        Intent intent = getIntent();

        account = null;
        room = null;

        if (savedInstanceState != null) {
            account = savedInstanceState.getString(SAVED_ACCOUNT);
            room = savedInstanceState.getString(SAVED_ROOM);
        } else {
            account = getAccount(intent);
            room = getUser(intent);
        }

        if (account == null) {
            Collection<String> accounts = AccountManager.getInstance().getAccounts();
            if (accounts.size() == 1) {
                account = accounts.iterator().next();
            }
        }

        if (account != null) {
            barPainter.updateWithAccountName(account);
        }

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, ConferenceAddFragment.newInstance(account, room))
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_ACCOUNT, account);
        outState.putString(SAVED_ROOM, room);
    }

    @Override
    public void onAccountSelected(String account) {
        barPainter.updateWithAccountName(account);
        this.account = account;
    }
}
