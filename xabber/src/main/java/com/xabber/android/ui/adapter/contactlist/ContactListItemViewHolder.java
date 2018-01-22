package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;

class ContactListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

    private static final String LOG_TAG = ContactListItemViewHolder.class.getSimpleName();
    final View accountColorIndicator;
    final ImageView ivAvatar;
    final TextView tvContactName;
    final TextView tvStatus;
    final ImageView ivOnlyStatus;
    final ImageView ivStatus;
    final ImageView offlineShadow;
    final TextView tvUnreadCount;
    private final ContactClickListener listener;


    interface ContactClickListener {
        void onContactClick(int adapterPosition);
        void onContactAvatarClick(int adapterPosition);
        void onContactButtonClick(int adapterPosition);
        void onContactCreateContextMenu(int adapterPosition, ContextMenu menu);
    }

    ContactListItemViewHolder(View view, ContactClickListener listener) {
        super(view);
        this.listener = listener;

        itemView.setOnClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
        ivAvatar.setOnClickListener(this);
        tvContactName = (TextView) view.findViewById(R.id.tvContactName);
        tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        ivOnlyStatus = (ImageView) view.findViewById(R.id.ivOnlyStatus);
        ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
        offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        tvUnreadCount = (TextView) view.findViewById(R.id.tvUnreadCount);
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        if (v.getId() == R.id.ivAvatar) {
            listener.onContactAvatarClick(adapterPosition);
        } else {
            listener.onContactClick(adapterPosition);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onCreateContextMenu: no position");
            return;
        }

        listener.onContactCreateContextMenu(adapterPosition, menu);
    }

}
