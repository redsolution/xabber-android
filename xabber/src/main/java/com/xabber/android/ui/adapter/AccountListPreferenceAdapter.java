package com.xabber.android.ui.adapter;

/**
 * Created by valery.miller on 29.08.17.
 */

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.jid.BareJid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountListPreferenceAdapter extends RecyclerView.Adapter {

    @SuppressWarnings("WeakerAccess")
    static final String LOG_TAG = AccountListAdapter.class.getSimpleName();
    @SuppressWarnings("WeakerAccess")
    List<AccountItem> accountItems;
    @SuppressWarnings("WeakerAccess")
    Listener listener;
    Activity activity;

    public interface Listener {
        void onAccountClick(AccountJid account);
        void onEditAccountStatus(AccountItem accountItem);
        void onEditAccount(AccountItem accountItem);
        void onDeleteAccount(AccountItem accountItem);
    }

    public AccountListPreferenceAdapter(Activity activity, Listener listener) {
        this.accountItems = new ArrayList<>();
        this.listener = listener;
        this.activity = activity;
    }

    public void setAccountItems(List<AccountItem> accountItems) {
        this.accountItems = accountItems;

        List<XMPPAccountSettings> accountSettings = new ArrayList<>();
        accountSettings = XabberAccountManager.getInstance().getXmppAccounts();

        for (AccountItem account : accountItems) {
            for (XMPPAccountSettings set : accountSettings) {
                BareJid jid = account.getAccount().getFullJid().asBareJid();
                if (jid != null) {
                    if (set.getJid().equals(jid.toString()))
                        account.setOrder(set.getOrder());
                }
            }
        }
        Collections.sort(accountItems);
        notifyDataSetChanged();
    }

    public List<AccountItem> getItems() {
        return accountItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AccountViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_for_preference, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final AccountViewHolder accountHolder = (AccountViewHolder) holder;
        AccountItem accountItem = accountItems.get(position);

        accountHolder.avatar.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(accountItem.getAccount()));

        accountHolder.avatar.setBorderColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(accountItem.getAccount()));

        accountHolder.name.setText(AccountManager.getInstance().getVerboseName(accountItem.getAccount()));

        accountHolder.status.setText(accountItem.getState().getStringId());

        accountHolder.enabledSwitch.setChecked(accountItem.isEnabled());

    }

    @Override
    public int getItemCount() {
        return accountItems.size();
    }

    private class AccountViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        CircleImageView avatar;
        TextView name;
        TextView status;
        SwitchCompat enabledSwitch;


        AccountViewHolder(View itemView) {
            super(itemView);
            avatar = (CircleImageView) itemView.findViewById(R.id.avatar);
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

