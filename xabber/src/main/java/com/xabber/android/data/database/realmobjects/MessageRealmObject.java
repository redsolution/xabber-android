/*
  Copyright (c) 2013, Redsolution LTD. All rights reserved.

  This file is part of Xabber project; you can redistribute it and/or
  modify it under the terms of the GNU General Public License, Version 3.

  Xabber is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License,
  along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.database.realmobjects;

import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Arrays;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

@SuppressWarnings("unused")
public class MessageRealmObject extends RealmObject {

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
        public static final String ACKNOWLEDGED = "acknowledged";
        public static final String IS_IN_PROGRESS = "isInProgress";
        public static final String ATTACHMENTS = "attachments";
        public static final String FORWARDED_IDS = "forwardedIds";
        public static final String ORIGINAL_STANZA = "originalStanza";
        public static final String ORIGINAL_FROM = "originalFrom";
        public static final String PARENT_MESSAGE_ID = "parentMessageId";
        public static final String PREVIOUS_ID = "previousId";
        public static final String GROUPCHAT_USER_ID = "groupchatUserId";
        public static final String IS_GROUPCHAT_SYSTEM = "isGroupchatSystem";
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
     * If message was acknowledged by server (XEP-0198: Stream Management)
     */
    private boolean acknowledged;

    /**
     * If message is currently in progress (i.e. file is uploading/downloading)
     */
    private boolean isInProgress;

    private RealmList<AttachmentRealmObject> attachmentRealmObjects;

    /** Message forwarding */

    private String originalStanza;

    /** If message was forwarded contains jid of original message author */
    private String originalFrom;

    private String parentMessageId;
    private String previousId;
    @Ignore
    private String packetId;
    private String groupchatUserId;
    private boolean isGroupchatSystem = false;

    private RealmList<ForwardIdRealmObject> forwardedIds;

    public MessageRealmObject(String uniqueId) { this.uniqueId = uniqueId; }

    public MessageRealmObject() { this.uniqueId = UUID.randomUUID().toString(); }

    public String getUniqueId() { return uniqueId; }

    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

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

    public ContactJid getUser() {
        try {
            return ContactJid.from(user);
        } catch (ContactJid.ContactJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(ContactJid user) { this.user = user.toString(); }

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

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public String getAction() { return action; }

    public void setAction(String action) { this.action = action; }

    public boolean isIncoming() { return incoming; }

    public void setIncoming(boolean incoming) { this.incoming = incoming; }

    public boolean isOffline() { return offline; }

    public void setOffline(boolean offline) { this.offline = offline; }

    public Long getTimestamp() { return timestamp; }

    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Long getEditedTimestamp() { return editedTimestamp; }

    public void setEditedTimestamp(Long timestamp) { this.editedTimestamp = timestamp; }

    public Long getDelayTimestamp() { return delayTimestamp; }

    public void setDelayTimestamp(Long delayTimestamp) { this.delayTimestamp = delayTimestamp; }

    public boolean isError() { return error; }

    public void setError(boolean error) { this.error = error; }

    public boolean isDelivered() { return delivered; }

    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public boolean isDisplayed() { return displayed; }

    public void setDisplayed(boolean displayed) { this.displayed = displayed; }

    public boolean isSent() { return sent; }

    public void setSent(boolean sent) { this.sent = sent; }

    public boolean isRead() { return read; }

    public void setRead(boolean read) { this.read = read; }

    public String getStanzaId() { return stanzaId; }

    public void setStanzaId(String stanzaId) { this.stanzaId = stanzaId; }

    public String getOriginId() { return originId; }

    public void setOriginId(String originId) { this.originId = originId; }

    public boolean isReceivedFromMessageArchive() { return isReceivedFromMessageArchive; }

    public void setReceivedFromMessageArchive(boolean receivedFromMessageArchive) { isReceivedFromMessageArchive = receivedFromMessageArchive; }

    public boolean isForwarded() { return forwarded; }

    public void setForwarded(boolean forwarded) { this.forwarded = forwarded; }

    public static ChatAction getChatAction(MessageRealmObject messageRealmObject) { return ChatAction.valueOf(messageRealmObject.getAction()); }

    public static Spannable getSpannable(MessageRealmObject messageRealmObject) { return new SpannableString(messageRealmObject.getText()); }

    public static boolean isUploadFileMessage(MessageRealmObject messageRealmObject) {
        return messageRealmObject.getAttachmentRealmObjects() != null
                && messageRealmObject.getAttachmentRealmObjects().size() != 0
                && !messageRealmObject.isSent(); }

    public boolean isAcknowledged() { return acknowledged; }

    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public boolean isInProgress() { return isInProgress; }

    public void setInProgress(boolean inProgress) {  isInProgress = inProgress; }

    public boolean isEncrypted() { return encrypted; }

    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public String getErrorDescription() { return errorDescription; }

    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

    public RealmList<AttachmentRealmObject> getAttachmentRealmObjects() { return attachmentRealmObjects; }

    public void setAttachmentRealmObjects(RealmList<AttachmentRealmObject> attachmentRealmObjects) {
        this.attachmentRealmObjects = attachmentRealmObjects;
    }

    public boolean haveAttachments() { return attachmentRealmObjects != null && attachmentRealmObjects.size() > 0; }

    public RealmList<ForwardIdRealmObject> getForwardedIds() { return forwardedIds; }

    public String[] getForwardedIdsAsArray() {
        String[] forwardedIds = new String[getForwardedIds().size()];

        int i = 0;
        for (ForwardIdRealmObject id : getForwardedIds()) {
            forwardedIds[i] = id.getForwardMessageId();
            i++;
        }

        return forwardedIds;
    }

    public void setForwardedIds(RealmList<ForwardIdRealmObject> forwardedMessages) { this.forwardedIds = forwardedMessages; }

    public boolean haveForwardedMessages() { return forwardedIds != null && forwardedIds.size() > 0; }

    public String getOriginalStanza() { return originalStanza; }

    public void setOriginalStanza(String originalStanza) { this.originalStanza = originalStanza; }

    public String getOriginalFrom() { return originalFrom; }

    public void setOriginalFrom(String originalFrom) { this.originalFrom = originalFrom; }

    public String getParentMessageId() { return parentMessageId; }

    public void setParentMessageId(String parentMessageId) { this.parentMessageId = parentMessageId; }

    public String getPreviousId() { return previousId; }

    public void setPreviousId(String previousId) { this.previousId = previousId; }

    public String getPacketId() { return packetId; }

    public void setPacketId(String packetId) { this.packetId = packetId; }

    public String getMarkupText() { return markupText; }

    public void setMarkupText(String markupText) { this.markupText = markupText; }

    public String getGroupchatUserId() { return groupchatUserId; }

    public void setGroupchatUserId(String groupchatUserId) { this.groupchatUserId = groupchatUserId; }

    public boolean isGroupchatSystem() { return isGroupchatSystem; }

    public void setGroupchatSystem(boolean groupchatSystem) {
        isGroupchatSystem = groupchatSystem;
        read = true;
    }

    public String getFirstForwardedMessageText() { return getFirstForwardedMessageText(-1); }

    public String getFirstForwardedMessageText(int color) {
        String text = null;
        if (haveForwardedMessages()) {
            String[] forwardedIDs = getForwardedIdsAsArray();
            if (!Arrays.asList(forwardedIDs).contains(null)) {
                Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                RealmResults<MessageRealmObject> forwardedMessages = realm
                        .where(MessageRealmObject.class)
                        .in(MessageRealmObject.Fields.UNIQUE_ID, forwardedIDs)
                        .findAll()
                        .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);


                if (forwardedMessages.size() > 0) {
                    MessageRealmObject message = forwardedMessages.last();
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
                    if (message.haveAttachments() && message.getAttachmentRealmObjects().size() > 0) {
                        AttachmentRealmObject attachmentRealmObject = message.getAttachmentRealmObjects().get(0);
                        attachmentName = StringUtils.getColoredText(attachmentRealmObject.getTitle().trim(), color);
                        stringBuilder.append(attachmentName);
                    }
                    if (!message.getText().trim().isEmpty() || !attachmentName.equals(""))
                        text = stringBuilder.toString();
                }
                if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
            }
        }
        return text;
    }

    public boolean isAttachmentImageOnly(){
        if(attachmentRealmObjects !=null && attachmentRealmObjects.size()>0) {
            for (AttachmentRealmObject a : attachmentRealmObjects) {
                if (!a.isImage()) return false;
            }
            return true;
        }
        return false;
    }

    public boolean hasImage(){
         
        if(attachmentRealmObjects !=null && attachmentRealmObjects.size()>0) {
            for (AttachmentRealmObject a : attachmentRealmObjects) {
                if (a.isImage()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUiEqual(MessageRealmObject comparableMessageRealmObject){
        return this.getText().equals(comparableMessageRealmObject.getText())
                && this.isIncoming() == comparableMessageRealmObject.isIncoming()
                && this.getTimestamp().equals(comparableMessageRealmObject.getTimestamp())
                && this.isError() == comparableMessageRealmObject.isError()
                && this.isDelivered() == comparableMessageRealmObject.isDelivered()
                && this.isDisplayed() == comparableMessageRealmObject.isDisplayed()
                && this.isSent() == comparableMessageRealmObject.isSent()
                && this.isRead() == comparableMessageRealmObject.isRead()
                && this.isAcknowledged() == comparableMessageRealmObject.isAcknowledged();
    }
}