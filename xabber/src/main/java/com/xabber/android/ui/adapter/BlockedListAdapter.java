package com.xabber.android.ui.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.AccountPainter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlockedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    private String account;
    private List<String> blockedContacts;
    private final AccountPainter accountPainter;
    private OnBlockedContactClickListener listener;

    public BlockedListAdapter(Context context, String account) {
        this.account = account;
        blockedContacts = new ArrayList<>();
        accountPainter = new AccountPainter(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ContactListItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContactListItemViewHolder viewHolder = (ContactListItemViewHolder) holder;
        final String contact = blockedContacts.get(position);

        viewHolder.mucIndicator.setVisibility(View.GONE);
        viewHolder.largeClientIcon.setVisibility(View.GONE);
        viewHolder.offlineShadow.setVisibility(View.GONE);
        viewHolder.statusIcon.setVisibility(View.GONE);
        viewHolder.statusIconSeparator.setVisibility(View.GONE);
        viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);
        viewHolder.secondLineMessage.setVisibility(View.GONE);
        viewHolder.smallRightIcon.setVisibility(View.GONE);
        viewHolder.smallRightText.setVisibility(View.GONE);

        final AbstractContact rosterContact = RosterManager.getInstance().getBestContact(account, contact);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(rosterContact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.color.setImageDrawable(new ColorDrawable(accountPainter.getAccountMainColor(account)));
        viewHolder.color.setVisibility(View.VISIBLE);

        viewHolder.name.setText(rosterContact.getName());
        viewHolder.name.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onBlockedContactClick(viewHolder.itemView, contact);
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
        notifyDataSetChanged();
    }

    public interface OnBlockedContactClickListener {
        void onBlockedContactClick(View itemView, String contact);
    }

    public void setListener(OnBlockedContactClickListener listener) {
        this.listener = listener;
    }
}
