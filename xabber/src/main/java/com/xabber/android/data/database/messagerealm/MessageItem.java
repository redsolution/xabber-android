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
package com.xabber.android.data.database.messagerealm;

import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Arrays;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class MessageItem extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String RESOURCE = "resource";
        public static final String TEXT = "text";
        public static final String MARKUP_TEXT = "markupText";
        public static final String ACTION = "action";
        public static final String INCOMING = "incoming";
        public static final String ENCRYPTED = "encrypted";
        public static final String UNENCRYPTED = "unencrypted"; // deprecated
        public static final String OFFLINE = "offline";
        public static final String TIMESTAMP = "timestamp";
        public static final String DELAY_TIMESTAMP = "delayTimestamp";
        public static final String EDITED_TIMESTAMP = "editedTimestamp";
        public static final String ERROR = "error";
        public static final String ERROR_DESCR = "errorDescription";
        public static final String DELIVERED = "delivered";
        public static final String DISPLAYED = "displayed";
        public static final String SENT = "sent";
        public static final String READ = "read";
        public static final String STANZA_ID = "stanzaId";
        public static final String ORIGIN_ID = "originId";
        public static final String IS_RECEIVED_FROM_MAM = "isReceivedFromMessageArchive";
        public static final String FORWARDED = "forwarded";
        public static final String FILE_PATH = "filePath";
        public static final String FILE_URL = "fileUrl";
        public static final String FILE_SIZE = "fileSize";
        public static final String IS_IMAGE = "isImage";
        public static final String IMAGE_WIDTH = "imageWidth";
        public static final String IMAGE_HEIGHT = "imageHeight";
        public static final String ACKNOWLEDGED = "acknowledged";
        public static final String IS_IN_PROGRESS = "isInProgress";
        public static final String ATTACHMENTS = "attachments";
        public static final String FORWARDED_IDS = "forwardedIds";
        public static final String ORIGINAL_STANZA = "originalStanza";
        public static final String ORIGINAL_FROM = "originalFrom";
        public static final String PARENT_MESSAGE_ID = "parentMessageId";
        public static final String FROM_MUC = "fromMUC";
        public static final String PREVIOUS_ID = "previousId";
        public static final String ARCHIVED_ID = "archivedId";
        public static final String GROUPCHAT_USER_ID = "groupchatUserId";
    }

    /**
     * UUID
     */

    @PrimaryKey
    @Required
    private String uniqueId;

    @Index
    private String account;
    @Index
    private String user;

    /**
     * Contact's resource.
     */
    private String resource;
    /**
     * Text representation.
     */
    private String text;
    private String markupText;
    /**
     * Optional action. If set message represent not an actual message but some
     * action in the chat.
     */
    private String action;

    private boolean incoming;

    private boolean encrypted;

    /**
     * Message was received from server side offline storage.
     */
    private boolean offline;

    /**
     * Time when message was received or sent by Xabber.
     * Realm truncated Date type to seconds, using long for accuracy
     */
    @Index
    private Long timestamp;
    /**
     * RIme when message was edited
     * Realm truncated Date type to seconds, using long for accuracy
     */
    private Long editedTimestamp;
    /**
     * Time when message was created.
     * Realm truncated Date type to seconds, using long for accuracy
     */
    private Long delayTimestamp;
    /**
     * Error response received on send request.
     */
    private boolean error;
    private String errorDescription;
    /**
     * ReceiptElement was received for sent message.
     */
    private boolean delivered;
    /**
     * Chat marker was received for sent message.
     */
    private boolean displayed;
    /**
     * Message was sent.
     */
    @Index
    private boolean sent;
    /**
     * Message was shown to the user.
     */
    private boolean read;
    /**
     * Outgoing packet id - usual message stanza (packet) id
     */
    private String stanzaId;
    /**
     * Internal packet id
     */
    private String originId;

    /**
     * If message was received from server message archive (XEP-0313)
     */
    private boolean isReceivedFromMessageArchive;

    /**
     * If message was forwarded (e.g. message carbons (XEP-0280))
     */
    private boolean forwarded;

    /**
     * If message text contains url to file
     */
    @Deprecated
    private String fileUrl;

    /**
     * If message "contains" file with local file path
     */
    @Deprecated
    private String filePath;

    /**
     * If message contains URL to image (and may be drawn as image)
     */
    @Deprecated
    private boolean isImage;

    @Deprecated
    @Nullable
    private Integer imageWidth;

    @Deprecated
    @Nullable
    private Integer imageHeight;

    @Deprecated
    private Long fileSize;

    /**
     * If message was acknowledged by server (XEP-0198: Stream Management)
     */
    private boolean acknowledged;

    /**
     * If message is currently in progress (i.e. file is uploading/downloading)
     */
    private boolean isInProgress;

    private RealmList<Attachment> attachments;

    /** Message forwarding */

    private String originalStanza;

    /** If message was forwarded contains jid of original message author */
    private String originalFrom;

    private String parentMessageId;
    private String previousId;
    private String archivedId;
    @Ignore
    private String packetId;
    private String groupchatUserId;

    private RealmList<ForwardId> forwardedIds;

    private boolean fromMUC;

    public MessageItem(String uniqueId) { this.uniqueId = uniqueId; }

    public MessageItem() { this.uniqueId = UUID.randomUUID().toString(); }

    public String getUniqueId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.uniqueId = uniqueId;
    }

    public AccountJid getAccount() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setAccount(AccountJid account) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.account = account.toString();
    }

    public UserJid getUser() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        try {
            return UserJid.from(user);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(UserJid user) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.user = user.toString();
    }

    public Resourcepart getResource() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        if (TextUtils.isEmpty(resource)) {
            return Resourcepart.EMPTY;
        }

        try {
            return Resourcepart.from(resource);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            return Resourcepart.EMPTY;
        }
    }

    public void setResource(Resourcepart resource) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        if (resource != null) {
            this.resource = resource.toString();
        } else {
            this.resource = Resourcepart.EMPTY.toString();
        }
    }

    public String getText() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return text;
    }

    public void setText(String text) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.text = text;
    }

    public String getAction() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return action;
    }

    public void setAction(String action) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.action = action;
    }

    public boolean isIncoming() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.incoming = incoming;
    }

    public boolean isOffline() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return offline;
    }

    public void setOffline(boolean offline) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.offline = offline;
    }

    public Long getTimestamp() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.timestamp = timestamp;
    }

    public Long getEditedTimestamp() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return editedTimestamp;
    }

    public void setEditedTimestamp(Long timestamp) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.editedTimestamp = timestamp;
    }

    public Long getDelayTimestamp() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return delayTimestamp;
    }

    public void setDelayTimestamp(Long delayTimestamp) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.delayTimestamp = delayTimestamp;
    }

    public boolean isError() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return error;
    }

    public void setError(boolean error) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.error = error;
    }

    public boolean isDelivered() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.delivered = delivered;
    }

    public boolean isDisplayed() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return displayed;
    }

    public void setDisplayed(boolean displayed) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.displayed = displayed;
    }

    public boolean isSent() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return sent;
    }

    public void setSent(boolean sent) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.sent = sent;
    }

    public boolean isRead() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return read;
    }

    public void setRead(boolean read) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.read = read;
    }

    public String getStanzaId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return stanzaId;
    }

    public void setStanzaId(String stanzaId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.stanzaId = stanzaId;
    }

    public String getOriginId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return originId;
    }

    public void setOriginId(String originId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.originId = originId;
    }

    public boolean isReceivedFromMessageArchive() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return isReceivedFromMessageArchive;
    }

    public void setReceivedFromMessageArchive(boolean receivedFromMessageArchive) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        isReceivedFromMessageArchive = receivedFromMessageArchive;
    }

    public boolean isForwarded() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.forwarded = forwarded;
    }

    @Deprecated
    public String getFilePath() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return filePath;
    }

    @Deprecated
    public void setFilePath(String filePath) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.filePath = filePath;
    }

    @Deprecated
    public boolean isImage() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return isImage;
    }

    @Deprecated
    public void setIsImage(boolean isImage) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.isImage = isImage;
    }

    @Deprecated
    @Nullable
    public Integer getImageWidth() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return imageWidth;
    }

    @Deprecated
    public void setImageWidth(@Nullable Integer imageWidth) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.imageWidth = imageWidth;
    }

    @Deprecated
    @Nullable
    public Integer getImageHeight() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return imageHeight;
    }

    @Deprecated
    public void setImageHeight(@Nullable Integer imageHeight) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.imageHeight = imageHeight;
    }

    @Deprecated
    public String getFileUrl() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return fileUrl;
    }

    @Deprecated
    public void setFileUrl(String fileUrl) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.fileUrl = fileUrl;
    }

    @Deprecated
    public Long getFileSize() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return fileSize;
    }

    @Deprecated
    public void setFileSize(Long fileSize) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.fileSize = fileSize;
    }

    public static ChatAction getChatAction(MessageItem messageItem) {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return ChatAction.valueOf(messageItem.getAction());
    }

    public static Spannable getSpannable(MessageItem messageItem) {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return new SpannableString(messageItem.getText());
    }

    public static boolean isUploadFileMessage(MessageItem messageItem) {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return messageItem.getFilePath() != null && !messageItem.isIncoming() && !messageItem.isSent();
    }

    public boolean isAcknowledged() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.acknowledged = acknowledged;
    }

    public boolean isInProgress() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return isInProgress;
    }

    public void setInProgress(boolean inProgress) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        isInProgress = inProgress;
    }

    public boolean isEncrypted() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.encrypted = encrypted;
    }

    public String getErrorDescription() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.errorDescription = errorDescription;
    }

    public RealmList<Attachment> getAttachments() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return attachments;
    }

    public void setAttachments(RealmList<Attachment> attachments) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.attachments = attachments;
    }

    public boolean haveAttachments() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return attachments != null && attachments.size() > 0;
    }

    public RealmList<ForwardId> getForwardedIds() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return forwardedIds;
    }

    public String[] getForwardedIdsAsArray() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        String forwardedIds[] = new String[getForwardedIds().size()];

        int i = 0;
        for (ForwardId id : getForwardedIds()) {
            forwardedIds[i] = id.getForwardMessageId();
            i++;
        }

        return forwardedIds;
    }

    public void setForwardedIds(RealmList<ForwardId> forwardedMessages) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.forwardedIds = forwardedMessages;
    }

    public boolean haveForwardedMessages() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return forwardedIds != null && forwardedIds.size() > 0;
    }

    public String getOriginalStanza() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return originalStanza;
    }

    public void setOriginalStanza(String originalStanza) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.originalStanza = originalStanza;
    }

    public String getOriginalFrom() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return originalFrom;
    }

    public void setOriginalFrom(String originalFrom) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.originalFrom = originalFrom;
    }

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.parentMessageId = parentMessageId;
    }

    public String getPreviousId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return previousId;
    }

    public void setPreviousId(String previousId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.previousId = previousId;
    }

    public String getArchivedId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return archivedId;
    }

    public void setArchivedId(String archivedId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.archivedId = archivedId;
    }

    public String getPacketId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return packetId;
    }

    public void setPacketId(String packetId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.packetId = packetId;
    }

    public boolean isFromMUC() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return fromMUC;
    }

    public void setFromMUC(boolean fromMUC) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.fromMUC = fromMUC;
    }

    public String getMarkupText() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return markupText;
    }

    public void setMarkupText(String markupText) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.markupText = markupText;
    }

    public String getGroupchatUserId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return groupchatUserId;
    }

    public void setGroupchatUserId(String groupchatUserId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.groupchatUserId = groupchatUserId;
    }

    public String getFirstForwardedMessageText() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return getFirstForwardedMessageText(-1);
    }

    public String getFirstForwardedMessageText(int color) {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        String text = null;
        if (haveForwardedMessages()) {
            String[] forwardedIDs = getForwardedIdsAsArray();
            if (!Arrays.asList(forwardedIDs).contains(null)) {
                RealmResults<MessageItem> forwardedMessages =
                        MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                                .in(MessageItem.Fields.UNIQUE_ID, forwardedIDs)
                                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

                if (forwardedMessages.size() > 0) {
                    MessageItem message = forwardedMessages.last();
                    String author = RosterManager.getDisplayAuthorName(message);
                    StringBuilder stringBuilder = new StringBuilder();
                    if (author!=null && !author.isEmpty()) {
                        if (color == -1) {
                            stringBuilder.append(author);
                            stringBuilder.append(":");
                        } else stringBuilder.append(StringUtils.getColoredText(author + ":", color));
                    }
                    stringBuilder.append(message.getText().trim()).append(" ");
                    String attachmentName = "";
                    if (message.haveAttachments() && message.getAttachments().size() > 0) {
                        Attachment attachment = message.getAttachments().get(0);
                        attachmentName = StringUtils.getColoredText(attachment.getTitle().trim(), color);
                        stringBuilder.append(attachmentName);
                    }
                    if (!message.getText().trim().isEmpty() || !attachmentName.equals(""))
                        text = stringBuilder.toString();
                }
            }
        }
        return text;
    }

    public boolean isAttachmentImageOnly(){
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        if(attachments!=null && attachments.size()>0) {
            for (Attachment a : attachments) {
                if (!a.isImage()) {
                    return false;
                }
            } return true;
        }
        return false;
    }

    public boolean hasImage(){
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(MessageItem.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        if(attachments!=null && attachments.size()>0) {
            for (Attachment a : attachments) {
                if (a.isImage()) {
                    return true;
                }
            }
        }
        return false;
    }
}
