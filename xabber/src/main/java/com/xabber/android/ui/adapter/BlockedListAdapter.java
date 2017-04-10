package com.xabber.android.ui.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BlockedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    private AccountJid account;
    @SuppressWarnings("WeakerAccess")
    List<UserJid> blockedContacts;

    @SuppressWarnings("WeakerAccess")
    @Nullable OnBlockedContactClickListener listener;

    @SuppressWarnings("WeakerAccess")
    Set<UserJid> checkedContacts;

    public BlockedListAdapter(AccountJid account) {
        this.account = account;
        blockedContacts = new ArrayList<>();
        checkedContacts = new HashSet<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BlockListItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_block, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final BlockListItemViewHolder viewHolder = (BlockListItemViewHolder) holder;
        final UserJid contact = blockedContacts.get(position);

        final AbstractContact rosterContact = RosterManager.getInstance().getBestContact(account, contact);

        if (viewHolder.avatar != null) {
            viewHolder.avatar.setImageDrawable(rosterContact.getAvatarForContactList());
        }

        viewHolder.name.setText(rosterContact.getName());
        viewHolder.name.setVisibility(View.VISIBLE);

        viewHolder.checkBox.setChecked(checkedContacts.contains(contact));
    }

    @Override
    public int getItemCount() {
        return blockedContacts.size();
    }

    @Override
    public void onChange() {
        blockedContacts.clear();
        final Collection<UserJid> blockedContacts = BlockingManager.getInstance().getBlockedContacts(account);
        if (blockedContacts != null) {
            this.blockedContacts.addAll(blockedContacts);
        }

        // remove checked contacts not containing in new blocked list
        final Iterator<UserJid> iterator = checkedContacts.iterator();
        while (iterator.hasNext()) {
            final UserJid next = iterator.next();
            if (!this.blockedContacts.contains(next)) {
                iterator.remove();
            }
        }

        notifyDataSetChanged();
    }

    public interface OnBlockedContactClickListener {
        void onBlockedContactClick();
    }

    public void setListener(@Nullable OnBlockedContactClickListener listener) {
        this.listener = listener;
    }

    private class BlockListItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final String LOG_TAG = BlockListItemViewHolder.class.getSimpleName();
        @Nullable
        final ImageView avatar;
        final TextView name;
        final CheckBox checkBox;

        BlockListItemViewHolder(View view) {
            super(view);

            if (SettingsManager.contactsShowAvatars()) {
                avatar = (ImageView) view.findViewById(R.id.avatar);
                avatar.setVisibility(View.VISIBLE);
            } else {
                avatar = null;
            }

            name = (TextView) view.findViewById(R.id.contact_list_item_name);
            checkBox = (CheckBox) view.findViewById(R.id.block_list_contact_checkbox);
            checkBox.setOnClickListener(this);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            UserJid userJid = blockedContacts.get(adapterPosition);

            if (checkedContacts.contains(userJid)) {
                checkedContacts.remove(userJid);
                checkBox.setChecked(false);
            } else {
                checkedContacts.add(userJid);
                checkBox.setChecked(true);
            }

            if (listener != null) {
                listener.onBlockedContactClick();
            }

        }
    }

    public ArrayList<UserJid> getCheckedContacts() {
        return new ArrayList<>(checkedContacts);
    }

    public void setCheckedContacts(List<UserJid> checkedContacts) {
        this.checkedContacts.clear();
        this.checkedContacts.addAll(checkedContacts);
    }
}
