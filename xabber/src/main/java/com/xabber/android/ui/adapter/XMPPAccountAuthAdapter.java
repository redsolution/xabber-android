package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPAccountAuthAdapter extends RecyclerView.Adapter {

    private List<AccountItem> items;

    public void setItems(List<AccountItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new XMPPAccountVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_xmpp_account_choose, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final AccountItem account = items.get(position);
        XMPPAccountVH viewHolder = (XMPPAccountVH) holder;

        // set color
        viewHolder.jid.setTextColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(account.getAccount()));

        // set avatar
        viewHolder.avatar.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(account.getAccount()));

        // set jid
        viewHolder.jid.setText(account.getAccount().getFullJid().asBareJid().toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private class XMPPAccountVH extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView jid;
        TextView action;

        XMPPAccountVH(View itemView) {
            super(itemView);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            jid = (TextView) itemView.findViewById(R.id.tvAccountJid);
            action = (TextView) itemView.findViewById(R.id.tvAction);
        }
    }
}
