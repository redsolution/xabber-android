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
import com.xabber.android.data.extension.capability.ClientSoftware;
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

class ContactItemInflater {

    private final Context context;
    private String outgoingMessageIndicatorText;

    ContactItemInflater(Context context) {
        this.context = context;
        outgoingMessageIndicatorText = context.getString(R.string.sender_is_you) + ": ";
    }

    void bindViewHolder(ContactListItemViewHolder viewHolder, final AbstractContact contact) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        }

        viewHolder.color.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));
        viewHolder.color.setVisibility(View.VISIBLE);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.name.setText(contact.getName());

        MessageManager messageManager = MessageManager.getInstance();
        if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMucPrivateChatText());
        } else if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getActiveChatTextColor());
        } else {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMain());
        }

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_indicator_black_16dp);
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_private_chat_indicator_black_16dp);
        } else {
            viewHolder.mucIndicator.setVisibility(View.GONE);
        }

        String statusText;

        viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);



        viewHolder.smallRightText.setVisibility(View.GONE);
        viewHolder.smallRightIcon.setVisibility(View.GONE);

        ClientSoftware clientSoftware = contact.getClientSoftware();
        if (clientSoftware == ClientSoftware.unknown) {
            viewHolder.largeClientIcon.setVisibility(View.GONE);
        } else {
            viewHolder.largeClientIcon.setVisibility(View.VISIBLE);
            viewHolder.largeClientIcon.setImageLevel(clientSoftware.ordinal());
        }

        MessageItem lastMessage = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser()).getLastMessage();

        if (lastMessage == null) {
            statusText = contact.getStatusText().trim();
        } else {
            if (lastMessage.getFilePath() != null) {
                statusText = new File(lastMessage.getFilePath()).getName();
            } else {
                statusText = lastMessage.getText().trim();
            }

            viewHolder.smallRightText.setText(StringUtils
                    .getSmartTimeText(context, new Date(lastMessage.getTimestamp())));
            viewHolder.smallRightText.setVisibility(View.VISIBLE);

            if (!lastMessage.isIncoming()) {
                viewHolder.outgoingMessageIndicator.setText(outgoingMessageIndicatorText);
                viewHolder.outgoingMessageIndicator.setVisibility(View.VISIBLE);
                viewHolder.outgoingMessageIndicator.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));
            }
            viewHolder.smallRightIcon.setImageResource(R.drawable.ic_client_small);
            viewHolder.smallRightIcon.setVisibility(View.VISIBLE);

            viewHolder.smallRightIcon.setImageLevel(clientSoftware.ordinal());
            viewHolder.largeClientIcon.setVisibility(View.GONE);
        }

        if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {

            viewHolder.itemView.setBackgroundColor(ColorManager.getInstance().getActiveChatBackgroundColor());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getActiveChatSeparatorColor());
            viewHolder.largeClientIcon.setColorFilter(ColorManager.getInstance().getActiveChatLargeClientIconColor());
            viewHolder.smallRightIcon.setColorFilter(ColorManager.getInstance().getActiveChatLargeClientIconColor());
            viewHolder.smallRightText.setTextColor(ColorManager.getInstance().getActiveChatLargeClientIconColor());
        } else {
            viewHolder.itemView.setBackgroundColor(ColorManager.getInstance().getContactBackground());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getContactSeparatorColor());
            viewHolder.largeClientIcon.setColorFilter(ColorManager.getInstance().getContactLargeClientIconColor());
            viewHolder.smallRightIcon.setColorFilter(ColorManager.getInstance().getContactLargeClientIconColor());
            viewHolder.smallRightText.setTextColor(ColorManager.getInstance().getContactLargeClientIconColor());
        }

        if (statusText.isEmpty()) {
            viewHolder.secondLineMessage.setVisibility(View.GONE);
        } else {
            viewHolder.secondLineMessage.setVisibility(View.VISIBLE);
            if (OTRManager.getInstance().isEncrypted(statusText)) {
                viewHolder.secondLineMessage.setText(R.string.otr_not_decrypted_message);
                viewHolder.secondLineMessage.
                        setTypeface(viewHolder.secondLineMessage.getTypeface(), Typeface.ITALIC);
            } else {
                viewHolder.secondLineMessage.setText(statusText);
                viewHolder.secondLineMessage.setTypeface(Typeface.DEFAULT);
            }
        }

        viewHolder.statusIcon.setImageLevel(contact.getStatusMode().getStatusLevel());
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
