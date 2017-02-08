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
package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter {

    @SuppressWarnings("WeakerAccess")
    List<AccountItem> accountItems;
    @SuppressWarnings("WeakerAccess")
    Listener listener;
    ManagedActivity activity;

    public interface Listener {
        void onAccountClick(AccountJid account);
        void onEditAccountStatus(AccountItem accountItem);
        void onEditAccount(AccountItem accountItem);
        void onDeleteAccount(AccountItem accountItem);
    }

    public AccountListAdapter(ManagedActivity activity, Listener listener) {
        this.accountItems = new ArrayList<>();
        this.activity = activity;
        this.listener = listener;
    }

    public void setAccountItems(List<AccountItem> accountItems) {
        this.accountItems = accountItems;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AccountViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AccountViewHolder accountHolder = (AccountViewHolder) holder;
        AccountItem accountItem = accountItems.get(position);

        accountHolder.color.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(accountItem.getAccount()));

        accountHolder.avatar.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(accountItem.getAccount()));

        accountHolder.name.setText(AccountManager.getInstance().getVerboseName(accountItem.getAccount()));
        accountHolder.status.setText(accountItem.getState().getStringId());

        accountHolder.enabledSwitch.setChecked(accountItem.isEnabled());
    }

    @Override
    public int getItemCount() {
        return accountItems.size();
    }

    private class AccountViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        ImageView color;
        ImageView avatar;
        TextView name;
        TextView status;
        SwitchCompat enabledSwitch;


        AccountViewHolder(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.item_account_color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            name = (TextView) itemView.findViewById(R.id.item_account_name);
            status = (TextView) itemView.findViewById(R.id.item_account_status);
            enabledSwitch = (SwitchCompat) itemView.findViewById(R.id.item_account_switch);

            // I used on click listener instead of on checked change listener to avoid callback in onBindViewHolder
            enabledSwitch.setOnClickListener(this);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

        }

        @Override
        public void onClick(View v) {
            AccountItem accountItem = accountItems.get(getAdapterPosition());

            switch (v.getId()) {
                case R.id.item_account_switch:
                    AccountManager.getInstance().setEnabled(accountItem.getAccount(), enabledSwitch.isChecked());

                    break;

                default:
                    if (listener != null) {
                        listener.onAccountClick(accountItem.getAccount());
                    }
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            AccountItem accountItem = accountItems.get(getAdapterPosition());

            MenuInflater inflater = activity.getMenuInflater();
            inflater.inflate(R.menu.item_account, menu);

            menu.setHeaderTitle(AccountManager.getInstance().getVerboseName(accountItem.getAccount()));
            menu.findItem(R.id.action_account_edit_status).setVisible(accountItem.isEnabled());

            menu.findItem(R.id.action_account_edit_status).setOnMenuItemClickListener(this);
            menu.findItem(R.id.action_account_edit).setOnMenuItemClickListener(this);
            menu.findItem(R.id.action_account_delete).setOnMenuItemClickListener(this);


        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AccountItem accountItem = accountItems.get(getAdapterPosition());

            switch (item.getItemId()) {
                case R.id.action_account_edit_status:
                    listener.onEditAccountStatus(accountItem);
                    return true;

                case R.id.action_account_edit:
                    listener.onEditAccount(accountItem);
                    return true;
                case R.id.action_account_delete:
                    listener.onDeleteAccount(accountItem);
                    return true;
            }

            return false;
        }
    }

}
