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
package com.xabber.android.data.database.realm;

import android.text.Spannable;
import android.text.SpannableString;

import com.xabber.android.data.message.ChatAction;

import java.io.File;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class MessageItem extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String RESOURCE = "resource";
        public static final String TEXT = "text";
        public static final String ACTION = "action";
        public static final String INCOMING = "incoming";
        public static final String UNENCRYPTED = "unencrypted";
        public static final String OFFLINE = "offline";
        public static final String TIMESTAMP = "timestamp";
        public static final String DELAY_TIMESTAMP = "delayTimestamp";
        public static final String ERROR = "error";
        public static final String DELIVERED = "delivered";
        public static final String SENT = "sent";
        public static final String READ = "read";
        public static final String STANZA_ID = "stanzaId";
        public static final String IS_RECEIVED_FROM_MAM = "isReceivedFromMessageArchive";
        public static final String FORWARDED = "forwarded";
        public static final String FILE_PATH = "filePath";
        public static final String FILE_SIZE = "fileSize";

    }

    /**
     * UUID
     */

    @PrimaryKey
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

    private boolean unencrypted;
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
    /**
     * Receipt was received for sent message.
     */
    private boolean delivered;
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

    private String filePath;

    private Long fileSize;


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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
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

    public boolean isUnencrypted() {
        return unencrypted;
    }

    public void setUnencrypted(boolean unencrypted) {
        this.unencrypted = unencrypted;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public static String getDisplayText(MessageItem messageItem) {
        String filePath = messageItem.getFilePath();

        if (filePath != null) {
            return new File(filePath).getName();
        } else {
        return messageItem.getText();
        }
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
}
