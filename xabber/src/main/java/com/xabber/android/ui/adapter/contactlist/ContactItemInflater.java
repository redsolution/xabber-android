package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO;
import com.xabber.android.ui.color.ColorManager;

class ContactItemInflater {

    private final Context context;
    private String outgoingMessageIndicatorText;

    ContactItemInflater(Context context) {
        this.context = context;
        outgoingMessageIndicatorText = context.getString(R.string.sender_is_you) + ": ";
    }

    void bindViewHolder(ContactListItemViewHolder viewHolder, final ContactVO viewObject) {

        if (viewObject.isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);

        viewHolder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatarView.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(viewObject.getAvatar());
        } else viewHolder.avatarView.setVisibility(View.GONE);

        viewHolder.tvContactName.setText(viewObject.getName());

        if (viewObject.getMucIndicatorLevel() == 0)
            viewHolder.ivMucIndicator.setVisibility(View.GONE);
        else {
            viewHolder.ivMucIndicator.setVisibility(View.VISIBLE);
            viewHolder.ivMucIndicator.setImageLevel(viewObject.getMucIndicatorLevel());
        }

        if (viewObject.getStatusLevel() == 6) {
            viewHolder.ivStatus.setVisibility(View.INVISIBLE);
            //viewHolder.ivDevice.setVisibility(View.INVISIBLE);
            viewHolder.tvStatus.setTextColor(ColorManager.getInstance().getColorContactSecondLine());
        } else {
            viewHolder.ivStatus.setVisibility(View.VISIBLE);
            //viewHolder.ivDevice.setVisibility(View.VISIBLE);
            viewHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_color_in_contact_list_online));
        }
        viewHolder.ivStatus.setImageLevel(viewObject.getStatusLevel());

        String statusText = viewObject.getStatus();
        if (statusText.isEmpty()) statusText = context.getString(viewObject.getStatusId());

        viewHolder.tvStatus.setText(statusText);

        if (viewObject.getUnreadCount() > 0) {
            viewHolder.tvUnreadCount.setText(String.valueOf(viewObject.getUnreadCount()));
            viewHolder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else viewHolder.tvUnreadCount.setVisibility(View.GONE);

        // notification mute
        Resources resources = context.getResources();
        switch (viewObject.getNotificationMode()) {
            case enabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        resources.getDrawable(R.drawable.ic_unmute), null);
                break;
            case disabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        resources.getDrawable(R.drawable.ic_mute), null);
                break;
            default:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, null, null);
        }

        if (viewObject.isMute())
            viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
                    resources.getColor(R.color.grey_500),
                    PorterDuff.Mode.SRC_IN);
        else viewHolder.tvUnreadCount.getBackground().mutate().clearColorFilter();
    }

    void onAvatarClick(ContactVO contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccountJid(), contact.getUserJid())) {
            intent = ContactActivity.createIntent(context, contact.getAccountJid(), contact.getUserJid());
        } else {
            intent = ContactEditActivity.createIntent(context, contact.getAccountJid(), contact.getUserJid());
        }
        context.startActivity(intent);
    }
}
