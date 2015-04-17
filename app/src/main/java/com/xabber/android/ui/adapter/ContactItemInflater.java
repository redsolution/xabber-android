package com.xabber.android.ui.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.ContactEditor;
import com.xabber.android.ui.ContactViewer;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

public class ContactItemInflater {

    final Context context;
    private int[] accountMainColors;
    private final int elevation;

    public ContactItemInflater(Context context) {
        this.context = context;
        accountMainColors = context.getResources().getIntArray(R.array.account_action_bar);
        elevation = context.getResources().getDimensionPixelSize(R.dimen.contact_elevation);
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact contact) {
        final View view;
        final ContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ContactListItemViewHolder(view);
            viewHolder.statusIconSeparator.setVisibility(View.INVISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setElevation(elevation);
            }
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

        int colorLevel = contact.getColorLevel();
        viewHolder.color.setImageDrawable(new ColorDrawable(accountMainColors[colorLevel]));
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
        String statusText;

        viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);

        ClientSoftware clientSoftware = contact.getClientSoftware();

        MessageManager messageManager = MessageManager.getInstance();

        if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {

            AbstractChat chat = messageManager.getChat(contact.getAccount(), contact.getUser());

            statusText = chat.getLastText();

            viewHolder.smallRightText.setText(StringUtils.getSmartTimeText(context, chat.getLastTime()));
            viewHolder.smallRightText.setVisibility(View.VISIBLE);

            if (!chat.isLastMessageIncoming()) {
                viewHolder.outgoingMessageIndicator.setText(context.getString(R.string.sender_is_you) + ": ");
                viewHolder.outgoingMessageIndicator.setVisibility(View.VISIBLE);
                viewHolder.outgoingMessageIndicator.setTextColor(accountMainColors[colorLevel]);

            }
            view.setBackgroundColor(context.getResources().getColor(R.color.grey_50));
            viewHolder.smallRightIcon.setImageResource(R.drawable.ic_client_small);
            viewHolder.smallRightIcon.setVisibility(View.VISIBLE);
            viewHolder.smallRightIcon.setImageLevel(clientSoftware.ordinal());
            viewHolder.largeClientIcon.setVisibility(View.GONE);
        } else {
            statusText = contact.getStatusText().trim();
            if (statusText.isEmpty()) {
                statusText = context.getString(contact.getStatusMode().getStringID());
            }
            viewHolder.smallRightText.setVisibility(View.GONE);
            view.setBackgroundColor(context.getResources().getColor(R.color.grey_300));
            viewHolder.smallRightIcon.setVisibility(View.GONE);
            viewHolder.largeClientIcon.setVisibility(View.VISIBLE);
            viewHolder.largeClientIcon.setImageLevel(clientSoftware.ordinal());
        }

        viewHolder.secondLineMessage.setText(statusText.trim());

        viewHolder.statusIcon.setImageLevel(contact.getStatusMode().getStatusLevel());
        return view;
    }

    private void onAvatarClick(AbstractContact contact) {
        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            context.startActivity(ContactViewer.createIntent(context,
                    contact.getAccount(), contact.getUser()));
        } else {
            context.startActivity(ContactEditor.createIntent(context,
                    contact.getAccount(), contact.getUser()));
        }
    }
}
