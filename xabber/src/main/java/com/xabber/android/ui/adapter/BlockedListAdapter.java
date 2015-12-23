package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BlockedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    private String account;
    private List<String> blockedContacts;
    private OnBlockedContactClickListener listener;

    private Set<String> checkedContacts;

    public BlockedListAdapter(String account) {
        this.account = account;
        blockedContacts = new ArrayList<>();
        checkedContacts = new HashSet<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BlockListItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.block_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final BlockListItemViewHolder viewHolder = (BlockListItemViewHolder) holder;
        final String contact = blockedContacts.get(position);

        final AbstractContact rosterContact = RosterManager.getInstance().getBestContact(account, contact);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(rosterContact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.name.setText(rosterContact.getName());
        viewHolder.name.setVisibility(View.VISIBLE);


        viewHolder.checkBox.setChecked(checkedContacts.contains(contact));

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked());
            }
        });

        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkedContacts.add(contact);
                } else {
                    checkedContacts.remove(contact);
                }
                if (listener != null) {
                    listener.onBlockedContactClick();
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return blockedContacts.size();
    }

    @Override
    public void onChange() {
        blockedContacts.clear();
        final Collection<String> blockedContacts = BlockingManager.getInstance().getBlockedContacts(account);
        if (blockedContacts != null) {
            this.blockedContacts.addAll(blockedContacts);
        }

        final Collection<String> blockedMucContacts = PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account);
        if (blockedMucContacts != null) {
            this.blockedContacts.addAll(blockedMucContacts);
        }

        final Iterator<String> iterator = checkedContacts.iterator();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (!this.blockedContacts.contains(next)) {
                iterator.remove();
            }
        }

        notifyDataSetChanged();
    }

    public interface OnBlockedContactClickListener {
        void onBlockedContactClick();
    }

    public void setListener(OnBlockedContactClickListener listener) {
        this.listener = listener;
    }

    class BlockListItemViewHolder extends RecyclerView.ViewHolder {

        final ImageView avatar;
        final TextView name;
        final CheckBox checkBox;

        public BlockListItemViewHolder(View view) {
            super(view);

            avatar = (ImageView) view.findViewById(R.id.avatar);
            name = (TextView) view.findViewById(R.id.contact_list_item_name);
            checkBox = (CheckBox) view.findViewById(R.id.block_list_contact_checkbox);
        }
    }

    public ArrayList<String> getCheckedContacts() {
        return new ArrayList<>(checkedContacts);
    }

    public void setCheckedContacts(List<String> checkedContacts) {
        this.checkedContacts.clear();
        this.checkedContacts.addAll(checkedContacts);
    }
}
