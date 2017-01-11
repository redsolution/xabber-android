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
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;

import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.Date;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;


public class ChatMessageAdapter extends RealmRecyclerViewAdapter<MessageItem, ChatMessageAdapter.BasicMessage> {

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_HINT = 1;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;

    private final Context context;
    private final Message.MessageClickListener messageClickListener;
    /**
     * Message font appearance.
     */
    private final int appearanceStyle;
    private boolean isMUC;
    private Resourcepart mucNickname;

    /**
     * Text with extra information.
     */
    private Listener listener;

    private AccountJid account;
    private UserJid user;
    private int prevItemCount;
    private long lastUpdateTimeMillis;

    public ChatMessageAdapter(Context context, RealmResults<MessageItem> messageItems, AbstractChat chat, ChatFragment chatFragment) {
        super(context, messageItems, true);

        this.context = context;
        this.messageClickListener = chatFragment;

        account = chat.getAccount();
        user = chat.getUser();

        isMUC = MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible());
        if (isMUC) {
            mucNickname = MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible());
        }
        appearanceStyle = SettingsManager.chatsAppearanceStyle();

        this.listener = chatFragment;

        prevItemCount = getItemCount();
    }

    public interface Listener {
        void onNoDownloadFilePermission();
        void onMessageNumberChanged(int prevItemCount);
        void onMessagesUpdated();
    }

    private void setUpOutgoingMessage(Message holder, MessageItem messageItem) {
        setUpMessage(messageItem, holder);
        setStatusIcon(messageItem, (OutgoingMessage) holder);

        OutgoingMessage outgoingMessage = (OutgoingMessage) holder;

        outgoingMessage.progressBar.setVisibility(View.GONE);

        outgoingMessage.messageImage.setVisibility(View.GONE);
        outgoingMessage.messageText.setVisibility(View.VISIBLE);

        if (messageItem.getFilePath() != null) {
            if (messageItem.isInProgress()) {
                outgoingMessage.progressBar.setVisibility(View.VISIBLE);
            }

            if (FileManager.fileIsImage(messageItem.getFilePath())) {
                FileManager.loadImageFromFile(messageItem.getFilePath(), outgoingMessage.messageImage);

                outgoingMessage.messageImage.setVisibility(View.VISIBLE);
                outgoingMessage.messageText.setVisibility(View.GONE);
            }
        }

        setUpMessageBalloonBackground(holder.messageBalloon,
                context.getResources().getColorStateList(R.color.outgoing_message_color_state_dark), R.drawable.message_outgoing_states);
    }

    private void setUpIncomingMessage(final IncomingMessage incomingMessage, final MessageItem messageItem) {
        setUpMessage(messageItem, incomingMessage);

        if (messageItem.isReceivedFromMessageArchive()) {
            incomingMessage.statusIcon.setVisibility(View.VISIBLE);
        } else {
            incomingMessage.statusIcon.setVisibility(View.GONE);
        }

        setUpMessageBalloonBackground(incomingMessage.messageBalloon,
                ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account), R.drawable.message_incoming);

        setUpAvatar(messageItem, incomingMessage);

        if (messageItem.getText().trim().isEmpty()) {
            incomingMessage.messageBalloon.setVisibility(View.GONE);
            incomingMessage.messageTime.setVisibility(View.GONE);
            incomingMessage.avatar.setVisibility(View.GONE);
            LogManager.w(this, "Empty message! Hidden, but need to correct");
        } else {
            incomingMessage.messageBalloon.setVisibility(View.VISIBLE);
            incomingMessage.messageTime.setVisibility(View.VISIBLE);
        }
    }

    private void setUpMessageBalloonBackground(View messageBalloon, ColorStateList darkColorStateList, int lightBackgroundId) {

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            final Drawable originalBackgroundDrawable = messageBalloon.getBackground();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalBackgroundDrawable.setTintList(darkColorStateList);
            } else {
                Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
                DrawableCompat.setTintList(wrapDrawable, darkColorStateList);

                int pL = messageBalloon.getPaddingLeft();
                int pT = messageBalloon.getPaddingTop();
                int pR = messageBalloon.getPaddingRight();
                int pB = messageBalloon.getPaddingBottom();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    messageBalloon.setBackground(wrapDrawable);
                } else {
                    messageBalloon.setBackgroundDrawable(wrapDrawable);
                }

                messageBalloon.setPadding(pL, pT, pR, pB);
            }
        } else {
            int pL = messageBalloon.getPaddingLeft();
            int pT = messageBalloon.getPaddingTop();
            int pR = messageBalloon.getPaddingRight();
            int pB = messageBalloon.getPaddingBottom();

            messageBalloon.setBackgroundResource(lightBackgroundId);
            messageBalloon.getBackground().setLevel(AccountManager.getInstance().getColorLevel(account));
            messageBalloon.setPadding(pL, pT, pR, pB);
        }
    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded()) {
            return realmResults.size();
        } else {
            return 0;
        }
    }

    public MessageItem getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return null;
        }

        if (position < realmResults.size()) {
            return realmResults.get(position);
        } else {
            return null;
        }
    }

    public String getMessageItemId(int position) {
        MessageItem messageItem = getMessageItem(position);
        if (messageItem == null) {
            return null;
        } else {
            return messageItem.getUniqueId();
        }
    }

    @Override
    public BasicMessage onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HINT:
                return new BasicMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_info, parent, false));

            case VIEW_TYPE_ACTION_MESSAGE:
                return new BasicMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_action_message, parent, false));

            case VIEW_TYPE_INCOMING_MESSAGE:
                return new IncomingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_incoming_message, parent, false), messageClickListener);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_outgoing_message, parent, false), messageClickListener);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(BasicMessage holder, int position) {
        final int viewType = getItemViewType(position);

        MessageItem messageItem = getMessageItem(position);

        switch (viewType) {
            case VIEW_TYPE_HINT:
//                holder.messageText.setText(hint);
                break;

            case VIEW_TYPE_ACTION_MESSAGE:
                ChatAction action = MessageItem.getChatAction(messageItem);
                String time = StringUtils.getSmartTimeText(context, new Date(messageItem.getTimestamp()));

                String name;
                if (isMUC) {
                    name = messageItem.getResource().toString();
                } else {
                    name = RosterManager.getInstance().getBestContact(account, messageItem.getUser()).getName();
                }
                holder.messageText.setText(time + ": "
                        + action.getText(context, name, MessageItem.getSpannable(messageItem).toString()));

                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                setUpIncomingMessage((IncomingMessage) holder, messageItem);
                break;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                setUpOutgoingMessage((Message) holder, messageItem);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= realmResults.size()) {
            return VIEW_TYPE_HINT;
        }

        MessageItem messageItem = getMessageItem(position);
        if (messageItem.getAction() != null) {
            return VIEW_TYPE_ACTION_MESSAGE;
        }

        if (messageItem.isIncoming()) {
            if (isMUC && messageItem.getResource().equals(mucNickname)) {
                return VIEW_TYPE_OUTGOING_MESSAGE;
            }
            return VIEW_TYPE_INCOMING_MESSAGE;
        } else {
            return VIEW_TYPE_OUTGOING_MESSAGE;
        }
    }

    @Override
    public void onChange() {
        lastUpdateTimeMillis = System.currentTimeMillis();
        notifyDataSetChanged();
        listener.onMessagesUpdated();
        int itemCount = getItemCount();
        if (prevItemCount != itemCount) {
            listener.onMessageNumberChanged(prevItemCount);
            prevItemCount = itemCount;
        }
    }

    private void setUpMessage(MessageItem messageItem, Message message) {
        if (isMUC) {
            message.messageHeader.setText(messageItem.getResource());
            message.messageHeader.setVisibility(View.VISIBLE);
        } else {
            message.messageHeader.setVisibility(View.GONE);
        }

        if (messageItem.isUnencrypted()) {
            message.messageUnencrypted.setVisibility(View.VISIBLE);
        } else {
            message.messageUnencrypted.setVisibility(View.GONE);
        }

        message.messageText.setTextAppearance(context, appearanceStyle);

        final Spannable spannable = MessageItem.getSpannable(messageItem);
        Emoticons.getSmiledText(context, spannable, message.messageText);
        message.messageText.setText(spannable);
        message.messageText.setVisibility(View.VISIBLE);

        String time = StringUtils.getSmartTimeText(context, new Date(messageItem.getTimestamp()));

        Long delayTimestamp = messageItem.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = context.getString(messageItem.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getSmartTimeText(context, new Date(delayTimestamp)));
            time += " (" + delay + ")";
        }

        message.messageTime.setText(time);
    }

    private void setStatusIcon(MessageItem messageItem, OutgoingMessage message) {
        message.statusIcon.setVisibility(View.VISIBLE);
        message.progressBar.setVisibility(View.GONE);

        boolean isFileUploadInProgress = MessageItem.isUploadFileMessage(messageItem);

        if (isFileUploadInProgress) {
            message.progressBar.setVisibility(View.VISIBLE);
        }

        int messageIcon = R.drawable.ic_message_delivered_14dp;
        if (messageItem.isForwarded()) {
            messageIcon = R.drawable.ic_message_forwarded_14dp;
        } else if (messageItem.isReceivedFromMessageArchive()) {
            messageIcon = R.drawable.ic_message_synced_14dp;
        } else if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_14dp;
        } else if (!isFileUploadInProgress && !messageItem.isSent()
                && lastUpdateTimeMillis - messageItem.getTimestamp() > 1000) {
            messageIcon = R.drawable.ic_message_not_sent_14dp;
        } else if (!messageItem.isDelivered()) {
            if (messageItem.isAcknowledged()) {
                messageIcon = R.drawable.ic_message_acknowledged_14dp;
            } else {
                message.statusIcon.setVisibility(View.GONE);
            }
        }

        message.statusIcon.setImageResource(messageIcon);
    }

    private void setUpAvatar(MessageItem messageItem, IncomingMessage message) {
        if (SettingsManager.chatsShowAvatars()) {
            final AccountJid account = messageItem.getAccount();
            final UserJid user = messageItem.getUser();
            final Resourcepart resource = messageItem.getResource();

            message.avatar.setVisibility(View.VISIBLE);
            if ((isMUC && MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible()).equals(resource))) {
                message.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (isMUC) {
                    if (resource.equals(Resourcepart.EMPTY)) {
                        message.avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                    } else {
                        try {
                            message.avatar.setImageDrawable(AvatarManager.getInstance()
                                    .getUserAvatar(UserJid.from(JidCreate.domainFullFrom(user.getJid().asDomainBareJid(), resource))));
                        } catch (UserJid.UserJidCreateException e) {
                            LogManager.exception(this, e);
                        }
                    }
                } else {
                    message.avatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user));
                }
            }
        } else {
            message.avatar.setVisibility(View.GONE);
        }
    }

    public int findMessagePosition(String uniqueId) {
        for (int i = 0; i < realmResults.size(); i++) {
            if (realmResults.get(i).getUniqueId().equals(uniqueId)) {
                return i;
            }
        }

        return RecyclerView.NO_POSITION;
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
                return context.getString(R.string.contact_is_offline);
            }
        }
        return null;
    }

    static class BasicMessage extends RecyclerView.ViewHolder {

        TextView messageText;

        BasicMessage(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
        }
    }

    public static abstract class Message extends BasicMessage implements View.OnClickListener {

        TextView messageTime;
        TextView messageHeader;
        TextView messageUnencrypted;
        View messageBalloon;

        MessageClickListener onClickListener;

        ImageView messageImage;
        ImageView statusIcon;


        public Message(View itemView, MessageClickListener onClickListener) {
            super(itemView);
            this.onClickListener = onClickListener;

            messageTime = (TextView) itemView.findViewById(R.id.message_time);
            messageHeader = (TextView) itemView.findViewById(R.id.message_header);
            messageUnencrypted = (TextView) itemView.findViewById(R.id.message_unencrypted);
            messageBalloon = itemView.findViewById(R.id.message_balloon);

            messageImage = (ImageView) itemView.findViewById(R.id.message_image);

            statusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onClickListener.onMessageClick(messageBalloon, getPosition());
        }

        public interface MessageClickListener {
            void onMessageClick(View caller, int position);
        }

    }

    private static class IncomingMessage extends Message {

        public ImageView avatar;

        IncomingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
        }
    }

    private static class OutgoingMessage extends Message {

        TextView messageFileInfo;
        ProgressBar progressBar;

        OutgoingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            progressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
            messageFileInfo = (TextView) itemView.findViewById(R.id.message_file_info);
        }
    }
}
