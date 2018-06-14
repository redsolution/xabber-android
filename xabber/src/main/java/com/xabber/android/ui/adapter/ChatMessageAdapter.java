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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.widget.ImageGridBuilder;
import com.xabber.android.utils.StringUtils;

import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;


public class ChatMessageAdapter extends RealmRecyclerViewAdapter<MessageItem, ChatMessageAdapter.BasicMessage> {

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_HINT = 1;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;
    private static final String LOG_TAG = ChatMessageAdapter.class.getSimpleName();

    private final Context context;
    private final Message.MessageClickListener messageClickListener;
    private final ImageGridBuilder gridBuilder = new ImageGridBuilder();

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

    private String userName;
    private AccountJid account;
    private UserJid user;
    private int prevItemCount;
    private List<String> itemsNeedOriginalText;
    private int unreadCount = 0;

    public ChatMessageAdapter(Context context, RealmResults<MessageItem> messageItems, AbstractChat chat, ChatFragment chatFragment) {
        super(context, messageItems, true);

        this.context = context;
        this.messageClickListener = chatFragment;

        account = chat.getAccount();
        user = chat.getUser();
        userName = RosterManager.getInstance().getName(account, user);

        isMUC = MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible());
        if (isMUC) {
            mucNickname = MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible());
        }
        appearanceStyle = SettingsManager.chatsAppearanceStyle();

        this.listener = chatFragment;

        prevItemCount = getItemCount();

        itemsNeedOriginalText = new ArrayList<>();
    }

    public interface Listener {
        void onMessageNumberChanged(int prevItemCount);
        void onMessagesUpdated();
    }

    public boolean setUnreadCount(int unreadCount) {
        if (this.unreadCount != unreadCount) {
            this.unreadCount = unreadCount;
            return true;
        } else return false;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void addOrRemoveItemNeedOriginalText(String messageId) {
        if (itemsNeedOriginalText.contains(messageId))
            itemsNeedOriginalText.remove(messageId);
        else itemsNeedOriginalText.add(messageId);
    }

    private void setUpOutgoingMessage(Message holder, final MessageItem messageItem) {
        setUpMessage(messageItem, holder);
        setStatusIcon(messageItem, (OutgoingMessage) holder);

        OutgoingMessage outgoingMessage = (OutgoingMessage) holder;

        if (messageItem.isInProgress()) {
            outgoingMessage.progressBar.setVisibility(View.VISIBLE);
        } else {
            outgoingMessage.progressBar.setVisibility(View.GONE);
        }

        setupImageOrFile(messageItem, outgoingMessage);

        setUpMessageBalloonBackground(holder.messageBalloon,
                context.getResources().getColorStateList(R.color.outgoing_message_color_state_dark), R.drawable.message_outgoing_states);
    }

    private void prepareImage(MessageItem messageItem, final Message messageHolder) {
        String filePath = messageItem.getFilePath();
        Integer imageWidth = messageItem.getImageWidth();
        Integer imageHeight = messageItem.getImageHeight();
        String imageUrl = messageItem.getText();
        final String uniqueId = messageItem.getUniqueId();
        setUpImage(filePath, imageUrl, uniqueId, imageWidth, imageHeight, messageHolder);
    }

    private void prepareImage(Attachment attachment, final Message messageHolder) {
        String filePath = attachment.getFilePath();
        Integer imageWidth = attachment.getImageWidth();
        Integer imageHeight = attachment.getImageHeight();
        String imageUrl = attachment.getFileUrl();
        final String uniqueId = attachment.getUniqueId();
        setUpImage(filePath, imageUrl, uniqueId, imageWidth, imageHeight, messageHolder);
    }

    private void setUpImage(RealmList<Attachment> attachments, final Message messageHolder) {
        if (!SettingsManager.connectionLoadImages()) return;

        RealmList<Attachment> imageAttachments = new RealmList<>();
        for (Attachment attachment : attachments) {
            if (attachment.isImage()) imageAttachments.add(attachment);
        }

        if (imageAttachments.size() > 0) {
            View imageGridView = gridBuilder.inflateView(messageHolder.imageGridContainer, imageAttachments.size());
            gridBuilder.bindView(imageGridView, imageAttachments, messageHolder);

            messageHolder.imageGridContainer.addView(imageGridView);
            messageHolder.imageGridContainer.setVisibility(View.VISIBLE);
            messageHolder.messageText.setVisibility(View.GONE);
        }
    }

    private void setUpFile(RealmList<Attachment> attachments, final Message messageHolder) {
        RealmList<Attachment> fileAttachments = new RealmList<>();
        for (Attachment attachment : attachments) {
            if (!attachment.isImage()) fileAttachments.add(attachment);
        }

        if (fileAttachments.size() > 0) {
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
            messageHolder.rvFileList.setLayoutManager(layoutManager);
            FilesAdapter adapter = new FilesAdapter(fileAttachments, messageHolder);
            messageHolder.rvFileList.setAdapter(adapter);
            messageHolder.messageText.setVisibility(View.GONE);
            messageHolder.fileLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setUpImage(String imagePath, String imageUrl, final String uniqueId, Integer imageWidth,
                            Integer imageHeight, final Message messageHolder) {

        if (!SettingsManager.connectionLoadImages()) return;

        if (imagePath != null) {
            boolean result = FileManager.loadImageFromFile(context, imagePath, messageHolder.messageImage);

            if (result) {
                messageHolder.messageImage.setVisibility(View.VISIBLE);
                messageHolder.messageText.setVisibility(View.GONE);
            } else {
                final Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        MessageItem first = realm.where(MessageItem.class)
                                .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId)
                                .findFirst();
                        if (first != null) {
                            first.setFilePath(null);
                        }
                    }
                });
            }
        } else {
            final ViewGroup.LayoutParams layoutParams = messageHolder.messageImage.getLayoutParams();

            if (imageWidth != null && imageHeight != null) {
                FileManager.scaleImage(layoutParams, imageHeight, imageWidth);
                Glide.with(context)
                        .load(imageUrl)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                messageHolder.messageImage.setVisibility(View.GONE);
                                messageHolder.messageText.setVisibility(View.VISIBLE);
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(messageHolder.messageImage);

                messageHolder.messageImage.setVisibility(View.VISIBLE);
                messageHolder.messageText.setVisibility(View.GONE);
            } else {

                Glide.with(context)
                        .load(imageUrl)
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                final int width = resource.getWidth();
                                final int height = resource.getHeight();

                                if (width <= 0 || height <= 0) {
                                    messageHolder.messageImage.setVisibility(View.GONE);
                                    messageHolder.messageText.setVisibility(View.VISIBLE);
                                    return;
                                }

                                final Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                                realm.executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        MessageItem first = realm.where(MessageItem.class)
                                                .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId)
                                                .findFirst();
                                        if (first != null) {
                                            first.setImageWidth(width);
                                            first.setImageHeight(height);
                                        }
                                    }
                                });


                                FileManager.scaleImage(layoutParams, height, width);

                                messageHolder.messageImage.setImageBitmap(resource);

                                messageHolder.messageImage.setVisibility(View.VISIBLE);
                                messageHolder.messageText.setVisibility(View.GONE);
                            }
                        });
            }
        }
    }

    private void setupImageOrFile(MessageItem messageItem, final Message messageHolder) {
        messageHolder.fileLayout.setVisibility(View.GONE);
        messageHolder.messageImage.setVisibility(View.GONE);
        messageHolder.imageGridContainer.removeAllViews();
        messageHolder.imageGridContainer.setVisibility(View.GONE);
        messageHolder.messageText.setVisibility(View.VISIBLE);

        if (messageItem.haveAttachments()) {
            setUpImage(messageItem.getAttachments(), messageHolder);
            setUpFile(messageItem.getAttachments(), messageHolder);
        } else if (messageItem.isImage()) {
            prepareImage(messageItem, messageHolder);
        }
    }

    private void setUpIncomingMessage(final IncomingMessage incomingMessage, final MessageItem messageItem) {
        setUpMessage(messageItem, incomingMessage);

        if (messageItem.isReceivedFromMessageArchive()) {
            incomingMessage.statusIcon.setVisibility(View.VISIBLE);
        } else {
            incomingMessage.statusIcon.setVisibility(View.GONE);
        }

        setUpMessageBalloonBackgroundIncoming(incomingMessage.messageBalloon,
                ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account), R.drawable.message_incoming);

        setUpAvatar(messageItem, incomingMessage);

        setupImageOrFile(messageItem, incomingMessage);

        if (messageItem.getText().trim().isEmpty()) {
            incomingMessage.messageBalloon.setVisibility(View.GONE);
            incomingMessage.messageTime.setVisibility(View.GONE);
            incomingMessage.avatar.setVisibility(View.GONE);
            incomingMessage.avatarBackground.setVisibility(View.GONE);
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

    private void setUpMessageBalloonBackgroundIncoming(View messageBalloon, ColorStateList darkColorStateList, int lightBackgroundId) {

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

    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded()) {
            return realmResults.size();
        } else {
            return 0;
        }
    }

    @Nullable
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
                        .inflate(R.layout.item_incoming_message, parent, false),
                        messageClickListener, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_outgoing_message, parent, false),
                        messageClickListener, appearanceStyle);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final BasicMessage holder, int position) {
        final int viewType = getItemViewType(position);

        MessageItem messageItem = getMessageItem(position);

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onBindViewHolder Null message item. Position: " + position);
            return;
        }

        if (holder instanceof Message)
            ((Message)holder).messageId = messageItem.getUniqueId();

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

        // setup message as unread
        if (position >= getItemCount() - unreadCount)
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.unread_messages_background));
        else holder.itemView.setBackgroundDrawable(null);

        if (holder instanceof OutgoingMessage) {
            holder.itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    ((Message) holder).subscribeForUploadProgress(context);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    ((Message) holder).unsubscribeAll();
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= realmResults.size()) {
            return VIEW_TYPE_HINT;
        }

        MessageItem messageItem = getMessageItem(position);
        if (messageItem == null) {
            return 0;
        }

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

        if (messageItem.isEncrypted()) {
            message.ivEncrypted.setVisibility(View.VISIBLE);
        } else {
            message.ivEncrypted.setVisibility(View.GONE);
        }

        message.messageText.setText(messageItem.getText());
        if (OTRManager.getInstance().isEncrypted(messageItem.getText())) {
            if (itemsNeedOriginalText.contains(messageItem.getUniqueId()))
                message.messageText.setVisibility(View.VISIBLE);
            else message.messageText.setVisibility(View.GONE);
            message.messageNotDecrypted.setVisibility(View.VISIBLE);
        } else {
            message.messageText.setVisibility(View.VISIBLE);
            message.messageNotDecrypted.setVisibility(View.GONE);
        }

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
                && System.currentTimeMillis() - messageItem.getTimestamp() > 1000) {
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
        if (SettingsManager.chatsShowAvatars() && !isMUC) {
            final UserJid user = messageItem.getUser();
            message.avatar.setVisibility(View.VISIBLE);
            message.avatarBackground.setVisibility(View.VISIBLE);
            message.avatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user, userName));

        } else if (SettingsManager.chatsShowAvatarsMUC() && isMUC) {
            final AccountJid account = messageItem.getAccount();
            final UserJid user = messageItem.getUser();
            final Resourcepart resource = messageItem.getResource();

            message.avatar.setVisibility(View.VISIBLE);
            message.avatarBackground.setVisibility(View.VISIBLE);
            if ((MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible()).equals(resource))) {
                message.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (resource.equals(Resourcepart.EMPTY)) {
                    message.avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                } else {

                    String nick = resource.toString();
                    UserJid userJid = null;

                    try {
                        userJid = UserJid.from(user.getJid().toString() + "/" + resource.toString());
                        message.avatar.setImageDrawable(AvatarManager.getInstance()
                                .getOccupantAvatar(userJid, nick));

                    } catch (UserJid.UserJidCreateException e) {
                        LogManager.exception(this, e);
                        message.avatar.setImageDrawable(AvatarManager.getInstance()
                                .generateDefaultAvatar(nick, nick));
                    }
                }
            }
        } else {
            message.avatar.setVisibility(View.GONE);
            message.avatarBackground.setVisibility(View.GONE);
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

    private static void setUpProgress(Context context, OutgoingMessage holder,
                                      HttpFileUploadManager.ProgressData progressData) {
        if (progressData != null && holder.messageId.equals(progressData.getMessageId())) {
            if (progressData.isCompleted()) {
                showProgress(holder, false);
            } else if (progressData.getError() != null) {
                showProgress(holder, false);
                holder.onClickListener.onDownloadError(progressData.getError());
            } else {
                if (holder.uploadProgressBar != null) holder.uploadProgressBar.setProgress(progressData.getProgress());
                if (holder.messageFileInfo != null)
                    holder.messageFileInfo.setText(context.getString(R.string.uploaded_files_count,
                            progressData.getProgress() + "/" + progressData.getFileCount()));
                showProgress(holder, true);
            }
        } else showProgress(holder, false);
    }

    private static void showProgress(OutgoingMessage holder, boolean show) {
        if (show) {
            if (holder.uploadProgressBar != null) holder.uploadProgressBar.setVisibility(View.VISIBLE);
            if (holder.ivCancelUpload != null) holder.ivCancelUpload.setVisibility(View.VISIBLE);
            if (holder.messageFileInfo != null) holder.messageFileInfo.setVisibility(View.VISIBLE);
        } else {
            if (holder.uploadProgressBar != null) holder.uploadProgressBar.setVisibility(View.GONE);
            if (holder.ivCancelUpload != null) holder.ivCancelUpload.setVisibility(View.GONE);
            if (holder.messageFileInfo != null) holder.messageFileInfo.setVisibility(View.GONE);
        }
    }

    static class BasicMessage extends RecyclerView.ViewHolder {

        TextView messageText;

        BasicMessage(View itemView, @StyleRes int appearance) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
            messageText.setTextAppearance(itemView.getContext(), appearance);
        }

        BasicMessage(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
        }
    }

    public static abstract class Message extends BasicMessage implements View.OnClickListener,
            FilesAdapter.FileListListener {

        private static final String LOG_TAG = Message.class.getSimpleName();
        TextView messageTime;
        TextView messageHeader;
        TextView messageNotDecrypted;
        View messageBalloon;

        MessageClickListener onClickListener;

        ImageView messageImage;
        ImageView statusIcon;
        ImageView ivEncrypted;

        View fileLayout;
        RecyclerView rvFileList;

        FrameLayout imageGridContainer;

        private CompositeSubscription subscriptions = new CompositeSubscription();
        String messageId;
        final ProgressBar uploadProgressBar;
        final ImageButton ivCancelUpload;

        public Message(View itemView, MessageClickListener onClickListener, @StyleRes int appearance) {
            super(itemView, appearance);
            this.onClickListener = onClickListener;

            uploadProgressBar = itemView.findViewById(R.id.uploadProgressBar);
            ivCancelUpload = itemView.findViewById(R.id.ivCancelUpload);
            if (ivCancelUpload != null) ivCancelUpload.setOnClickListener(this);

            messageTime = (TextView) itemView.findViewById(R.id.message_time);
            messageHeader = (TextView) itemView.findViewById(R.id.message_header);
            messageNotDecrypted = (TextView) itemView.findViewById(R.id.message_not_decrypted);
            messageBalloon = itemView.findViewById(R.id.message_balloon);

            messageImage = (ImageView) itemView.findViewById(R.id.message_image);

            statusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);
            ivEncrypted = (ImageView) itemView.findViewById(R.id.message_encrypted_icon);

            fileLayout = itemView.findViewById(R.id.fileLayout);
            rvFileList = itemView.findViewById(R.id.rvFileList);

            imageGridContainer = itemView.findViewById(R.id.imageGridContainer);

            itemView.setOnClickListener(this);
            messageImage.setOnClickListener(this);
        }

        @Override
        public void onFileClick(int attachmentPosition) {
            int messagePosition = getAdapterPosition();
            if (messagePosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            onClickListener.onFileClick(messagePosition, attachmentPosition);
        }

        @Override
        public void onFileLongClick(int attachmentPosition, View caller) {
            int messagePosition = getAdapterPosition();
            if (messagePosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }
            onClickListener.onFileLongClick(messagePosition, attachmentPosition, caller);
        }

        @Override
        public void onDownloadCancel() {
            onClickListener.onDownloadCancel();
        }

        @Override
        public void onDownloadError(String error) {
            onClickListener.onDownloadError(error);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            switch (v.getId()) {
                case R.id.ivImage0:
                    onClickListener.onImageClick(adapterPosition, 0);
                    break;
                case R.id.ivImage1:
                    onClickListener.onImageClick(adapterPosition, 1);
                    break;
                case R.id.ivImage2:
                    onClickListener.onImageClick(adapterPosition, 2);
                    break;
                case R.id.ivImage3:
                    onClickListener.onImageClick(adapterPosition, 3);
                    break;
                case R.id.ivImage4:
                    onClickListener.onImageClick(adapterPosition, 4);
                    break;
                case R.id.ivImage5:
                    onClickListener.onImageClick(adapterPosition, 5);
                    break;
                case R.id.message_image:
                    onClickListener.onImageClick(adapterPosition, 0);
                    break;
                case R.id.ivCancelUpload:
                    onClickListener.onUploadCancel();
                    break;
                default:
                    onClickListener.onMessageClick(messageBalloon, adapterPosition);
                    break;
            }
        }

        public interface MessageClickListener {
            void onMessageClick(View caller, int position);
            void onImageClick(int messagePosition, int attachmentPosition);
            void onFileClick(int messagePosition, int attachmentPosition);
            void onFileLongClick(int messagePosition, int attachmentPosition, View caller);
            void onDownloadCancel();
            void onUploadCancel();
            void onDownloadError(String error);
        }

        public void unsubscribeAll() {
            subscriptions.clear();
        }

        public void subscribeForUploadProgress(final Context context) {
            subscriptions.add(HttpFileUploadManager.getInstance().subscribeForProgress()
                .doOnNext(new Action1<HttpFileUploadManager.ProgressData>() {
                    @Override
                    public void call(HttpFileUploadManager.ProgressData progressData) {
                        setUpProgress(context, (OutgoingMessage) Message.this, progressData);
                    }
                }).subscribe());
        }
    }

    private static class IncomingMessage extends Message {

        public ImageView avatar;
        public ImageView avatarBackground;

        IncomingMessage(View itemView, MessageClickListener listener, @StyleRes int appearance) {
            super(itemView, listener, appearance);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            avatarBackground = (ImageView) itemView.findViewById(R.id.avatarBackground);
        }
    }

    private static class OutgoingMessage extends Message {

        TextView messageFileInfo;
        ProgressBar progressBar;

        OutgoingMessage(View itemView, MessageClickListener listener, @StyleRes int appearance) {
            super(itemView, listener, appearance);
            progressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
            messageFileInfo = (TextView) itemView.findViewById(R.id.message_file_info);
        }
    }
}
