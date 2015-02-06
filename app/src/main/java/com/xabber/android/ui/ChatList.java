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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.ChatListAdapter;
import com.xabber.android.ui.helper.ManagedListActivity;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChatList extends ManagedListActivity implements OnAccountChangedListener,
        OnContactChangedListener, OnChatChangedListener, OnItemClickListener {

    private ChatListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.list);
        listAdapter = new ChatListAdapter(this);
        setListAdapter(listAdapter);
        getListView().setOnItemClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnChatChangedListener.class, this);
        updateAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnChatChangedListener.class, this);
    }

    @Override
    public void onChatChanged(String account, String user, boolean incoming) {
        updateAdapter();
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> addresses) {
        updateAdapter();
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        updateAdapter();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AbstractChat chat = (AbstractChat) parent.getAdapter().getItem(position);
        startActivity(ChatViewer.createIntent(this, chat.getAccount(), chat.getUser()));
        finish();
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, ChatList.class);
    }

    private void updateAdapter() {
        List<AbstractChat> chats = new ArrayList<>();
        chats.addAll(MessageManager.getInstance().getActiveChats());

        if (chats.size() == 0) {
            Toast.makeText(this, R.string.chat_list_is_empty, Toast.LENGTH_LONG).show();
            finish();
        }

        Collections.sort(chats, ChatComparator.CHAT_COMPARATOR);
        listAdapter.updateChats(chats);
    }

}
