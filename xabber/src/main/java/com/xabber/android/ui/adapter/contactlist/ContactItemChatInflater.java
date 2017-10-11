package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;

import java.io.File;
import java.util.Date;

/**
 * Created by valery.miller on 10.10.17.
 */

public class ContactItemChatInflater {

    private final Context context;
    private String outgoingMessageIndicatorText;

    ContactItemChatInflater(Context context) {
        this.context = context;
        outgoingMessageIndicatorText = context.getString(R.string.sender_is_you) + ": ";
    }

    void bindViewHolder(RosterChatViewHolder viewHolder, final AbstractContact contact) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        }

        viewHolder.accountColorIndicator.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.ivAvatar.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.ivAvatar.setVisibility(View.GONE);
        }

        viewHolder.tvContactName.setText(contact.getName());

        MessageManager messageManager = MessageManager.getInstance();

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            viewHolder.ivMucIndicator.setVisibility(View.VISIBLE);
            viewHolder.ivMucIndicator.setImageResource(R.drawable.ic_muc_indicator_black_16dp);
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.ivMucIndicator.setVisibility(View.VISIBLE);
            viewHolder.ivMucIndicator.setImageResource(R.drawable.ic_muc_private_chat_indicator_black_16dp);
        } else {
            viewHolder.ivMucIndicator.setVisibility(View.GONE);
        }

        String statusText;

        viewHolder.tvOutgoingMessage.setVisibility(View.GONE);

        MessageItem lastMessage = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser()).getLastMessage();

        if (lastMessage == null) {
            statusText = contact.getStatusText().trim();
        } else {
            if (lastMessage.getFilePath() != null) {
                statusText = new File(lastMessage.getFilePath()).getName();
            } else {
                statusText = lastMessage.getText().trim();
            }

            viewHolder.tvTime.setText(StringUtils
                    .getSmartTimeText(context, new Date(lastMessage.getTimestamp())));
            viewHolder.tvTime.setVisibility(View.VISIBLE);

            if (!lastMessage.isIncoming()) {
                viewHolder.tvOutgoingMessage.setText(outgoingMessageIndicatorText);
                viewHolder.tvOutgoingMessage.setVisibility(View.VISIBLE);
                viewHolder.tvOutgoingMessage.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));
            }
        }

        if (statusText.isEmpty()) {
            viewHolder.tvMessageText.setVisibility(View.GONE);
        } else {
            viewHolder.tvMessageText.setVisibility(View.VISIBLE);
            if (OTRManager.getInstance().isEncrypted(statusText)) {
                viewHolder.tvMessageText.setText(R.string.otr_not_decrypted_message);
                viewHolder.tvMessageText.
                        setTypeface(viewHolder.tvMessageText.getTypeface(), Typeface.ITALIC);
            } else {
                viewHolder.tvMessageText.setText(statusText);
                viewHolder.tvMessageText.setTypeface(Typeface.DEFAULT);
            }
        }

        viewHolder.ivStatus.setImageLevel(contact.getStatusMode().getStatusLevel());
    }

    void onAvatarClick(BaseEntity contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            intent = ContactActivity.createIntent(context, contact.getAccount(), contact.getUser());
        } else {
            intent = ContactEditActivity.createIntent(context, contact.getAccount(), contact.getUser());
        }
        context.startActivity(intent);
    }

}
