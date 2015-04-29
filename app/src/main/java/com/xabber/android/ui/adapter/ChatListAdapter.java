package com.xabber.android.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends BaseAdapter {

    private List<AbstractChat> chats;

    private final ContactItemInflater contactItemInflater;

    public ChatListAdapter(Context context) {
        chats = new ArrayList<>();
        contactItemInflater = new ContactItemInflater(context);
    }

    public void updateChats(List<AbstractChat> chats) {
        this.chats.clear();
        this.chats.addAll(chats);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chats.size();
    }

    @Override
    public Object getItem(int position) {
        return chats.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AbstractChat abstractChat = (AbstractChat) getItem(position);
        final AbstractContact abstractContact = RosterManager.getInstance()
                .getBestContact(abstractChat.getAccount(), abstractChat.getUser());
        return contactItemInflater.setUpContactView(convertView, parent, abstractContact);
    }
}
