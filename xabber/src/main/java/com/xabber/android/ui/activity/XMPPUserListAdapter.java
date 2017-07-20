package com.xabber.android.ui.activity;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XMPPUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPUserListAdapter extends RecyclerView.Adapter {

    List<XMPPUser> items;

    public XMPPUserListAdapter() {
        this.items = new ArrayList<>();
    }

    public void setItems(List<XMPPUser> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new XMPPUserViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        XMPPUser user = items.get(position);
        XMPPUserViewHolder viewHolder = (XMPPUserViewHolder) holder;

        viewHolder.name.setText("User Name");
        viewHolder.status.setText(user.getUsername() + "@" + user.getHost());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private class XMPPUserViewHolder extends RecyclerView.ViewHolder {

        ImageView color;
        ImageView avatar;
        TextView name;
        TextView status;
        SwitchCompat enabledSwitch;


        XMPPUserViewHolder(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.item_account_color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            name = (TextView) itemView.findViewById(R.id.item_account_name);
            status = (TextView) itemView.findViewById(R.id.item_account_status);
            enabledSwitch = (SwitchCompat) itemView.findViewById(R.id.item_account_switch);
        }

    }
}
