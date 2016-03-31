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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.ChatViewerFragment;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;

import java.io.File;
import java.util.Date;

import io.realm.Realm;
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
    private String mucNickname;

    /**
     * Text with extra information.
     */
    private Listener listener;

    private String account;
    private String user;
    private int prevItemCount;
    private long lastUpdateTimeMillis;

    public ChatMessageAdapter(Context context, RealmResults<MessageItem> messageItems, AbstractChat chat, ChatViewerFragment chatViewerFragment) {
        super(context, messageItems, true);

        this.context = context;
        this.messageClickListener = chatViewerFragment;

        account = chat.getAccount();
        user = chat.getUser();

        isMUC = MUCManager.getInstance().hasRoom(account, user);
        if (isMUC) {
            mucNickname = MUCManager.getInstance().getNickname(account, user);
        }
        appearanceStyle = SettingsManager.chatsAppearanceStyle();

        this.listener = chatViewerFragment;

    }

    public interface Listener {
        void onNoDownloadFilePermission();
        void onMessageNumberChanged(int prevItemCount);
        void onMessagesUpdated();
    }

    private void setUpOutgoingMessage(Message holder, MessageItem messageItem) {
        setUpMessage(messageItem, holder);
        setStatusIcon(messageItem, (OutgoingMessage) holder);
        setUpFileMessage(holder, messageItem);

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
        setUpFileMessage(incomingMessage, messageItem);

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


    private void setUpFileMessage(final Message messageView, final MessageItem messageItem) {
        messageView.downloadProgressBar.setVisibility(View.GONE);
        messageView.attachmentButton.setVisibility(View.GONE);
        messageView.downloadButton.setVisibility(View.GONE);
        messageView.messageImage.setVisibility(View.GONE);
        messageView.messageFileInfo.setVisibility(View.GONE);
        messageView.messageTextForFileName.setVisibility(View.GONE);

        if (messageItem.isError()) {
            return;
        }

        String filePath = messageItem.getFilePath();

        if (filePath == null) {
            return;
        }

        final File file = new File(filePath);

        messageView.messageText.setVisibility(View.GONE);
        messageView.messageTextForFileName.setText(FileManager.getFileName(file.getPath()));
        messageView.messageTextForFileName.setVisibility(View.VISIBLE);

        messageView.attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileManager.openFile(context, file);
            }
        });

        final Long fileSize = messageItem.getFileSize();
        if (fileSize != null) {
            messageView.messageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(context, fileSize));
            messageView.messageFileInfo.setVisibility(View.VISIBLE);
        }

        if (file.exists()) {
            onFileExists(messageView, file);
        } else {
            if (SettingsManager.connectionLoadImages()
                    && FileManager.fileIsImage(file)
                    && PermissionsRequester.hasFileWritePermission()) {
                LogManager.i(this, "Downloading file from message adapter");
                downloadFile(messageView, messageItem);
            } else {
                messageView.downloadButton.setVisibility(View.VISIBLE);
                messageView.downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadFile(messageView, messageItem);
                    }
                });
            }
        }
    }

    private void downloadFile(final Message messageView, final MessageItem messageItem) {
        if (!PermissionsRequester.hasFileWritePermission()) {
            if (listener != null) {
                listener.onNoDownloadFilePermission();
            }
            return;
        }

        final String uniqueId = messageItem.getUniqueId();

        messageView.downloadButton.setVisibility(View.GONE);
        messageView.downloadProgressBar.setVisibility(View.VISIBLE);
        FileManager.getInstance().downloadFile(messageItem, new FileManager.ProgressListener() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                String progress = android.text.format.Formatter.formatShortFileSize(context, bytesWritten);
                // in some cases total size set to 1 (should be fixed in future version of com.loopj.android:android-async-http)
                if (bytesWritten <= totalSize) {
                    progress += " / " + android.text.format.Formatter.formatShortFileSize(context, totalSize);
                }

                if (!progress.equals(messageView.messageFileInfo.getText())) {
                    messageView.messageFileInfo.setText(progress);
                }

                if (messageView.messageFileInfo.getVisibility() != View.VISIBLE) {
                    messageView.messageFileInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFinish(long totalSize) {
                setUpFileMessage(messageView, messageItem);
            }

            @Override
            public void onError() {
                Realm realm = Realm.getDefaultInstance();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        MessageItem first = realm.where(MessageItem.class)
                                .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId).findFirst();
                        first.setError(true);
                    }
                }, null);

                realm.close();
            }

        });
    }

    private void onFileExists(Message message, final File file) {
        if (FileManager.fileIsImage(file) && PermissionsRequester.hasFileReadPermission()) {
            message.messageTextForFileName.setVisibility(View.GONE);
            message.messageImage.setVisibility(View.VISIBLE);
            FileManager.loadImageFromFile(file, message.messageImage);
            message.messageImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FileManager.openFile(context, file);
                }
            });

        } else {
            message.attachmentButton.setVisibility(View.VISIBLE);
        }

        message.messageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(context, file.length()));
        message.messageFileInfo.setVisibility(View.VISIBLE);
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
                        .inflate(R.layout.chat_viewer_info, parent, false));

            case VIEW_TYPE_ACTION_MESSAGE:
                return new BasicMessage(LayoutInflater.from(parent.getContext())
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
                    name = messageItem.getResource();
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
        message.messageTextForFileName.setTextAppearance(context, appearanceStyle);

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
                message.statusIcon.setVisibility(View.INVISIBLE);
            }
        }

        message.statusIcon.setImageResource(messageIcon);
    }

    private void setUpAvatar(MessageItem messageItem, IncomingMessage message) {
        if (SettingsManager.chatsShowAvatars()) {
            final String account = messageItem.getAccount();
            final String user = messageItem.getUser();
            final String resource = messageItem.getResource();

            message.avatar.setVisibility(View.VISIBLE);
            if ((isMUC && MUCManager.getInstance().getNickname(account, user).equalsIgnoreCase(resource))) {
                message.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (isMUC) {
                    if ("".equals(resource)) {
                        message.avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                    } else {
                        message.avatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user + "/" + resource));
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

    public static class BasicMessage extends RecyclerView.ViewHolder {

        public TextView messageText;

        public BasicMessage(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
        }
    }

    public static abstract class Message extends BasicMessage implements View.OnClickListener {

        public TextView messageTime;
        public TextView messageHeader;
        public TextView messageUnencrypted;
        public View messageBalloon;

        MessageClickListener onClickListener;

        public ImageButton downloadButton;
        public ImageButton attachmentButton;
        public ProgressBar downloadProgressBar;
        public ImageView messageImage;
        public TextView messageFileInfo;
        public TextView messageTextForFileName;

        public ImageView statusIcon;


        public Message(View itemView, MessageClickListener onClickListener) {
            super(itemView);
            this.onClickListener = onClickListener;


            messageTime = (TextView) itemView.findViewById(R.id.message_time);
            messageHeader = (TextView) itemView.findViewById(R.id.message_header);
            messageUnencrypted = (TextView) itemView.findViewById(R.id.message_unencrypted);
            messageBalloon = itemView.findViewById(R.id.message_balloon);

            downloadButton = (ImageButton) itemView.findViewById(R.id.message_download_button);
            attachmentButton = (ImageButton) itemView.findViewById(R.id.message_attachment_button);
            downloadProgressBar = (ProgressBar) itemView.findViewById(R.id.message_download_progress_bar);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image);
            messageFileInfo = (TextView) itemView.findViewById(R.id.message_file_info);
            messageTextForFileName = (TextView) itemView.findViewById(R.id.message_text_for_filenames);

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

    public static class IncomingMessage extends Message {

        public ImageView avatar;

        public IncomingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
        }
    }

    public static class OutgoingMessage extends Message {


        public ProgressBar progressBar;

        public OutgoingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            progressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
        }
    }
}
