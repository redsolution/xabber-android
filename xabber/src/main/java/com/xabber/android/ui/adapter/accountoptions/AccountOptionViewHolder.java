package com.xabber.android.ui.adapter.accountoptions;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;

class AccountOptionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String LOG_TAG = AccountOptionViewHolder.class.getSimpleName();
    private final AccountOptionClickListener listener;
    ImageView icon;
    TextView title;
    TextView description;
    View separator;

    AccountOptionViewHolder(View itemView, AccountOptionClickListener listener) {
        super(itemView);

        this.listener = listener;

        icon = (ImageView) itemView.findViewById(R.id.account_option_icon);
        title = (TextView) itemView.findViewById(R.id.account_option_title);
        description = (TextView) itemView.findViewById(R.id.account_option_description);
        separator = itemView.findViewById(R.id.account_option_separator);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        listener.onAccountOptionClick(adapterPosition);
    }
}
