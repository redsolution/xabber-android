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

import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ChatAction;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
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
        public static final String ACTION = "action";
        public static final String INCOMING = "incoming";
        public static final String ENCRYPTED = "encrypted";
        public static final String UNENCRYPTED = "unencrypted"; // deprecated
        public static final String OFFLINE = "offline";
        public static final String TIMESTAMP = "timestamp";
        public static final String DELAY_TIMESTAMP = "delayTimestamp";
        public static final String ERROR = "error";
        public static final String ERROR_DESCR = "errorDescription";
        public static final String DELIVERED = "delivered";
        public static final String DISPLAYED = "displayed";
        public static final String SENT = "sent";
        public static final String READ = "read";
        public static final String STANZA_ID = "stanzaId";
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
     * Receipt was received for sent message.
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

    private RealmList<ForwardId> forwardedIds;

    private boolean fromMUC;

    public MessageItem(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public MessageItem() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public AccountJid getAccount() {
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setAccount(AccountJid account) {
        this.account = account.toString();
    }

    public UserJid getUser() {
        try {
            return UserJid.from(user);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(UserJid user) {
        this.user = user.toString();
    }

    public Resourcepart getResource() {
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
        if (resource != null) {
            this.resource = resource.toString();
        } else {
            this.resource = Resourcepart.EMPTY.toString();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDelayTimestamp() {
        return delayTimestamp;
    }

    public void setDelayTimestamp(Long delayTimestamp) {
        this.delayTimestamp = delayTimestamp;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getStanzaId() {
        return stanzaId;
    }

    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    public boolean isReceivedFromMessageArchive() {
        return isReceivedFromMessageArchive;
    }

    public void setReceivedFromMessageArchive(boolean receivedFromMessageArchive) {
        isReceivedFromMessageArchive = receivedFromMessageArchive;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    @Deprecated
    public String getFilePath() {
        return filePath;
    }

    @Deprecated
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Deprecated
    public boolean isImage() {
        return isImage;
    }

    @Deprecated
    public void setIsImage(boolean isImage) {
        this.isImage = isImage;
    }

    @Deprecated
    @Nullable
    public Integer getImageWidth() {
        return imageWidth;
    }

    @Deprecated
    public void setImageWidth(@Nullable Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    @Deprecated
    @Nullable
    public Integer getImageHeight() {
        return imageHeight;
    }

    @Deprecated
    public void setImageHeight(@Nullable Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    @Deprecated
    public String getFileUrl() {
        return fileUrl;
    }

    @Deprecated
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Deprecated
    public Long getFileSize() {
        return fileSize;
    }

    @Deprecated
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public static ChatAction getChatAction(MessageItem messageItem) {
        return ChatAction.valueOf(messageItem.getAction());
    }

    public static Spannable getSpannable(MessageItem messageItem) {
        return new SpannableString(messageItem.getText());
    }

    public static boolean isUploadFileMessage(MessageItem messageItem) {
        return messageItem.getFilePath() != null && !messageItem.isIncoming() && !messageItem.isSent();
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    public void setInProgress(boolean inProgress) {
        isInProgress = inProgress;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public RealmList<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(RealmList<Attachment> attachments) {
        this.attachments = attachments;
    }

    public boolean haveAttachments() {
        return attachments != null && attachments.size() > 0;
    }

    public RealmList<ForwardId> getForwardedIds() {
        return forwardedIds;
    }

    public String[] getForwardedIdsAsArray() {
        String forwardedIds[] = new String[getForwardedIds().size()];

        int i = 0;
        for (ForwardId id : getForwardedIds()) {
            forwardedIds[i] = id.getForwardMessageId();
            i++;
        }

        return forwardedIds;
    }

    public void setForwardedIds(RealmList<ForwardId> forwardedMessages) {
        this.forwardedIds = forwardedMessages;
    }

    public boolean haveForwardedMessages() {
        return forwardedIds != null && forwardedIds.size() > 0;
    }

    public String getOriginalStanza() {
        return originalStanza;
    }

    public void setOriginalStanza(String originalStanza) {
        this.originalStanza = originalStanza;
    }

    public String getOriginalFrom() {
        return originalFrom;
    }

    public void setOriginalFrom(String originalFrom) {
        this.originalFrom = originalFrom;
    }

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getPreviousId() {
        return previousId;
    }

    public void setPreviousId(String previousId) {
        this.previousId = previousId;
    }

    public String getArchivedId() {
        return archivedId;
    }

    public void setArchivedId(String archivedId) {
        this.archivedId = archivedId;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public boolean isFromMUC() {
        return fromMUC;
    }

    public void setFromMUC(boolean fromMUC) {
        this.fromMUC = fromMUC;
    }
}
