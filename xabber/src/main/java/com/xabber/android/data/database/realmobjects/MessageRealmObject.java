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
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.text.StringUtilsKt;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Arrays;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

@SuppressWarnings("FieldMayBeFinal")
public class MessageRealmObject extends RealmObject {

    @SuppressWarnings("unused")
    public static class Fields {
        public static final String PRIMARY_KEY = "primaryKey";
        public static final String STANZA_ID = "stanzaId";
        public static final String ORIGIN_ID = "originId";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String RESOURCE = "resource";
        public static final String TEXT = "text";
        public static final String MARKUP_TEXT = "markupText";
        public static final String ACTION = "action";
        public static final String INCOMING = "incoming";
        public static final String ENCRYPTED = "encrypted";
        public static final String TIMESTAMP = "timestamp";
        public static final String DELAY_TIMESTAMP = "delayTimestamp";
        public static final String EDITED_TIMESTAMP = "editedTimestamp";
        public static final String ERROR = "error";
        public static final String ERROR_DESCR = "errorDescription";
        public static final String MESSAGE_STATUS = "messageStatus";
        public static final String READ = "read";
        public static final String FORWARDED = "forwarded";
        public static final String ATTACHMENTS = "attachments";
        public static final String FORWARDED_IDS = "forwardedIds";
        public static final String ORIGINAL_STANZA = "originalStanza";
        public static final String ORIGINAL_FROM = "originalFrom";
        public static final String PARENT_MESSAGE_ID = "parentMessageId";
        public static final String GROUPCHAT_USER_ID = "groupchatUserId";
        public static final String IS_GROUPCHAT_SYSTEM = "isGroupchatSystem";
        public static final String IS_REGULAR_RECEIVED = "isRegularReceived";
    }

    /**
     * Should be account jid + # + contact jid + originId or stanzaId
     */
    @PrimaryKey
    @Required
    private String primaryKey;

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
     * Message state represented by MessageStatus
     */
    private String messageStatus;

    /**
     * Message was shown to the us.
     */
    private boolean read;

    /**
     * Error response received on send request.
     */
    private String errorDescription;

    /**
     * Outgoing packet id - usual message stanza (packet) id
     */
    private String stanzaId;

    /**
     * Internal packet id
     */
    private String originId;

    /**
     * If message was forwarded (e.g. message carbons (XEP-0280))
     */
    private boolean forwarded;

    private RealmList<ReferenceRealmObject> referenceRealmObjects;

    /** Message forwarding */

    private String originalStanza;

    /** If message was forwarded contains jid of original message author */
    private String originalFrom;

    private String parentMessageId;
    private String groupchatUserId;
    private boolean isGroupchatSystem = false;
    private boolean isRegularReceived = true;

    private RealmList<ForwardIdRealmObject> forwardedIds;

    private MessageRealmObject(String primaryKey) { this.primaryKey = primaryKey; }

    public static MessageRealmObject createMessageRealmObjectWithStanzaId(AccountJid accountJid,
                                                                          ContactJid contactJid,
                                                                          String stanzaId){
        MessageRealmObject messageRealmObject =
                new MessageRealmObject(createPrimaryKey(accountJid, contactJid, stanzaId));

        messageRealmObject.account = accountJid.toString();
        messageRealmObject.user = contactJid.toString();
        messageRealmObject.stanzaId = stanzaId;

        return messageRealmObject;
    }

    public static MessageRealmObject createForwardedMessageRealmObjectWithStanzaId(
            AccountJid accountJid, ContactJid contactJid, String stanzaId
    ){
        MessageRealmObject messageRealmObject =
                new MessageRealmObject(createForwardedPrimaryKey(accountJid, contactJid, stanzaId));

        messageRealmObject.account = accountJid.toString();
        messageRealmObject.user = contactJid.toString();
        messageRealmObject.stanzaId = stanzaId;
        messageRealmObject.setForwarded(true);

        return messageRealmObject;
    }


    public static MessageRealmObject createMessageRealmObjectWithOriginId(AccountJid accountJid,
                                                                         ContactJid contactJid,
                                                                         String originId){
        MessageRealmObject messageRealmObject =
                new MessageRealmObject(createPrimaryKey(accountJid, contactJid, originId));

        messageRealmObject.account = accountJid.toString();
        messageRealmObject.user = contactJid.toString();
        messageRealmObject.originId = originId;

        return messageRealmObject;
    }

    public static MessageRealmObject createForwardedMessageRealmObjectWithOriginId(
            AccountJid accountJid, ContactJid contactJid, String originId
    ){
        MessageRealmObject messageRealmObject =
                new MessageRealmObject(createForwardedPrimaryKey(accountJid, contactJid, originId));

        messageRealmObject.account = accountJid.toString();
        messageRealmObject.user = contactJid.toString();
        messageRealmObject.originId = originId;
        messageRealmObject.setForwarded(true);

        return messageRealmObject;
    }


    public static String createPrimaryKey(AccountJid accountJid, ContactJid contactJid, String id){
        return accountJid.toString() + "#" + contactJid.toString() + "#" + id;
    }

