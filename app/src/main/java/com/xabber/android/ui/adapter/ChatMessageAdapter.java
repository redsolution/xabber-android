/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsDivide;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    private static final int VIEW_TYPE_HINT = 1;
    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;

    private final Context context;
    private final Message.MessageClickListener messageClickListener;
    private String account;
    private String user;
    private boolean isMUC;
    private List<MessageItem> messages;

    /**
     * Message font appearance.
     */
    private final int appearanceStyle;

    /**
     * Divider between header and body.
     */
    private final String divider;

    /**
     * Text with extra information.
     */
    private String hint;

    public static class HintMessage extends RecyclerView.ViewHolder {

        public TextView info;

        public HintMessage(View itemView) {
            super(itemView);
            info = (TextView) itemView.findViewById(R.id.info);
        }
    }

    public static class ActionMessage extends RecyclerView.ViewHolder {

        public TextView actionMessage;

        public ActionMessage(View itemView) {
            super(itemView);
            actionMessage = (TextView) itemView.findViewById(R.id.action_message_text);
        }
    }

    public static abstract class Message extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView messageText;
        public TextView messageTime;

        MessageClickListener onClickListener;

        public Message(View itemView, MessageClickListener onClickListener) {
            super(itemView);
            this.onClickListener = onClickListener;

            messageText = (TextView) itemView.findViewById(R.id.message_text);
            messageTime = (TextView) itemView.findViewById(R.id.message_time);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onClickListener.onMessageClick(v, getPosition());
        }

        public interface MessageClickListener {
            void onMessageClick(View caller, int position);
        }

    }

    public static class IncomingMessage extends Message {

        public ImageView avatar;

        public IncomingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
        }
    }

    public static class OutgoingMessage extends Message {

        public ImageView statusIcon;

        public OutgoingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            statusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);
        }
    }

    public ChatMessageAdapter(Context context, String account, String user, Message.MessageClickListener messageClickListener) {
        this.context = context;
        messages = Collections.emptyList();
        this.account = account;
        this.user = user;
        this.messageClickListener = messageClickListener;

        isMUC = MUCManager.getInstance().hasRoom(account, user);
        hint = null;
        appearanceStyle = SettingsManager.chatsAppearanceStyle();
        ChatsDivide chatsDivide = SettingsManager.chatsDivide();
        if (chatsDivide == ChatsDivide.always || (chatsDivide == ChatsDivide.portial
                && !context.getResources().getBoolean(R.bool.landscape))) {
            divider = "\n";
        } else {
            divider = " ";
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HINT:
                return new HintMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_info, parent, false));

            case VIEW_TYPE_ACTION_MESSAGE:
                return new ActionMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_action_message, parent, false));

            case VIEW_TYPE_INCOMING_MESSAGE:
                return new IncomingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_incoming_message, parent, false), messageClickListener);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_outgoing_message, parent, false), messageClickListener);
            default:
                return null;
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int viewType = getItemViewType(position);

        MessageItem messageItem = getMessageItem(position);

        switch (viewType) {
            case VIEW_TYPE_HINT:
                ((HintMessage)holder).info.setText(hint);
                break;

            case VIEW_TYPE_ACTION_MESSAGE:
                ChatAction action = messageItem.getAction();
                String time = StringUtils.getSmartTimeText(context, messageItem.getTimestamp());
                ((ActionMessage)holder).actionMessage.setText(time + ": "
                        + action.getText(context, messageItem.getResource(), messageItem.getSpannable().toString()));
                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                setUpMessage(messageItem, (Message) holder);
                setUpAvatar(messageItem, (IncomingMessage) holder);
                break;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                setUpMessage(messageItem, (Message) holder);
                setStatusIcon(messageItem, (OutgoingMessage) holder);
                break;
        }

    }

    @Override
    public int getItemCount() {
        return messages.size() + 1;
    }

    public MessageItem getMessageItem(int position) {
        if (position < messages.size()) {
            return messages.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= messages.size()) {
            return VIEW_TYPE_HINT;
        }

        MessageItem messageItem = getMessageItem(position);
        if (messageItem.getAction() != null) {
            return VIEW_TYPE_ACTION_MESSAGE;
        }

        return messageItem.isIncoming() ? VIEW_TYPE_INCOMING_MESSAGE : VIEW_TYPE_OUTGOING_MESSAGE;
    }

    private void setUpMessage(MessageItem messageItem, Message message) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        final String resource = messageItem.getResource();

        if (isMUC) {
            append(builder, resource, new TextAppearanceSpan(context, R.style.ChatHeader_Time));
            append(builder, divider, new TextAppearanceSpan(context, R.style.ChatHeader));
        }

        Date delayTimestamp = messageItem.getDelayTimestamp();

        if (messageItem.isUnencypted()) {
            append(builder, context.getString(R.string.otr_unencrypted_message),
                    new TextAppearanceSpan(context, R.style.ChatHeader_Delay));
            append(builder, divider, new TextAppearanceSpan(context, R.style.ChatHeader));
        }

        Spannable text = messageItem.getSpannable();
        if (messageItem.getTag() == null) {
            builder.append(text);
        } else {
            append(builder, text, new TextAppearanceSpan(context, R.style.ChatRead));
        }

        message.messageText.setTextAppearance(context, appearanceStyle);
        message.messageText.setText(builder);
        message.messageText.getBackground().setLevel(AccountManager.getInstance().getColorLevel(account));

        String time = StringUtils.getSmartTimeText(context, messageItem.getTimestamp());

        if (delayTimestamp != null) {
            String delay = context.getString(messageItem.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getSmartTimeText(context, delayTimestamp));
            time += " (" + delay + ")";
        }

        message.messageTime.setText(time);
    }

    private void setStatusIcon(MessageItem messageItem, OutgoingMessage message) {
        message.statusIcon.setVisibility(View.VISIBLE);

        int messageIcon = R.drawable.ic_message_delivered_18dp;
        if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_18dp;
        } else if (!messageItem.isSent()) {
            messageIcon = R.drawable.ic_message_not_sent_18dp;
        } else if (!messageItem.isDelivered()) {
            message.statusIcon.setVisibility(View.INVISIBLE);
        }

        message.statusIcon.setImageResource(messageIcon);
    }

    private void append(SpannableStringBuilder builder, CharSequence text, CharacterStyle span) {
        int start = builder.length();
        builder.append(text);
        builder.setSpan(span, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setUpAvatar(MessageItem messageItem, IncomingMessage message) {
        if (SettingsManager.chatsShowAvatars()) {
            final String account = messageItem.getChat().getAccount();
            final String user = messageItem.getChat().getUser();
            final String resource = messageItem.getResource();

            message.avatar.setVisibility(View.VISIBLE);
            if ((isMUC && MUCManager.getInstance().getNickname(account, user).equalsIgnoreCase(resource))) {
                message.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (isMUC) {
                    if ("".equals(resource)) {
                        message.avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                    } else {
                        message.avatar.setImageDrawable(AvatarManager.getInstance().getOccupantAvatar(user + "/" + resource));
                    }
                } else {
                    message.avatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user));
                }
            }
        } else {
            message.avatar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onChange() {
        messages = new ArrayList<>(MessageManager.getInstance().getMessages(account, user));
        hint = getHint();
        notifyDataSetChanged();
    }

    /**
     * @return New hint.
     */
    private String getHint() {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        boolean online = accountItem != null && accountItem.getState().isConnected();
        final AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);
        if (!online) {
            if (abstractContact instanceof RoomContact) {
                return context.getString(R.string.muc_is_unavailable);
            } else {
                return context.getString(R.string.account_is_offline);
            }
        } else if (!abstractContact.getStatusMode().isOnline()) {
            if (abstractContact instanceof RoomContact) {
                return context.getString(R.string.muc_is_unavailable);
            } else {
                return context.getString(R.string.contact_is_offline, abstractContact.getName());
            }
        }
        return null;
    }
}
