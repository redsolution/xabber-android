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
package com.xabber.android.data.message;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.ForwardId;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.chat_markers.BackpressureMessageReader;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.file.UriUtils;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.reliablemessagedelivery.OriginIdElement;
import com.xabber.android.data.extension.reliablemessagedelivery.ReceiptRequestElement;
import com.xabber.android.data.extension.reliablemessagedelivery.ReliableMessageDeliveryManager;
import com.xabber.android.data.extension.reliablemessagedelivery.RetryReceiptRequestElement;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterCacheManager;
import com.xabber.android.ui.adapter.chat.FileMessageVH;
import com.xabber.android.utils.Utils;
import com.xabber.xmpp.sid.UniqStanzaHelper;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Chat instance.
 *
 * @author alexander.ivanov
 */
public abstract class AbstractChat extends BaseEntity implements RealmChangeListener<RealmResults<MessageItem>> {

    private static final String LOG_TAG = AbstractChat.class.getSimpleName();

    /**
     * Number of messages from history to be shown for context purpose.
     */
    public static final int PRELOADED_MESSAGES = 50;

    /**
     * Whether chat is open and should be displayed as active chat.
     */
    protected boolean active;
    /**
     * Whether changes in status should be record.
     */
    private boolean trackStatus;
    /**
     * Whether user never received notifications from this chat.
     */
    private boolean firstNotification;

    /**
     * Current thread id.
     */
    private String threadId;

    /**
     *  Last known "chatstate", i.e. "deleted chat", "chat with cleared history", "normal chat".
     *  TODO: tie this state with the lastActionTimestamp and archive loading to avoid showing "cleared messages" in chat or showing a "deleted" chat.
     */
    private int chatstateType;

    /**
     *  The timestamp of the last chat action, such as: deletion, history clear, etc.
     */
    private Long lastActionTimestamp;


    private int lastPosition;
    private boolean archived;
    protected NotificationState notificationState;

    private Set<String> waitToMarkAsRead = new HashSet<>();

    private boolean isPrivateMucChat;
    private boolean isPrivateMucChatAccepted;

    private boolean isRemotePreviousHistoryCompletelyLoaded = false;

    private Date lastSyncedTime;
    private MessageItem lastMessage;
    private RealmResults<MessageItem> messages;
    private String lastMessageId = null;
    private boolean addContactSuggested = false;
    private boolean historyIsFull = false;
    private boolean historyRequestedAtStart = false;
    protected boolean isGroupchat = false;

