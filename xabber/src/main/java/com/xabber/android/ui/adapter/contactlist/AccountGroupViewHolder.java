package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;


class AccountGroupViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnCreateContextMenuListener {

    private static final String LOG_TAG = AccountGroupViewHolder.class.getSimpleName();
    final ImageView ivAvatar;
    final View avatarView;
    final TextView tvAccountName;
    final TextView tvJid;
    final TextView tvStatus;
    final TextView tvContactCount;
    final ImageView smallRightIcon;
    final ImageView ivStatus;
    final ImageView ivMenu;
    final ImageView offlineShadow;
    final View accountColorIndicator;

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

        ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
        avatarView = view.findViewById(R.id.avatarView);
        ivAvatar.setOnClickListener(this);
        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvJid = (TextView) view.findViewById(R.id.tvJid);
        tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        smallRightIcon = (ImageView) view.findViewById(R.id.small_right_icon);
        tvContactCount = (TextView) view.findViewById(R.id.tvContactCount);
        ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
        ivStatus.setOnClickListener(this);
        offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        ivMenu = (ImageView) view.findViewById(R.id.ivMenu);
        ivMenu.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int adapterPosition = getAdapterPosition();
//        if (adapterPosition == RecyclerView.NO_POSITION) {
//            LogManager.w(LOG_TAG, "onClick: no position");
//            return;
//        }

        if (view.getId() == R.id.ivAvatar) {
            listener.onAccountAvatarClick(adapterPosition);
        } else if (view.getId() == R.id.ivMenu) {
            listener.onAccountMenuClick(adapterPosition, view);
        } else {
            listener.onAccountGroupClick(adapterPosition);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int adapterPosition = getAdapterPosition();
//        if (adapterPosition == RecyclerView.NO_POSITION) {
//            LogManager.w(LOG_TAG, "onCreateContextMenu: no position");
//            return;
//        }

        listener.onAccountGroupCreateContextMenu(adapterPosition, menu);
    }
}
