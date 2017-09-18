package com.xabber.android.ui.adapter.accountoptions;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.xaccount.XabberAccountManager;

public class AccountOptionsAdapter extends RecyclerView.Adapter<AccountOptionViewHolder>
        implements AccountOptionClickListener {

    private final AccountOption[] options;
    private final Listener listener;
    private final AccountItem accountItem;

    public interface Listener {
        void onAccountOptionClick(AccountOption option);
    }

    public AccountOptionsAdapter(AccountOption[] options, Listener listener, AccountItem accountItem) {
        this.options = options;
        this.listener = listener;
        this.accountItem = accountItem;
    }

    @Override
    public AccountOptionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_option, parent, false);

        return new AccountOptionViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(AccountOptionViewHolder holder, int position) {
        AccountOption accountOption = options[position];

        holder.icon.setImageResource(accountOption.getIconId());
        holder.title.setText(accountOption.getTitleId());
        holder.description.setText(accountOption.getDescription());

        if (position == 0) {
            holder.separator.setVisibility(View.GONE);
        } else {
            holder.separator.setVisibility(View.VISIBLE);
        }

        if ((XabberAccountManager.getInstance().getAccount() == null || accountItem.isSyncNotAllowed()) && position == 1) {
            holder.title.setEnabled(false);
            holder.icon.setImageResource(R.drawable.ic_sync_lightgrey);
            holder.description.setEnabled(false);
        } else {
            holder.title.setEnabled(true);
            holder.description.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return options.length;
    }

    @Override
    public void onAccountOptionClick(int adapterPosition) {
        listener.onAccountOptionClick(options[adapterPosition]);
    }
}