    public static String createForwardedPrimaryKey(
            AccountJid accountJid, ContactJid contactJid, String id
    ) {
        return accountJid.toString() + "#" + contactJid.toString() + "#" + id + "forwarded";
    }

    public MessageRealmObject() { this.primaryKey = UUID.randomUUID().toString(); }

    public String getPrimaryKey() { return primaryKey; }

    public AccountJid getAccount() {
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public ContactJid getUser() {
        try {
            return ContactJid.from(user);
        } catch (ContactJid.ContactJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
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

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public String getAction() { return action; }

    public void setAction(String action) { this.action = action; }

    public boolean isIncoming() { return incoming; }

    public void setIncoming(boolean incoming) { this.incoming = incoming; }

    public Long getTimestamp() { return timestamp; }

    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Long getEditedTimestamp() { return editedTimestamp; }

    public void setEditedTimestamp(Long timestamp) { this.editedTimestamp = timestamp; }

    public Long getDelayTimestamp() { return delayTimestamp; }

    public void setDelayTimestamp(Long delayTimestamp) { this.delayTimestamp = delayTimestamp; }

    public String getStanzaId() { return stanzaId; }

    public void setStanzaId(String stanzaId) { this.stanzaId = stanzaId; }

    public String getOriginId() { return originId; }

    public void setOriginId(String originId) { this.originId = originId; }

    public boolean isForwarded() { return forwarded; }

    public void setForwarded(boolean forwarded) { this.forwarded = forwarded; }

    public ChatAction getChatAction() {
        try {
            return ChatAction.valueOf(getAction());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Spannable getSpannable() { return new SpannableString(getText()); }

    public MessageStatus getMessageStatus() {
        try {
            return MessageStatus.valueOf(messageStatus);
        } catch (Exception e) {
            return MessageStatus.NONE;
        }
    }

    public void setMessageStatus(MessageStatus messageStatus) { this.messageStatus = messageStatus.toString(); }

    public boolean isRead() { return read; }

    public void setRead(boolean read) { this.read = read; }

    public String getErrorDescription() { return errorDescription; }

    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

    public RealmList<ReferenceRealmObject> getReferencesRealmObjects() { return referenceRealmObjects; }

    public void setReferencesRealmObjects(RealmList<ReferenceRealmObject> referenceRealmObjects) {
        this.referenceRealmObjects = referenceRealmObjects;
    }

    public boolean hasReferences() { return referenceRealmObjects != null && referenceRealmObjects.size() > 0; }

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

    public boolean hasForwardedMessages() { return forwardedIds != null && !forwardedIds.isEmpty(); }

    public String getOriginalStanza() { return originalStanza; }

    public void setOriginalStanza(String originalStanza) { this.originalStanza = originalStanza; }

    public String getOriginalFrom() { return originalFrom; }

    public void setOriginalFrom(String originalFrom) { this.originalFrom = originalFrom; }

    public String getParentMessageId() { return parentMessageId; }

    public void setParentMessageId(String parentMessageId) { this.parentMessageId = parentMessageId; }

    public String getMarkupText() { return markupText; }

    public void setMarkupText(String markupText) { this.markupText = markupText; }

    public String getGroupchatUserId() { return groupchatUserId; }

    public void setGroupchatUserId(String groupchatUserId) { this.groupchatUserId = groupchatUserId; }

    public boolean isGroupchatSystem() { return isGroupchatSystem; }

    public void setGroupchatSystem(boolean groupchatSystem) {
        isGroupchatSystem = groupchatSystem;
        if (groupchatSystem) setRead(true);
    }

    public String getFirstForwardedMessageText() { return getFirstForwardedMessageText(-1); }

    public String getFirstForwardedMessageText(int color) {
        String text = null;
        if (hasForwardedMessages()) {
            String[] forwardedIDs = getForwardedIdsAsArray();
            if (!Arrays.asList(forwardedIDs).contains(null)) {
                Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                RealmResults<MessageRealmObject> forwardedMessages = realm
                        .where(MessageRealmObject.class)
                        .in(MessageRealmObject.Fields.PRIMARY_KEY, forwardedIDs)
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
                        } else {
                            stringBuilder.append(StringUtilsKt.wrapWithColorTag(author, color));
                        }
                    }
                    stringBuilder.append(message.getText().trim()).append(" ");
                    String attachmentName = "";
                    if (message.hasReferences() && message.getReferencesRealmObjects().size() > 0) {
                        ReferenceRealmObject referenceRealmObject = message.getReferencesRealmObjects().get(0);
                        attachmentName = StringUtilsKt.wrapWithColorTag(referenceRealmObject.getTitle().trim(), color);
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
        if(referenceRealmObjects !=null && referenceRealmObjects.size()>0) {
            for (ReferenceRealmObject a : referenceRealmObjects) {
                if (!a.isImage()) return false;
            }
            return true;
        }
        return false;
    }

    public boolean hasImage(){
         
        if(referenceRealmObjects !=null && referenceRealmObjects.size()>0) {
            for (ReferenceRealmObject a : referenceRealmObjects) {
                if (a.isImage()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRegularReceived() { return isRegularReceived; }

    public void setRegularReceived(boolean regularReceived) { isRegularReceived = regularReceived; }

}