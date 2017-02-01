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
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.OccupantListAdapter;
import com.xabber.android.ui.color.BarPainter;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.Collection;

/**
 * Represent list of occupants in the room.
 *
 * @author alexander.ivanov
 */
public class OccupantListActivity extends ManagedListActivity implements
        OnAccountChangedListener, OnContactChangedListener, AdapterView.OnItemClickListener {

    private AccountJid account;
    private EntityBareJid room;
    private OccupantListAdapter listAdapter;

    public static Intent createIntent(Context context, AccountJid account, UserJid room) {
        return new EntityIntentBuilder(context, OccupantListActivity.class)
                .setAccount(account).setUser(room).build();
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

        account = getAccount(getIntent());
        room = getUser(getIntent()).getJid().asEntityBareJidIfPossible();
        if (account == null || room == null || !MUCManager.getInstance().hasRoom(account, room)) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        setContentView(R.layout.list);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(OccupantListActivity.this);
            }
        });
        toolbar.setTitle(room);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);


        listAdapter = new OccupantListAdapter(this, account, room);
        setListAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        listAdapter.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        try {
            if (entities.contains(RosterManager.getInstance().getAbstractContact(account, UserJid.from(room)))) {
                listAdapter.onChange();
            }
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            listAdapter.onChange();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        com.xabber.android.data.extension.muc.Occupant occupant
                = (com.xabber.android.data.extension.muc.Occupant) listAdapter.getItem(position);
        LogManager.i(this, occupant.getNickname().toString());

        UserJid occupantFullJid = null;
        try {
            occupantFullJid = UserJid.from(JidCreate.entityFullFrom(room, occupant.getNickname()));
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            return;
        }

        final AbstractChat mucPrivateChat;
        try {
            mucPrivateChat = MessageManager.getInstance()
                    .getOrCreatePrivateMucChat(account, occupantFullJid.getJid().asFullJidIfPossible());
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            return;
        }
        mucPrivateChat.setIsPrivateMucChatAccepted(true);

        startActivity(ChatActivity.createSpecificChatIntent(this, account, occupantFullJid));
    }
}
