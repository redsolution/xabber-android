package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

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
    private final Context context;

    public MainTitleViewHolder(View itemView, Context context) {
        super(itemView);

        this.context = context;
        accountColorIndicator = itemView.findViewById(R.id.accountColorIndicator);
        ivAdd = (ImageView) itemView.findViewById(R.id.ivAdd);
        ivAdd.setOnClickListener(this);
        ivSetStatus = (ImageView) itemView.findViewById(R.id.ivSetStatus);
        ivSetStatus.setOnClickListener(this);
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
}
