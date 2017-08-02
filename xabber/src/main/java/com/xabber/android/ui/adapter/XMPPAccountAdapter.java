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
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPAccountAdapter extends RecyclerView.Adapter {

    private List<XMPPAccountSettings> items;
    private boolean isAllChecked;

    public XMPPAccountAdapter() {}

    public void setItems(List<XMPPAccountSettings> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setAllChecked(boolean checked) {
        isAllChecked = checked;

        for (XMPPAccountSettings item : items) {
            item.setSynchronization(checked);
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new XMPPAccountVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_xmpp_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final XMPPAccountSettings account = items.get(position);
        XMPPAccountVH viewHolder = (XMPPAccountVH) holder;

        viewHolder.chkAccountSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                account.setSynchronization(isChecked);
            }
        });

        // set colors
        int colorId = ColorManager.getInstance().convertColorNameToId(account.getColor());
        viewHolder.color.setColorFilter(colorId);
        viewHolder.username.setTextColor(colorId);

        // set username
        if (account.getUsername() != null && !account.getUsername().isEmpty())
            viewHolder.username.setText(account.getUsername());
        else viewHolder.username.setText(account.getJid());

        // set jid
        viewHolder.jid.setText(account.getJid());

        // set sync checkbox
        viewHolder.chkAccountSync.setChecked(account.isSynchronization());

        if (isAllChecked) {
            viewHolder.chkAccountSync.setEnabled(false);
        } else {
            viewHolder.chkAccountSync.setEnabled(true);
        }
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
        CheckBox chkAccountSync;


        XMPPAccountVH(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            username = (TextView) itemView.findViewById(R.id.tvAccountName);
            jid = (TextView) itemView.findViewById(R.id.tvAccountJid);
            chkAccountSync = (CheckBox) itemView.findViewById(R.id.chkAccountSync);
        }

    }
}