    protected AbstractChat(@NonNull final AccountJid account, @NonNull final UserJid user, boolean isPrivateMucChat) {
        super(account, isPrivateMucChat ? user : user.getBareUserJid());
        threadId = StringUtils.randomString(12);
        active = false;
        trackStatus = false;
        firstNotification = true;
        this.isPrivateMucChat = isPrivateMucChat;
        isPrivateMucChatAccepted = false;
        notificationState = new NotificationState(NotificationState.NotificationMode.bydefault, 0);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMessages();
            }
        });
    }

    public boolean isRemotePreviousHistoryCompletelyLoaded() {
        return isRemotePreviousHistoryCompletelyLoaded;
    }

    public void setRemotePreviousHistoryCompletelyLoaded(boolean remotePreviousHistoryCompletelyLoaded) {
        isRemotePreviousHistoryCompletelyLoaded = remotePreviousHistoryCompletelyLoaded;
    }

    public Date getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setLastSyncedTime(Date lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
    }


    public boolean isActive() {
        if (isPrivateMucChat && !isPrivateMucChatAccepted) {
            return false;
        }

        return active;
    }

    public void openChat() {
        active = true;
        trackStatus = true;
    }

    void closeChat() {
        active = false;
        firstNotification = true;
    }

    private String getAccountString() {
        return account.toString();
    }

    private String getUserString() {
        return user.toString();
    }

    public RealmResults<MessageItem> getMessages() {
        if (messages == null) {
            messages = MessageRepository.getChatMessages(account, user);
            updateLastMessage();

            messages.addChangeListener(this);
        }

        return messages;
    }

    boolean isStatusTrackingEnabled() {
        return trackStatus;
    }

    /**
     * @return Target address for sending message.
     */
    @NonNull
    public abstract Jid getTo();

    /**
     * @return Message type to be assigned.
     */
    public abstract Type getType();

    /**
     * @return Whether user never received notifications from this chat. And
     * mark as received.
     */
    public boolean getFirstNotification() {
        boolean result = firstNotification;
        firstNotification = false;
        return result;
    }

    /**
     * @return Whether user should be notified about incoming messages in chat.
     */
    public boolean notifyAboutMessage() {
        if (notificationState.getMode().equals(NotificationState.NotificationMode.bydefault))
            return SettingsManager.eventsOnChat();
        if (notificationState.getMode().equals(NotificationState.NotificationMode.enabled))
            return true;
        else return false;
    }

    public void enableNotificationsIfNeed() {
        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        NotificationState.NotificationMode mode = notificationState.getMode();

        if ((mode.equals(NotificationState.NotificationMode.snooze15m)
                && currentTime > notificationState.getTimestamp() + TimeUnit.MINUTES.toSeconds(15))
                || (mode.equals(NotificationState.NotificationMode.snooze1h)
                && currentTime > notificationState.getTimestamp() + TimeUnit.HOURS.toSeconds(1))
                || (mode.equals(NotificationState.NotificationMode.snooze2h)
                && currentTime > notificationState.getTimestamp() + TimeUnit.HOURS.toSeconds(2))
                || (mode.equals(NotificationState.NotificationMode.snooze1d)
                && currentTime > notificationState.getTimestamp() + TimeUnit.DAYS.toSeconds(1))) {

            setNotificationStateOrDefault(new NotificationState(
                    NotificationState.NotificationMode.enabled, 0), true);
        }
    }

    abstract protected MessageItem createNewMessageItem(String text);

    /**
     * Creates new action.
     *
     * @param resource can be <code>null</code>.
     * @param text     can be <code>null</code>.
     */
    public void newAction(Resourcepart resource, String text, ChatAction action, boolean fromMUC) {
        createAndSaveNewMessage(true, UUID.randomUUID().toString(), resource, text, null,
                action, null, null, true, false, false, false,
                null, null, null, null, null, false, null,
                fromMUC, false, null);
    }

    /**
     * Creates new message.
     * <p/>
     * Any parameter can be <code>null</code> (except boolean values).
     *
     * @param resource       Contact's resource or nick in conference.
     * @param text           message.
     * @param action         Informational message.
     * @param delayTimestamp Time when incoming message was sent or outgoing was created.
     * @param incoming       Incoming message.
     * @param notify         Notify user about this message when appropriated.
     * @param encrypted      Whether encrypted message in OTR chat was received.
     * @param offline        Whether message was received from server side offline storage.
     * @return
     */
    protected void createAndSaveNewMessage(boolean ui, String uid, Resourcepart resource, String text,
                                           String markupText, final ChatAction action, final Date timestamp,
                                           final Date delayTimestamp, final boolean incoming, boolean notify,
                                           final boolean encrypted, final boolean offline, final String stanzaId, final String originId,
                                           final String originalStanza, final String parentMessageId, final String originalFrom, boolean isForwarded,
                                           final RealmList<ForwardId> forwardIds, boolean fromMUC, boolean fromMAM, String groupchatUserId) {

        final MessageItem messageItem = createMessageItem(uid, resource, text, markupText, action,
                timestamp, delayTimestamp, incoming, notify, encrypted, offline, stanzaId, originId, null,
                originalStanza, parentMessageId, originalFrom, isForwarded, forwardIds, fromMUC, fromMAM, groupchatUserId);

        saveMessageItem(ui, messageItem);
        //EventBus.getDefault().post(new NewMessageEvent());
    }

    protected void createAndSaveFileMessage(boolean ui, String uid, Resourcepart resource, String text,
                                            String markupText, final ChatAction action, final Date timestamp,
                                            final Date delayTimestamp, final boolean incoming, boolean notify,
                                            final boolean encrypted, final boolean offline, final String stanzaId,
                                            String originId, RealmList<Attachment> attachments, final String originalStanza,
                                            final String parentMessageId, final String originalFrom, boolean isForwarded,
                                            final RealmList<ForwardId> forwardIds, boolean fromMUC, boolean fromMAM, String groupchatUserId) {

        final MessageItem messageItem = createMessageItem(uid, resource, text, markupText, action,
                timestamp, delayTimestamp, incoming, notify, encrypted, offline, stanzaId, originId, attachments,
                originalStanza, parentMessageId, originalFrom, isForwarded, forwardIds, fromMUC, fromMAM, groupchatUserId);

        saveMessageItem(ui, messageItem);
        //EventBus.getDefault().post(new NewMessageEvent());
    }

    public void saveMessageItem(boolean ui, final MessageItem messageItem) {
        if (ui) BackpressureMessageSaver.getInstance().saveMessageItem(messageItem);
        else {
            final long startTime = System.currentTimeMillis();
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    Realm realm = null;
                    try {
                        realm = realm;
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealm(messageItem);
                                LogManager.d("REALM", Thread.currentThread().getName()
                                        + " save message item: " + (System.currentTimeMillis() - startTime));
                                EventBus.getDefault().post(new NewMessageEvent());
                            }
                        });
                    } catch (Exception e) {
                        LogManager.exception(LOG_TAG, e);
                    } finally { if (realm != null) realm.close(); }
                }
            });
        }
    }

    protected MessageItem createMessageItem(Resourcepart resource, String text,
                                            String markupText, ChatAction action,
                                            Date delayTimestamp, boolean incoming, boolean notify, boolean encrypted,
                                            boolean offline, String stanzaId, String originId, RealmList<Attachment> attachments,
                                            String originalStanza, String parentMessageId, String originalFrom, boolean isForwarded,
                                            RealmList<ForwardId> forwardIds, boolean fromMUC) {

        return createMessageItem(UUID.randomUUID().toString(), resource, text, markupText, action,
                null, delayTimestamp, incoming, notify, encrypted, offline, stanzaId, originId, attachments,
                originalStanza, parentMessageId, originalFrom, isForwarded, forwardIds, fromMUC, false, null);
    }

    protected MessageItem createMessageItem(String uid, Resourcepart resource, String text,
                                            String markupText, ChatAction action, Date timestamp,
                                            Date delayTimestamp, boolean incoming, boolean notify, boolean encrypted,
                                            boolean offline, String stanzaId, String originId, RealmList<Attachment> attachments,
                                            String originalStanza, String parentMessageId, String originalFrom, boolean isForwarded,
                                            RealmList<ForwardId> forwardIds, boolean fromMUC, boolean fromMAM, String groupchatUserId) {

        final boolean visible = MessageManager.getInstance().isVisibleChat(this);
        boolean read = !incoming;
        boolean send = incoming;
        if (action == null && text == null) {
            throw new IllegalArgumentException();
        }
        if (text == null) {
            text = " ";
        }
        if (action != null) {
            read = true;
            send = true;
        }

        if (timestamp == null) timestamp = new Date();

        if (text.trim().isEmpty() && (forwardIds == null || forwardIds.isEmpty())
                && (attachments == null || attachments.isEmpty())) {
            notify = false;
        }

        if (notify || !incoming) {
            openChat();
        }
        if (!incoming) {
            notify = false;
        }

        if (isPrivateMucChat) {
            if (!isPrivateMucChatAccepted) {
                notify = false;
            }
        }

        MessageItem messageItem = new MessageItem(uid);

        messageItem.setAccount(account);
        messageItem.setUser(user);

        if (resource == null) {
            messageItem.setResource(Resourcepart.EMPTY);
        } else {
            messageItem.setResource(resource);
        }

        if (action != null) {
            messageItem.setAction(action.toString());
        }
        messageItem.setText(text);
        if (markupText != null) messageItem.setMarkupText(markupText);
        messageItem.setTimestamp(timestamp.getTime());
        if (delayTimestamp != null) {
            messageItem.setDelayTimestamp(delayTimestamp.getTime());
        }
        messageItem.setIncoming(incoming);
        messageItem.setRead(fromMAM || read);
        messageItem.setSent(send);
        messageItem.setEncrypted(encrypted);
        messageItem.setOffline(offline);
        messageItem.setFromMUC(fromMUC);
        messageItem.setStanzaId(stanzaId);
        messageItem.setOriginId(originId);
        if (attachments != null) messageItem.setAttachments(attachments);
        FileManager.processFileMessage(messageItem);

        // forwarding
        if (forwardIds != null) messageItem.setForwardedIds(forwardIds);
        messageItem.setOriginalStanza(originalStanza);
        messageItem.setOriginalFrom(originalFrom);
        messageItem.setParentMessageId(parentMessageId);
        messageItem.setForwarded(isForwarded);

        // groupchat
        if (groupchatUserId != null) messageItem.setGroupchatUserId(groupchatUserId);

        // notification
        enableNotificationsIfNeed();
        if (notify && notifyAboutMessage() && !visible)
            NotificationManager.getInstance().onMessageNotification(messageItem);

        // remove notifications if get outgoing message with 2 sec delay
        if (!incoming) MessageNotificationManager.getInstance().removeChatWithTimer(account, user);

        // when getting new message, unarchive chat if chat not muted
        if (this.notifyAboutMessage())
            this.archived = false;

        // update last id in chat
        messageItem.setPreviousId(getLastMessageId());
        String id = messageItem.getArchivedId();
        if (id == null) id = messageItem.getStanzaId();
        setLastMessageId(id);

        return messageItem;
    }

    public String newFileMessage(final List<File> files, final List<Uri> uris) {
        return newFileMessage(files, uris, null);
    }

    public String newFileMessage(final List<File> files, final List<Uri> uris, final String referenceType) {
        return newFileMessageWithFwr(files, uris, referenceType, null);
    }

    public String newFileMessageWithFwr(final List<File> files, final List<Uri> uris, final String referenceType, final List<String> forwards) {
        Realm realm = Realm.getDefaultInstance();
        final String messageId = UUID.randomUUID().toString();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmList<Attachment> attachments;
                if (files != null) attachments = attachmentsFromFiles(files, referenceType);
                else attachments = attachmentsFromUris(uris);
                String initialID = UUID.randomUUID().toString();

                MessageItem messageItem = new MessageItem(messageId);

                if (forwards != null && forwards.size()>0) {
                    RealmList<ForwardId> ids = new RealmList<>();

                    for (String forward : forwards) {
                        ids.add(new ForwardId(forward));
                    }
                    messageItem.setForwardedIds(ids);
                }

                messageItem.setAccount(account);
                messageItem.setUser(user);
                messageItem.setOriginalFrom(account.toString());
                messageItem.setText(FileMessageVH.UPLOAD_TAG);
                messageItem.setAttachments(attachments);
                messageItem.setTimestamp(System.currentTimeMillis());
                messageItem.setRead(true);
                messageItem.setSent(true);
                messageItem.setError(false);
                messageItem.setIncoming(false);
                messageItem.setInProgress(true);
                messageItem.setStanzaId(initialID);
                messageItem.setOriginId(initialID);
                realm.copyToRealm(messageItem);
            }
        });
        realm.close();

        return messageId;
    }

    public RealmList<Attachment> attachmentsFromFiles(List<File> files) {
        return attachmentsFromFiles(files, null);
    }

    public RealmList<Attachment> attachmentsFromFiles(List<File> files, String refElement) {
        RealmList<Attachment> attachments = new RealmList<>();
        for (File file : files) {
            boolean isImage = FileManager.fileIsImage(file);
            Attachment attachment = new Attachment();
            attachment.setFilePath(file.getPath());
            attachment.setFileSize(file.length());
            attachment.setTitle(file.getName());
            attachment.setIsImage(isImage);
            attachment.setMimeType(HttpFileUploadManager.getMimeType(file.getPath()));
            if (ReferenceElement.Type.voice.name().equals(refElement)) {
                attachment.setIsVoice(true);
                attachment.setRefType(ReferenceElement.Type.voice.name());
                attachment.setDuration(HttpFileUploadManager.getVoiceLength(file.getPath()));
            } else {
                attachment.setRefType(ReferenceElement.Type.media.name());
                attachment.setDuration((long) 0);
            }

            if (isImage) {
                HttpFileUploadManager.ImageSize imageSize =
                        HttpFileUploadManager.getImageSizes(file.getPath());
                attachment.setImageHeight(imageSize.getHeight());
                attachment.setImageWidth(imageSize.getWidth());
            }
            attachments.add(attachment);
        }
        return attachments;
    }

    public RealmList<Attachment> attachmentsFromUris(List<Uri> uris) {
        RealmList<Attachment> attachments = new RealmList<>();
        for (Uri uri : uris) {
            Attachment attachment = new Attachment();
            attachment.setTitle(UriUtils.getFullFileName(uri));
            attachment.setIsImage(UriUtils.uriIsImage(uri));
            attachment.setMimeType(UriUtils.getMimeType(uri));
            attachment.setDuration((long) 0);
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * @return Whether chat accepts packets from specified user.
     */
    private boolean accept(UserJid jid) {
        return this.user.equals(jid);
    }

    @Nullable
    public synchronized MessageItem getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageItem lastMessage) {
        this.lastMessage = lastMessage;
    }

    private void updateLastMessage() {
        lastMessage = Realm.getDefaultInstance()
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                .equalTo(MessageItem.Fields.USER, user.toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.TEXT)
                .isNull(MessageItem.Fields.ACTION)
                .or()
                .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                .equalTo(MessageItem.Fields.USER, user.toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.TEXT)
                .isNotNull(MessageItem.Fields.ACTION)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING).last(null);
    }

    /**
     * @return Time of last message in chat. Can be <code>null</code>.
     */
    public Date getLastTime() {
        MessageItem lastMessage = getLastMessage();
        if (lastMessage != null) {
            return new Date(lastMessage.getTimestamp());
        } else {
            if (lastActionTimestamp != null) {
                return new Date(getLastActionTimestamp());
            }
            return null;
        }
    }

    public Long getLastActionTimestamp() {
        return lastActionTimestamp;
    }

    public void setLastActionTimestamp() {
        MessageItem lastMessage = getLastMessage();
        if (lastMessage != null) {
            lastActionTimestamp = lastMessage.getTimestamp();
        }
    }

    public void setLastActionTimestamp(Long timestamp) {
        lastActionTimestamp = timestamp;
    }

    public Message createMessagePacket(String body, String stanzaId) {
        Message message = createMessagePacket(body);
        if (stanzaId != null) message.setStanzaId(stanzaId);
        return message;
    }

    /**
     * @return New message packet to be sent.
     */
    public Message createMessagePacket(String body) {
        Message message = new Message();
        message.setTo(getTo());
        message.setType(getType());
        message.setBody(body);
        message.setThread(threadId);
        return message;
    }

    public Message createFileAndForwardMessagePacket(String stanzaId, RealmList<Attachment> attachments, String[] forwardIds, String text) {

        Message message = new Message();
        message.setTo(getTo());
        message.setType(getType());
        message.setThread(threadId);
        if (stanzaId != null) message.setStanzaId(stanzaId);

        StringBuilder builder = new StringBuilder();
        createForwardMessageReferences(message, forwardIds, builder);
        builder.append(text);
        createFileMessageReferences(message, attachments, builder);

        message.setBody(builder);
        return message;
    }

    /**
     * Send stanza with data-references.
     */
    public Message createFileMessagePacket(String stanzaId, RealmList<Attachment> attachments, String body) {

        Message message = new Message();
        message.setTo(getTo());
        message.setType(getType());
        message.setThread(threadId);
        if (stanzaId != null) message.setStanzaId(stanzaId);

        StringBuilder builder = new StringBuilder(body);
        createFileMessageReferences(message, attachments, builder);

        message.setBody(builder);
        return message;
    }

    public Message createForwardMessagePacket(String stanzaId, String[] forwardIds, String text) {
        Message message = new Message();
        message.setTo(getTo());
        message.setType(getType());
        message.setThread(threadId);
        if (stanzaId != null) message.setStanzaId(stanzaId);

        StringBuilder builder = new StringBuilder();
        createForwardMessageReferences(message, forwardIds, builder);
        builder.append(text);

        message.setBody(builder);
        return message;
    }

    private void createFileMessageReferences(Message message, RealmList<Attachment> attachments, StringBuilder builder) {
        for (Attachment attachment : attachments) {
            StringBuilder rowBuilder = new StringBuilder();
            if (builder.length() > 0) rowBuilder.append("\n");
            rowBuilder.append(attachment.getFileUrl());

            int begin = getSizeOfEncodedChars(builder.toString());
            builder.append(rowBuilder);
            ReferenceElement reference;
            if (attachment.isVoice()) {
                reference = ReferencesManager.createVoiceReferences(attachment,
                        begin, getSizeOfEncodedChars(builder.toString()) - 1);
            } else {
                reference = ReferencesManager.createMediaReferences(attachment,
                        begin, getSizeOfEncodedChars(builder.toString()) - 1);
            }
            message.addExtension(reference);
        }
    }

    private void createForwardMessageReferences(Message message, String[] forwardedIds, StringBuilder builder) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<MessageItem> items = realm.where(MessageItem.class)
                .in(MessageItem.Fields.UNIQUE_ID, forwardedIds).findAll();

        if (items != null && !items.isEmpty()) {
            for (MessageItem item : items) {
                String forward = ClipManager.createMessageTree(realm, item.getUniqueId()) + "\n";
                int begin = getSizeOfEncodedChars(builder.toString());
                builder.append(forward);
                ReferenceElement reference = ReferencesManager.createForwardReference(item,
                        begin, getSizeOfEncodedChars(builder.toString()) - 1);
                message.addExtension(reference);
            }
        }
        realm.close();
    }

    private int getSizeOfEncodedChars(String str) {
        return Utils.xmlEncode(str).toCharArray().length;
    }

    /**
     * Prepare text to be send.
     *
     * @return <code>null</code> if text shouldn't be send.
     */
    protected String prepareText(String text) {
        return text;
    }


    public void sendMessages() {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try{
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<MessageItem> messagesToSend = realm.where(MessageItem.class)
                                    .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                                    .equalTo(MessageItem.Fields.USER, user.toString())
                                    .equalTo(MessageItem.Fields.SENT, false)
                                    .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

                            for (final MessageItem messageItem : messagesToSend) {
                                if (messageItem.isInProgress()) continue;
                                if (!sendMessage(messageItem)) {
                                    break;
                                }
                            }
                        }
                    });

                } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            }
        });
    }

    protected boolean canSendMessage() {
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean sendMessage(MessageItem messageItem) {
        String text = prepareText(messageItem.getText());
        messageItem.setEncrypted(OTRManager.getInstance().isEncrypted(text));
        Long timestamp = messageItem.getTimestamp();

        Date currentTime = new Date(System.currentTimeMillis());
        Date delayTimestamp = null;

        if (timestamp != null) {
            if (currentTime.getTime() - timestamp > 60000) {
                delayTimestamp = currentTime;
            }
        }

        Message message = null;

        if (messageItem.haveAttachments()) {
            if (messageItem.haveForwardedMessages()) {
                message = createFileAndForwardMessagePacket(messageItem.getStanzaId(),
                        messageItem.getAttachments(), messageItem.getForwardedIdsAsArray(), text);
            } else {
                message = createFileMessagePacket(messageItem.getStanzaId(),
                        messageItem.getAttachments(), text);
            }
        } else if (messageItem.haveForwardedMessages()) {
            message = createForwardMessagePacket(messageItem.getStanzaId(), messageItem.getForwardedIdsAsArray(), text);
        } else if (text != null) {
            message = createMessagePacket(text, messageItem.getStanzaId());
        }

        if (message != null) {
            ChatStateManager.getInstance().updateOutgoingMessage(AbstractChat.this, message);
            CarbonManager.getInstance().updateOutgoingMessage(AbstractChat.this, message);
            LogManager.d(AbstractChat.class.toString(), "Message sent. Invoke CarbonManager updateOutgoingMessage");
            message.addExtension(new OriginIdElement(messageItem.getStanzaId()));
            if (ReliableMessageDeliveryManager.getInstance().isSupported(account))
                if (!messageItem.isDelivered() && messageItem.isSent())
                    message.addExtension(new RetryReceiptRequestElement());
                else message.addExtension(new ReceiptRequestElement());
            if (delayTimestamp != null) {
                message.addExtension(new DelayInformation(delayTimestamp));
            }

            final String messageId = messageItem.getUniqueId();
            try {
                StanzaSender.sendStanza(account, message, new StanzaListener() {
                    @Override
                    public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
                        Realm realm = Realm.getDefaultInstance();
                        realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    MessageItem acknowledgedMessage = realm
                                            .where(MessageItem.class)
                                            .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                                            .findFirst();

                                    if (acknowledgedMessage != null && !ReliableMessageDeliveryManager.getInstance().isSupported(account)) {
                                        acknowledgedMessage.setAcknowledged(true);
                                    }
                                }
                            });
                        realm.close();
                    }
                });
            } catch (NetworkException e) {
                return false;
            }
        }

        if (message == null) {
            messageItem.setError(true);
            messageItem.setErrorDescription("Internal error: message is null");
        } else {
            message.setFrom(account.getFullJid());
            messageItem.setOriginalStanza(message.toXML().toString());
        }

        if (delayTimestamp != null) {
            messageItem.setDelayTimestamp(delayTimestamp.getTime());
        }
        if (messageItem.getTimestamp() == null) {
            messageItem.setTimestamp(currentTime.getTime());
        }
        messageItem.setSent(true);
        return true;
    }

    public String getThreadId() {
        return threadId;
    }

    /**
     * Update thread id with new value.
     *
     * @param threadId <code>null</code> if current value shouldn't be changed.
     */
    protected void updateThreadId(String threadId) {
        if (threadId == null) {
            return;
        }
        this.threadId = threadId;
    }

    /**
     * Processes incoming packet.
     *
     * @param userJid
     * @param packet
     * @return Whether packet was directed to this chat.
     */
    protected boolean onPacket(UserJid userJid, Stanza packet, boolean isCarbons) {
        return accept(userJid);
    }

    /**
     * Connection complete.f
     */
    protected void onComplete() {
    }

    /**
     * Disconnection occured.
     */
    protected void onDisconnect() {
        setLastMessageId(null);
    }

    public void setIsPrivateMucChatAccepted(boolean isPrivateMucChatAccepted) {
        this.isPrivateMucChatAccepted = isPrivateMucChatAccepted;
    }

    boolean isPrivateMucChat() {
        return isPrivateMucChat;
    }

    boolean isPrivateMucChatAccepted() {
        return isPrivateMucChatAccepted;
    }

    @Override
    public void onChange(RealmResults<MessageItem> messageItems) {
        updateLastMessage();
        RosterCacheManager.saveLastMessageToContact(lastMessage);
    }

    /** UNREAD MESSAGES */

    public String getFirstUnreadMessageId() {
        String id = null;
        RealmResults<MessageItem> results = getAllUnreadAscending();
        if (results != null && !results.isEmpty()) {
            MessageItem firstUnreadMessage = results.first();
            if (firstUnreadMessage != null)
                id = firstUnreadMessage.getUniqueId();
        }
        return id;
    }

    public int getUnreadMessageCount() {
        int unread = ((int) getAllUnreadQuery().count()) - waitToMarkAsRead.size();
        if (unread < 0) unread = 0;
        if (getLastMessage() != null && !getLastMessage().isIncoming()) unread = 0;
        return unread;
    }

    public int getwaitToMarkAsReadCount(){ return waitToMarkAsRead.size(); }

    public void approveRead(List<String> ids) {
        for (String id : ids) {
            waitToMarkAsRead.remove(id);
        }
        EventBus.getDefault().post(new MessageUpdateEvent(account, user));
    }

    //public void markAsRead(String messageId, boolean trySendDisplay) {
    //    MessageItem message = Realm.getDefaultInstance()
    //            .where(MessageItem.class)
    //            .equalTo(MessageItem.Fields.STANZA_ID, messageId)
    //            .findFirst();
    //    if (message != null) executeRead(message, trySendDisplay);
    //}

    public void markAsRead(String messageId, ArrayList<String> stanzaId, boolean trySendDisplay) {
        executeRead(messageId, stanzaId, trySendDisplay);
    }

    private void executeRead(String messageId, ArrayList<String> stanzaId, boolean trySendDisplay) {
        EventBus.getDefault().post(new MessageUpdateEvent(account, user));
        BackpressureMessageReader.getInstance().markAsRead(messageId, stanzaId, account, user, trySendDisplay);
    }

    public void markAsRead(MessageItem messageItem, boolean trySendDisplay) {
        waitToMarkAsRead.add(messageItem.getUniqueId());
        executeRead(messageItem, trySendDisplay);
    }

    public void markAsReadAll(boolean trySendDisplay) {
        RealmResults<MessageItem> results = getAllUnreadAscending();
        if (results != null && !results.isEmpty()) {
            for (MessageItem message : results) {
                waitToMarkAsRead.add(message.getUniqueId());
            }
            MessageItem lastMessage = results.last();
            if (lastMessage != null) executeRead(lastMessage, trySendDisplay);
        }
    }

    private void executeRead(MessageItem messageItem, boolean trySendDisplay) {
        EventBus.getDefault().post(new MessageUpdateEvent(account, user));
        BackpressureMessageReader.getInstance().markAsRead(messageItem, trySendDisplay);
    }

    private RealmQuery<MessageItem> getAllUnreadQuery() {
        return Realm.getDefaultInstance()
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                .equalTo(MessageItem.Fields.USER, user.toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.TEXT)
                .equalTo(MessageItem.Fields.INCOMING, true)
                .equalTo(MessageItem.Fields.READ, false);
    }

    private RealmResults<MessageItem> getAllUnreadAscending() {
        return getAllUnreadQuery().findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
    }

    /** ^ UNREAD MESSAGES ^ */

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived, boolean needSaveToRealm) {
        this.archived = archived;
        if (needSaveToRealm) ChatManager.getInstance().saveOrUpdateChatDataToRealm(this);
    }

    public void setAddContactSuggested(boolean suggested) {
        addContactSuggested = suggested;
    }

    public boolean isAddContactSuggested() {
        return addContactSuggested;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState, boolean needSaveToRealm) {
        this.notificationState = notificationState;
        if (notificationState.getMode() == NotificationState.NotificationMode.disabled && needSaveToRealm)
            NotificationManager.getInstance().removeMessageNotification(account, user);
        if (needSaveToRealm) ChatManager.getInstance().saveOrUpdateChatDataToRealm(this);
    }

    public void setNotificationStateOrDefault(NotificationState notificationState, boolean needSaveToRealm) {
        if (notificationState.getMode() != NotificationState.NotificationMode.enabled
                && notificationState.getMode() != NotificationState.NotificationMode.disabled)
            throw new IllegalStateException("In this method mode must be enabled or disabled.");

        if (!eventsOnChatGlobal() && notificationState.getMode() == NotificationState.NotificationMode.disabled
                || eventsOnChatGlobal() && notificationState.getMode() == NotificationState.NotificationMode.enabled)
            notificationState.setMode(NotificationState.NotificationMode.bydefault);

        setNotificationState(notificationState, needSaveToRealm);
    }

    private boolean eventsOnChatGlobal() {
        if (MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible()))
            return SettingsManager.eventsOnMuc();
        else return SettingsManager.eventsOnChat();
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void saveLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
        ChatManager.getInstance().saveOrUpdateChatDataToRealm(this);
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public RealmList<ForwardId> parseForwardedMessage(boolean ui, Stanza packet, String parentMessageId) {
        List<Forwarded> forwarded = ReferencesManager.getForwardedFromReferences(packet);
        if (forwarded.isEmpty()) forwarded = ForwardManager.getForwardedFromStanza(packet);
        if (forwarded.isEmpty()) return null;

        RealmList<ForwardId> forwardedIds = new RealmList<>();
        for (Forwarded forward : forwarded) {
            Stanza stanza = forward.getForwardedStanza();
            DelayInformation delayInformation = forward.getDelayInformation();
            Date timestamp = delayInformation.getStamp();
            if (stanza instanceof Message) {
                forwardedIds.add(new ForwardId(parseInnerMessage(ui, (Message) stanza, timestamp, parentMessageId)));
            }
        }
        return forwardedIds;
    }

    protected abstract String parseInnerMessage(boolean ui, Message message, Date timestamp, String parentMessageId);

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public boolean historyIsFull() {
        return historyIsFull;
    }

    public void setHistoryIsFull() {
        this.historyIsFull = true;
    }

    public boolean isHistoryRequestedAtStart() {
        return historyRequestedAtStart;
    }

    public void setHistoryRequestedAtStart(boolean needSaveToRealm) {
        this.historyRequestedAtStart = true;
        if (needSaveToRealm) ChatManager.getInstance().saveOrUpdateChatDataToRealm(this);
    }

    public static String getStanzaId(Message message) {
        String stanzaId = null;

        stanzaId = UniqStanzaHelper.getStanzaId(message);
        if (stanzaId != null && !stanzaId.isEmpty()) return stanzaId;

        stanzaId = message.getStanzaId();
        if (stanzaId != null && !stanzaId.isEmpty()) return stanzaId;

        stanzaId = UniqStanzaHelper.getOriginId(message);
        if (stanzaId != null && !stanzaId.isEmpty()) return stanzaId;

        return stanzaId;
    }

    public static Date getDelayStamp(Message message) {
        DelayInformation delayInformation = DelayInformation.from(message);
        if (delayInformation != null) {
            return delayInformation.getStamp();
        } else {
            return null;
        }
    }

    public boolean isGroupchat() {
        return isGroupchat;
    }

    public void setGroupchat(boolean isGroupchat) {
        this.isGroupchat = isGroupchat;
    }

    public int getChatstateMode() {
        return chatstateType;
    }

    public void setChatstate(int chatstateType) {
        this.chatstateType = chatstateType;
    }

    public static class ChatstateType {
        public static final int NORMAL = 0;
        public static final int CLEARED_HISTORY = 1;
        public static final int DELETED = 2;
    }
}