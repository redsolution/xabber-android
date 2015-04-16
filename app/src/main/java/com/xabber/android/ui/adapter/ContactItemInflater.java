package com.xabber.android.ui.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.ContactEditor;
import com.xabber.android.ui.ContactViewer;
import com.xabber.androiddev.R;

public class ContactItemInflater {
    static class ContactViewHolder {

        final ImageView color;
        final ImageView avatar;
        final TextView name;
        final TextView status;
        final ImageView offlineShadow;
        final ImageView statusMode;
        final ImageView clientSoftware;

        public ContactViewHolder(View view) {
            color = (ImageView) view.findViewById(R.id.color);
            avatar = (ImageView) view.findViewById(R.id.avatar);
            name = (TextView) view.findViewById(R.id.name);
            status = (TextView) view.findViewById(R.id.status);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            statusMode = (ImageView) view.findViewById(R.id.status_icon);
            clientSoftware = (ImageView) view.findViewById(R.id.client_software);
        }
    }

    final Context context;
    private int[] accountMainColors;

    public ContactItemInflater(Context context) {
        this.context = context;
        accountMainColors = context.getResources().getIntArray(R.array.account_action_bar);
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact abstractContact) {
        final View view;
        final ContactViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.base_contact_item, parent, false);
            viewHolder = new ContactViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ContactViewHolder) view.getTag();
        }

        if (abstractContact.isConnected()) {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        }

        int colorLevel = abstractContact.getColorLevel();
        viewHolder.color.setImageDrawable(new ColorDrawable(accountMainColors[colorLevel]));

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(abstractContact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MUCManager.getInstance().hasRoom(abstractContact.getAccount(), abstractContact.getUser())) {
                    context.startActivity(ContactViewer.createIntent(context,
                            abstractContact.getAccount(), abstractContact.getUser()));
                } else {
                    context.startActivity(ContactEditor.createIntent(context,
                            abstractContact.getAccount(), abstractContact.getUser()));
                }
            }
        });

        viewHolder.name.setText(abstractContact.getName());
        String statusText;

        if (MessageManager.getInstance()
                .hasActiveChat(abstractContact.getAccount(), abstractContact.getUser())) {
            statusText =  MessageManager.getInstance()
                    .getLastText(abstractContact.getAccount(), abstractContact.getUser());
        } else {
            statusText = abstractContact.getStatusText();
        }

        statusText = statusText.trim();

        if ("".equals(statusText)) {
            viewHolder.status.setVisibility(View.GONE);
        } else {
            viewHolder.status.setText(statusText);
            viewHolder.status.setVisibility(View.VISIBLE);
        }

        viewHolder.statusMode.setImageLevel(abstractContact.getStatusMode().getStatusLevel());

        ClientSoftware clientSoftware = abstractContact.getClientSoftware();
        if (clientSoftware == ClientSoftware.unknown) {
            viewHolder.clientSoftware.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.clientSoftware.setVisibility(View.VISIBLE);
            viewHolder.clientSoftware.setImageLevel(clientSoftware.ordinal());
        }

        if (MessageManager.getInstance().hasActiveChat(abstractContact.getAccount(), abstractContact.getUser())) {
            view.setBackgroundColor(context.getResources().getColor(R.color.grey_50));
        } else {
            view.setBackgroundColor(context.getResources().getColor(R.color.grey_300));
        }

        return view;
    }
}
