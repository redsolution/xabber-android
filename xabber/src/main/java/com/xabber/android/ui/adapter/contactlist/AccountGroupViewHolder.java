package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;


class AccountGroupViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnCreateContextMenuListener {

    private static final String LOG_TAG = AccountGroupViewHolder.class.getSimpleName();
    final ImageView avatar;
    final TextView name;
    final TextView secondLineMessage;
    final TextView smallRightText;
    final ImageView smallRightIcon;
    final ImageView statusIcon;
    final ImageView offlineShadow;

    private final AccountGroupClickListener listener;

    interface AccountGroupClickListener {
        void onAccountAvatarClick(int adapterPosition);
        void onAccountMenuClick(int adapterPosition, View view);
        void onAccountGroupClick(int adapterPosition);
        void onAccountGroupCreateContextMenu(int adapterPosition, ContextMenu menu);
    }

    AccountGroupViewHolder(View view, AccountGroupClickListener listener) {
        super(view);

        this.listener = listener;
        itemView.setOnClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        avatar = (ImageView) view.findViewById(R.id.avatar);
        avatar.setOnClickListener(this);
        name = (TextView) view.findViewById(R.id.contact_list_item_name);
        secondLineMessage = (TextView) view.findViewById(R.id.second_line_message);
        smallRightIcon = (ImageView) view.findViewById(R.id.small_right_icon);
        smallRightText = (TextView) view.findViewById(R.id.small_right_text);
        statusIcon = (ImageView) view.findViewById(R.id.contact_list_item_status_icon);
        statusIcon.setOnClickListener(this);
        offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
    }

    @Override
    public void onClick(View view) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        if (view.getId() == R.id.avatar) {
            listener.onAccountAvatarClick(adapterPosition);
        } else if (view.getId() == R.id.contact_list_item_status_icon) {
            listener.onAccountMenuClick(adapterPosition, view);
        } else {
            listener.onAccountGroupClick(adapterPosition);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onCreateContextMenu: no position");
            return;
        }

        listener.onAccountGroupCreateContextMenu(adapterPosition, menu);
    }
}
