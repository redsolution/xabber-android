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
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.ConferenceAddFragment;

import org.jxmpp.jid.EntityBareJid;

public class ConferenceAddActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.activity.ConferenceAdd.SAVED_ACCOUNT";
    private static final String SAVED_ROOM = "com.xabber.android.ui.activity.ConferenceAdd.SAVED_ROOM";

    private AccountJid account;
    private EntityBareJid room;

    public static Intent createIntent(Context context, AccountJid account, UserJid room) {
        return new EntityIntentBuilder(context, ConferenceAddActivity.class).setAccount(account).setUser(room).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
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
        toolbar.inflateMenu(R.menu.toolbar_add_conference);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setOnMenuItemClickListener(this);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        Intent intent = getIntent();

        if (savedInstanceState != null) {
            account = savedInstanceState.getParcelable(SAVED_ACCOUNT);
            room = (EntityBareJid) savedInstanceState.getSerializable(SAVED_ROOM);
        } else {
            account = getAccount(intent);
            room = getUser(intent).getJid().asEntityBareJidIfPossible();
        }

        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, ConferenceAddFragment.newInstance(account, room))
                    .commit();
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_ACCOUNT, account);
        outState.putSerializable(SAVED_ROOM, room);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_conference:
                ((ConferenceAddFragment)getFragmentManager().findFragmentById(R.id.fragment_container)).addConference();
                return true;

            default:
                return false;
        }
    }
}
