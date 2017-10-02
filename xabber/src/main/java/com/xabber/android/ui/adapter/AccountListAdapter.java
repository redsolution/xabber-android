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

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.widget.ItemTouchHelperAdapter;

import org.jxmpp.jid.BareJid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter implements ItemTouchHelperAdapter {

    @SuppressWarnings("WeakerAccess")
    static final String LOG_TAG = AccountListAdapter.class.getSimpleName();
    @SuppressWarnings("WeakerAccess")
    List<AccountItem> accountItems;
    @SuppressWarnings("WeakerAccess")
    Listener listener;
    ManagedActivity activity;
    boolean showAnchors = false;

    public interface Listener {
        void onAccountClick(AccountJid account);
        void onEditAccountStatus(AccountItem accountItem);
        void onEditAccount(AccountItem accountItem);
        void onDeleteAccount(AccountItem accountItem);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public AccountListAdapter(ManagedActivity activity, Listener listener) {
        this.accountItems = new ArrayList<>();
        this.activity = activity;
        this.listener = listener;
    }

    public List<AccountItem> getItems() {
        return accountItems;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(accountItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(accountItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AccountViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final AccountViewHolder accountHolder = (AccountViewHolder) holder;
        AccountItem accountItem = accountItems.get(position);

        accountHolder.color.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(accountItem.getAccount()));

        accountHolder.avatar.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(accountItem.getAccount()));

        accountHolder.name.setText(AccountManager.getInstance().getVerboseName(accountItem.getAccount()));
        accountHolder.status.setText(accountItem.getState().getStringId());

        accountHolder.enabledSwitch.setChecked(accountItem.isEnabled());

        accountHolder.ivAnchor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(accountHolder);
                }
                return false;
            }
        });

        if (showAnchors) accountHolder.ivAnchor.setVisibility(View.VISIBLE);
        else accountHolder.ivAnchor.setVisibility(View.GONE);
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
        ImageView ivAnchor;


        AccountViewHolder(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.item_account_color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            name = (TextView) itemView.findViewById(R.id.item_account_name);
            status = (TextView) itemView.findViewById(R.id.item_account_status);
            enabledSwitch = (SwitchCompat) itemView.findViewById(R.id.item_account_switch);
            ivAnchor = (ImageView) itemView.findViewById(R.id.ivAnchor);

            // I used on click listener instead of on checked change listener to avoid callback in onBindViewHolder
            enabledSwitch.setOnClickListener(this);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            AccountItem accountItem = accountItems.get(adapterPosition);

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
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onCreateContextMenu: no position");
                return;
            }

            AccountItem accountItem = accountItems.get(adapterPosition);

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
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onMenuItemClick: no position");
                return false;
            }

            AccountItem accountItem = accountItems.get(adapterPosition);

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
