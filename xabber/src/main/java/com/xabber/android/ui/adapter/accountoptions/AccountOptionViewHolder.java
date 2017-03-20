package com.xabber.android.ui.adapter.accountoptions;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;

class AccountOptionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final AccountOptionClickListener listener;
    ImageView icon;
    TextView title;
    TextView description;

    AccountOptionViewHolder(View itemView, AccountOptionClickListener listener) {
        super(itemView);

        this.listener = listener;

        icon = (ImageView) itemView.findViewById(R.id.account_option_icon);
        title = (TextView) itemView.findViewById(R.id.account_option_title);
        description = (TextView) itemView.findViewById(R.id.account_option_description);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        listener.onAccountOptionClick(getAdapterPosition());
    }
}
