package com.xabber.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.ColorManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.ContactEditor;
import com.xabber.android.ui.activity.ContactViewer;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;

public class ContactItemInflater {

    final Context context;
    private final int colorMucPrivateChatText;
    private final int colorMain;
    private final int activeChatTextColor;
    private final int activeChatBackgroundColor;
    private final int contactBackground;
    private final int contactSeparatorColor;
    private final int activeChatSeparatorColor;
    private final AccountPainter accountPainter;

    public ContactItemInflater(Context context) {
        this.context = context;
        accountPainter = new AccountPainter(context);

        colorMucPrivateChatText = ColorManager.getThemeColor(context, R.attr.contact_list_contact_muc_private_chat_name_text_color);
        colorMain = ColorManager.getThemeColor(context, R.attr.contact_list_contact_name_text_color);
        activeChatTextColor = ColorManager.getThemeColor(context, R.attr.contact_list_active_chat_text_color);
        activeChatBackgroundColor = ColorManager.getThemeColor(context, R.attr.contact_list_active_chat_background);
        contactBackground = ColorManager.getThemeColor(context, R.attr.contact_list_contact_background);
        contactSeparatorColor = ColorManager.getThemeColor(context, R.attr.contact_list_contact_separator);
        activeChatSeparatorColor = ColorManager.getThemeColor(context, R.attr.contact_list_active_chat_separator);
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact contact) {
        final View view;
        final ContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ContactListItemViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ContactListItemViewHolder) view.getTag();
        }

        if (contact.isConnected()) {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        }

        viewHolder.color.setImageDrawable(new ColorDrawable(accountPainter.getAccountMainColor(contact.getAccount())));
        viewHolder.color.setVisibility(View.VISIBLE);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAvatarClick(contact);
            }
        });

        viewHolder.name.setText(contact.getName());

        MessageManager messageManager = MessageManager.getInstance();
        if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(colorMucPrivateChatText);
        } else if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(activeChatTextColor);
        } else {
            viewHolder.name.setTextColor(colorMain);
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


        if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {

            AbstractChat chat = messageManager.getChat(contact.getAccount(), contact.getUser());

            statusText = chat.getLastText().trim();

            view.setBackgroundColor(activeChatBackgroundColor);
            viewHolder.separator.setBackgroundColor(activeChatSeparatorColor);

            if (!statusText.isEmpty()) {

                viewHolder.smallRightText.setText(StringUtils.getSmartTimeText(context, chat.getLastTime()));
                viewHolder.smallRightText.setVisibility(View.VISIBLE);

                if (!chat.isLastMessageIncoming()) {
                    viewHolder.outgoingMessageIndicator.setText(context.getString(R.string.sender_is_you) + ": ");
                    viewHolder.outgoingMessageIndicator.setVisibility(View.VISIBLE);
                    viewHolder.outgoingMessageIndicator.setTextColor(accountPainter.getAccountMainColor(contact.getAccount()));
                }
                viewHolder.smallRightIcon.setImageResource(R.drawable.ic_client_small);
                viewHolder.smallRightIcon.setVisibility(View.VISIBLE);
                viewHolder.smallRightIcon.setImageLevel(clientSoftware.ordinal());
                viewHolder.largeClientIcon.setVisibility(View.GONE);
            }
        } else {
            statusText = contact.getStatusText().trim();
            view.setBackgroundColor(contactBackground);
            viewHolder.separator.setBackgroundColor(contactSeparatorColor);
        }

        if (statusText.isEmpty()) {
            viewHolder.secondLineMessage.setVisibility(View.GONE);
        } else {
            viewHolder.secondLineMessage.setVisibility(View.VISIBLE);
            viewHolder.secondLineMessage.setText(Emoticons.getSmiledText(context, statusText, viewHolder.secondLineMessage));
        }

        viewHolder.statusIcon.setImageLevel(contact.getStatusMode().getStatusLevel());
        return view;
    }


    private void onAvatarClick(AbstractContact contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            intent = ContactViewer.createIntent(context, contact.getAccount(), contact.getUser());
        } else {
            intent = ContactEditor.createIntent(context, contact.getAccount(), contact.getUser());
        }
        context.startActivity(intent);
    }
}
