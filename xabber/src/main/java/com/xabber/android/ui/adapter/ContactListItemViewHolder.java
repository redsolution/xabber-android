package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;

class ContactListItemViewHolder extends RecyclerView.ViewHolder {

    final ImageView color;
    final ImageView avatar;
    final TextView name;
    final TextView outgoingMessageIndicator;
    final TextView secondLineMessage;
    final TextView smallRightText;
    final ImageView smallRightIcon;
    final ImageView largeClientIcon;
    final View statusIconSeparator;
    final ImageView statusIcon;
    final ImageView offlineShadow;
    final ImageView mucIndicator;

    public ContactListItemViewHolder(View view) {
        super(view);

        color = (ImageView) view.findViewById(R.id.account_color_indicator);
        avatar = (ImageView) view.findViewById(R.id.avatar);
        name = (TextView) view.findViewById(R.id.contact_list_item_name);
        outgoingMessageIndicator = (TextView) view.findViewById(R.id.outgoing_message_indicator);
        secondLineMessage = (TextView) view.findViewById(R.id.second_line_message);
        smallRightIcon = (ImageView) view.findViewById(R.id.small_right_icon);
        smallRightText = (TextView) view.findViewById(R.id.small_right_text);
        largeClientIcon = (ImageView) view.findViewById(R.id.client_icon_large);
        statusIconSeparator = view.findViewById(R.id.status_icon_separator);
        statusIcon = (ImageView) view.findViewById(R.id.contact_list_item_status_icon);
        offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        mucIndicator = (ImageView) view.findViewById(R.id.contact_list_item_muc_indicator);
    }
}
