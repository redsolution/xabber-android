package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ContactListItemViewHolder.ContactClickListener {

    private List<AbstractContact> contacts;
    private final ContactItemInflater contactItemInflater;

    public ChatListAdapter(Context context) {
        contacts = new ArrayList<>();
        contactItemInflater = new ContactItemInflater(context);
    }

    public void updateContacts(List<AbstractContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ContactListItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false), this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        contactItemInflater.bindViewHolder((ContactListItemViewHolder) holder, contacts.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onContactClick(int adapterPosition) {

    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        contactItemInflater.onAvatarClick(contacts.get(adapterPosition));
    }
}
