package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.ui.activity.ConferenceSelectActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.StatusEditActivity;

/**
 * Created by valery.miller on 23.10.17.
 */

public class MainTitleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        android.widget.PopupMenu.OnMenuItemClickListener {

    final View accountColorIndicator;
    final ImageView ivAdd;
    final ImageView ivSetStatus;
    final TextView tvTitle;
    private final Context context;
    private Listener listener;

    interface Listener {
        void onStateChanged(ContactListAdapter.ChatListState state);
    }

    public MainTitleViewHolder(View itemView, Context context, Listener listener) {
        super(itemView);

        this.context = context;
        this.listener = listener;

        accountColorIndicator = itemView.findViewById(R.id.accountColorIndicator);
        ivAdd = (ImageView) itemView.findViewById(R.id.ivAdd);
        ivAdd.setOnClickListener(this);
        ivSetStatus = (ImageView) itemView.findViewById(R.id.ivSetStatus);
        ivSetStatus.setOnClickListener(this);
        tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
        tvTitle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivAdd:
                showToolbarPopup(ivAdd);
                break;
            case R.id.ivSetStatus:
                context.startActivity(StatusEditActivity.createIntent(context));
                break;
            case R.id.tvTitle:
                showTitlePopup(tvTitle);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                context.startActivity(ContactAddActivity.createIntent(context));
                return true;
            case R.id.action_join_conference:
                context.startActivity(ConferenceSelectActivity.createIntent(context));
                return true;
            case R.id.action_recent_chats:
                listener.onStateChanged(ContactListAdapter.ChatListState.recent);
                return true;
            case R.id.action_unread_chats:
                listener.onStateChanged(ContactListAdapter.ChatListState.unread);
                return true;
            case R.id.action_archived_chats:
                listener.onStateChanged(ContactListAdapter.ChatListState.archived);
                return true;
            default:
                return false;
        }
    }

    private void showToolbarPopup(View v) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_add_in_contact_list);
        popupMenu.show();
    }

    private void showTitlePopup(View v) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_chat_list);
        popupMenu.show();
    }
}
