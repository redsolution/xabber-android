package com.xabber.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends BaseAdapter {

    private List<AbstractChat> chats;

    private final AbstractAvatarInflaterHelper avatarInflaterHelper;

    private final Context context;


    public ChatListAdapter(Context context) {
        this.context = context;
        chats = new ArrayList<>();
        avatarInflaterHelper = AbstractAvatarInflaterHelper.createAbstractContactInflaterHelper();
    }

    public void updateChats(List<AbstractChat> chats) {
        this.chats.clear();
        this.chats.addAll(chats);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chats.size();
    }

    @Override
    public Object getItem(int position) {
        return chats.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.chat_list_item, parent, false);
        } else {
            view = convertView;
        }

        final AbstractChat abstractChat = (AbstractChat) getItem(position);
        final AbstractContact abstractContact = RosterManager.getInstance()
                .getBestContact(abstractChat.getAccount(), abstractChat.getUser());

        ((TextView) view.findViewById(R.id.name)).setText(abstractContact.getName());
        ((ImageView) view.findViewById(R.id.color)).setImageLevel(abstractContact.getColorLevel());

        final ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
        avatarView.setImageDrawable(abstractContact.getAvatar());
        avatarInflaterHelper.updateAvatar(avatarView, abstractContact);

        final TextView textView = (TextView) view.findViewById(R.id.text);

        String statusText = MessageManager.getInstance().getLastText(
                abstractContact.getAccount(), abstractContact.getUser());
        textView.setText(statusText);

        boolean newMessages = NotificationManager.getInstance()
                .getNotificationMessageCount(abstractChat.getAccount(), abstractChat.getUser()) > 0;
        textView.setTextAppearance(context, newMessages ? R.style.ChatList_Notification
                        : R.style.ChatList_Normal);
        return view;
    }
}
