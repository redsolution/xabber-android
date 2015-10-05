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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
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
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

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
    private String account;
    private String user;
    private boolean isMUC;
    private String mucNickname;

    private List<MessageItem> messages;
    /**
     * Text with extra information.
     */
    private String hint;

    public ChatMessageAdapter(Context context, String account, String user, Message.MessageClickListener messageClickListener) {
        this.context = context;
        messages = Collections.emptyList();
        this.account = account;
        this.user = user;
        this.messageClickListener = messageClickListener;

        isMUC = MUCManager.getInstance().hasRoom(account, user);
        if (isMUC) {
            mucNickname = MUCManager.getInstance().getNickname(account, user);
        }
        hint = null;
        appearanceStyle = SettingsManager.chatsAppearanceStyle();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int viewType = getItemViewType(position);

        MessageItem messageItem = getMessageItem(position);

        switch (viewType) {
            case VIEW_TYPE_HINT:
                ((BasicMessage) holder).messageText.setText(hint);
                break;

            case VIEW_TYPE_ACTION_MESSAGE:
                ChatAction action = messageItem.getAction();
                String time = StringUtils.getSmartTimeText(context, messageItem.getTimestamp());

                String name;
                if (isMUC) {
                    name = messageItem.getResource();
                } else {
                    name = RosterManager.getInstance().getBestContact(account, messageItem.getChat().getUser()).getName();
                }
                ((BasicMessage)holder).messageText.setText(time + ": "
                        + action.getText(context, name, messageItem.getSpannable().toString()));

                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                setUpIncomingMessage((IncomingMessage) holder, messageItem);
                break;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                setUpMessage(messageItem, (Message) holder);
                setStatusIcon(messageItem, (OutgoingMessage) holder);
                setUpFileMessage((Message) holder, messageItem);
                break;
        }

    }

    private void setUpIncomingMessage(final IncomingMessage incomingMessage, final MessageItem messageItem) {
        setUpMessage(messageItem, incomingMessage);

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

    // TODO: refactoring needed
    private void setUpFileMessage(final Message message, final MessageItem messageItem) {
        message.downloadProgressBar.setVisibility(View.GONE);
        message.attachmentButton.setVisibility(View.GONE);
        message.downloadButton.setVisibility(View.GONE);
        message.messageImage.setVisibility(View.GONE);

        message.messageFileInfo.setVisibility(View.GONE);

        if (StringUtils.treatAsDownloadable(messageItem.getText())) {
            message.messageText.setVisibility(View.GONE);
            message.messageTextForFileName.setVisibility(View.VISIBLE);

            final String path;
            final URL url;

            try {
                url = new URL(messageItem.getText());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }

            path = url.getPath();

            String filename = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
            message.messageTextForFileName.setText(filename);

            final String extension = StringUtils.extractRelevantExtension(url);

            final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Xabber/Cache/" + path);

            messageItem.setFilePath(file.getPath());

            message.attachmentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFile(context, file);
                }
            });


            if (file.exists()) {
                onFileExists(message, extension, file);
            } else {
                AsyncHttpClient client = new AsyncHttpClient();
                client.head(messageItem.getText(), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        for (Header header : headers) {
                            if (header.getName().equals(HttpHeaders.CONTENT_LENGTH)) {
                                message.messageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(context, Long.parseLong(header.getValue())));
                                message.messageFileInfo.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });


                message.downloadButton.setVisibility(View.VISIBLE);
                message.downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.setConnectTimeout(60 * 1000);
                        client.setLoggingEnabled(SettingsManager.debugLog());
                        client.setResponseTimeout(60 * 1000);
                        client.get(messageItem.getText(), new AsyncHttpResponseHandler() {
                            @Override
                            public void onStart() {
                                message.downloadProgressBar.setVisibility(View.VISIBLE);
                                message.downloadButton.setVisibility(View.GONE);
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, final byte[] responseBody) {
                                LogManager.i(this, "onSuccess: " + statusCode);

                                message.downloadProgressBar.setVisibility(View.GONE);

                                Application.getInstance().runInBackground(new Runnable() {
                                    @Override
                                    public void run() {
                                        new File(file.getParent()).mkdirs();
                                        try {
                                            file.createNewFile();
                                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                                            bos.write(responseBody);
                                            bos.flush();
                                            bos.close();

                                            Application.getInstance().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    message.messageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(context, file.length()));
                                                    message.messageFileInfo.setVisibility(View.VISIBLE);

                                                    if (extensionIsImage(extension)) {
                                                        message.messageImage.setOnClickListener(new View.OnClickListener() {
                                                            @Override
                                                            public void onClick(View v) {
                                                                openFile(context, file);
                                                            }
                                                        });
                                                    } else {
                                                        message.attachmentButton.setVisibility(View.VISIBLE);
                                                    }

                                                }
                                            });


                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            file.delete();

                                            Application.getInstance().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    message.downloadButton.setVisibility(View.VISIBLE);
                                                }
                                            });

                                        }
                                    }
                                });

                                if (extensionIsImage(extension)) {
                                    message.messageTextForFileName.setVisibility(View.GONE);
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;

                                    //Returns null, sizes are in the options variable
                                    BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length, options);

                                    ImageScaler imageScaler = new ImageScaler(options.outHeight, options.outWidth).invoke();

                                    message.messageImage.setVisibility(View.VISIBLE);

                                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageScaler.getScalledW(), imageScaler.getScalledH());
                                    message.messageImage.setLayoutParams(layoutParams);

                                    LogManager.i(this, String.format("Loading image from bytes. src: %d x %d, dst: %d x %d", options.outHeight, options.outWidth, imageScaler.getScalledW(), imageScaler.getScalledH()));
                                    Glide.with(context).load(responseBody).crossFade().override(imageScaler.getScalledW(), imageScaler.getScalledH()).into(message.messageImage);

                                }


                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                LogManager.i(this, "onFailure: " + statusCode);

                                message.downloadProgressBar.setVisibility(View.GONE);
                                message.downloadButton.setVisibility(View.VISIBLE);

                            }

                            @Override
                            public void onProgress(long bytesWritten, long totalSize) {
                                LogManager.i(this, "onProgress: " + bytesWritten + " / " + totalSize);

                                String progress = android.text.format.Formatter.formatShortFileSize(context, bytesWritten);
                                // in some cases total size set to 1 (should be fixed in future version of com.loopj.android:android-async-http)
                                if (bytesWritten <= totalSize) {
                                    progress += " / " + android.text.format.Formatter.formatShortFileSize(context, totalSize);
                                }

                                message.messageFileInfo.setText(progress);
                                message.messageFileInfo.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onFinish() {


                            }
                        });
                    }
                });

                // TODO download images on message receiving
                if (SettingsManager.connectionLoadImages() && extensionIsImage(extension)) {
                    message.downloadButton.callOnClick();
                }
            }
        }
    }

    private boolean extensionIsImage(String extension) {
        return Arrays.asList(StringUtils.VALID_IMAGE_EXTENSIONS).contains(extension);
    }

    public static void openFile(Context context, File file) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString())));

        PackageManager manager = context.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        if (infos.size() > 0) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_application_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void onFileExists(Message message, final String extension, final File file) {
        if (extensionIsImage(extension)) {
            message.messageTextForFileName.setVisibility(View.GONE);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            //Returns null, sizes are in the options variable
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            final int height = options.outHeight;
            final int width = options.outWidth;

            ImageScaler imageScaler = new ImageScaler(height, width).invoke();
            int scalledW = imageScaler.getScalledW();
            int scalledH = imageScaler.getScalledH();

            message.messageImage.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(scalledW, scalledH);
            message.messageImage.setLayoutParams(layoutParams);

            LogManager.i(this, String.format("Loading image from file. src: %d x %d, dst: %d x %d", width, height, scalledW, scalledH));
            Glide.with(context).load(file).crossFade().override(scalledW, scalledH).into(message.messageImage);
            message.messageImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFile(context, file);
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
        if (hint == null) {
            return messages.size();
        } else {
            return messages.size() + 1;
        }
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

        if (messageItem.isIncoming()) {
            if (isMUC && messageItem.getResource().equals(mucNickname)) {
                return VIEW_TYPE_OUTGOING_MESSAGE;
            }
            return VIEW_TYPE_INCOMING_MESSAGE;
        } else {
            return VIEW_TYPE_OUTGOING_MESSAGE;
        }
    }

    private void setUpMessage(MessageItem messageItem, Message message) {

        if (isMUC) {
            message.messageHeader.setText(messageItem.getResource());
            message.messageHeader.setVisibility(View.VISIBLE);
        } else {
            message.messageHeader.setVisibility(View.GONE);
        }

        if (messageItem.isUnencypted()) {
            message.messageUnencrypted.setVisibility(View.VISIBLE);
        } else {
            message.messageUnencrypted.setVisibility(View.GONE);
        }

        message.messageText.setTextAppearance(context, appearanceStyle);
        message.messageTextForFileName.setTextAppearance(context, appearanceStyle);

        final Spannable spannable = messageItem.getSpannable();
        Emoticons.getSmiledText(context, spannable, message.messageText);
        message.messageText.setText(spannable);

        message.messageBalloon.getBackground().setLevel(AccountManager.getInstance().getColorLevel(account));

        String time = StringUtils.getSmartTimeText(context, messageItem.getTimestamp());

        Date delayTimestamp = messageItem.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = context.getString(messageItem.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getSmartTimeText(context, delayTimestamp));
            time += " (" + delay + ")";
        }

        message.messageTime.setText(time);
    }

    private void setStatusIcon(MessageItem messageItem, OutgoingMessage message) {
        message.statusIcon.setVisibility(View.VISIBLE);
        message.progressBar.setVisibility(View.GONE);

        if (messageItem.isUploadFileMessage() && !messageItem.isError()) {
            message.progressBar.setVisibility(View.VISIBLE);
        }

        int messageIcon = R.drawable.ic_message_delivered_18dp;
        if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_18dp;
        } else if (!messageItem.isUploadFileMessage() && !messageItem.isSent()) {
            messageIcon = R.drawable.ic_message_not_sent_18dp;
        } else if (!messageItem.isDelivered()) {
            message.statusIcon.setVisibility(View.GONE);
        }

        message.statusIcon.setImageResource(messageIcon);
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

        public ImageView statusIcon;
        public ProgressBar progressBar;

        public OutgoingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            statusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);
            progressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
        }
    }

    private class ImageScaler {
        private int height;
        private int width;
        private int scalledW;
        private int scalledH;

        public ImageScaler(int height, int width) {
            this.height = height;
            this.width = width;
        }

        public int getScalledW() {
            return scalledW;
        }

        public int getScalledH() {
            return scalledH;
        }

        public ImageScaler invoke() {
            Resources resources = context.getResources();
            final int maxImageSize = resources.getDimensionPixelSize(R.dimen.max_chat_image_size);
            final int minImageSize = resources.getDimensionPixelSize(R.dimen.min_chat_image_size);

            if (width <= height) {
                if (height > maxImageSize) {
                    scalledW = (int) (width / ((double) height / maxImageSize));
                    scalledH = maxImageSize;
                } else if (width < minImageSize) {
                    scalledW = minImageSize;
                    scalledH = (int) (height / ((double) width / minImageSize));
                    if (scalledH > maxImageSize) {
                        scalledH = maxImageSize;
                    }
                } else {
                    scalledW = width;
                    scalledH = height;
                }
            } else {
                if (width > maxImageSize) {
                    scalledW = maxImageSize;
                    scalledH = (int) (height / ((double) width / maxImageSize));
                } else if (height < minImageSize) {
                    scalledW = (int) (width / ((double) height / minImageSize));
                    if (scalledW > maxImageSize) {
                        scalledW = maxImageSize;
                    }
                    scalledH = minImageSize;
                } else {
                    scalledW = width;
                    scalledH = height;
                }
            }
            return this;
        }
    }
}
