package com.xabber.android.ui.adapter.accountoptions;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;

public class AccountOptionsAdapter extends RecyclerView.Adapter<AccountOptionViewHolder>
        implements AccountOptionClickListener {

    private final AccountOption[] options;
    private final Listener listener;

    public interface Listener {
        void onAccountOptionClick(AccountOption option);
    }

    public AccountOptionsAdapter(AccountOption[] options, Listener listener) {
        this.options = options;
        this.listener = listener;
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
