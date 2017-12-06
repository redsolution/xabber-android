package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;

/**
 * Created by valery.miller on 06.12.17.
 */

public class ExtContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener{

    private static final String LOG_TAG = ContactListItemViewHolder.class.getSimpleName();

    final View accountColorIndicator;
    final View avatarView;
    final ImageView ivAvatar;
    final ImageView ivStatus;
    final TextView tvContactName;
    final TextView tvOutgoingMessage;
    final TextView tvMessageText;
    final TextView tvTime;
    final ImageView ivMessageStatus;
    final ImageView offlineShadow;
    final ImageView ivMucIndicator;
    final TextView tvUnreadCount;
    final TextView tvAction;
    final ImageView ivActionIcon;
    final TextView tvActionLeft;
    final ImageView ivActionIconLeft;
    public final RelativeLayout foregroundView;
    public final RelativeLayout backgroundView;
    public LinearLayout viewActionRight;
    public LinearLayout viewActionLeft;
    public RelativeLayout buttonView;
    public Button btnListAction;

    private final ContactListItemViewHolder.ContactClickListener listener;


    ExtContactViewHolder(View view, ContactListItemViewHolder.ContactClickListener listener) {
        super(view);
        this.listener = listener;

        itemView.setOnClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        avatarView = view.findViewById(R.id.avatarView);
        ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
        ivAvatar.setOnClickListener(this);
        ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
        tvContactName = (TextView) view.findViewById(R.id.tvContactName);
        tvOutgoingMessage = (TextView) view.findViewById(R.id.tvOutgoingMessage);
        tvMessageText = (TextView) view.findViewById(R.id.tvMessageText);
        tvTime = (TextView) view.findViewById(R.id.tvTime);
        ivMessageStatus = (ImageView) view.findViewById(R.id.ivMessageStatus);
        offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        ivMucIndicator = (ImageView) view.findViewById(R.id.ivMucIndicator);
        tvUnreadCount = (TextView) view.findViewById(R.id.tvUnreadCount);
        foregroundView = (RelativeLayout) view.findViewById(R.id.foregroundView);
        backgroundView = (RelativeLayout) view.findViewById(R.id.backgroundView);
        viewActionRight = (LinearLayout) view.findViewById(R.id.viewActionRight);
        viewActionLeft = (LinearLayout) view.findViewById(R.id.viewActionLeft);
        tvAction = (TextView) view.findViewById(R.id.tvAction);
        ivActionIcon = (ImageView) view.findViewById(R.id.ivActionIcon);
        tvActionLeft = (TextView) view.findViewById(R.id.tvActionLeft);
        ivActionIconLeft = (ImageView) view.findViewById(R.id.ivActionIconLeft);
        buttonView = (RelativeLayout) view.findViewById(R.id.buttonView);
        btnListAction = (Button) view.findViewById(R.id.btnListAction);
        btnListAction.setOnClickListener(this);
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
        } else if (v.getId() == R.id.btnListAction) {
            listener.onContactButtonClick(adapterPosition);
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
