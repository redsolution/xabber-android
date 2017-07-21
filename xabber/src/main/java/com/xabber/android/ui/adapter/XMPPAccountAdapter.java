package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XMPPAccountSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPAccountAdapter extends RecyclerView.Adapter {

    List<XMPPAccountSettings> items;

    public XMPPAccountAdapter() {
        this.items = new ArrayList<>();
    }

    public void setItems(List<XMPPAccountSettings> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new XMPPAccountVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        XMPPAccountSettings account = items.get(position);
        XMPPAccountVH viewHolder = (XMPPAccountVH) holder;

        viewHolder.username.setText(account.getUsername());
        viewHolder.jid.setText(account.getJid());
        viewHolder.syncSwitch.setChecked(account.isSynchronization());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private class XMPPAccountVH extends RecyclerView.ViewHolder {

        ImageView color;
        ImageView avatar;
        TextView username;
        TextView jid;
        SwitchCompat syncSwitch;


        XMPPAccountVH(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.item_account_color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            username = (TextView) itemView.findViewById(R.id.item_account_name);
            jid = (TextView) itemView.findViewById(R.id.item_account_status);
            syncSwitch = (SwitchCompat) itemView.findViewById(R.id.item_account_switch);
        }

    }
}
