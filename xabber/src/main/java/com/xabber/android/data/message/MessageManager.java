/*
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
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.captcha.Captcha;
import com.xabber.android.data.extension.captcha.CaptchaManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.groups.GroupInviteManager;
import com.xabber.android.data.extension.groups.GroupMemberManager;
import com.xabber.android.data.extension.groups.GroupsManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnChatUpdatedListener;
import com.xabber.android.ui.OnNewMessageListener;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.xmpp.groups.invite.incoming.IncomingInviteExtensionElement;
import com.xabber.xmpp.sid.UniqueIdsHelper;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Domainpart;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Manage chats and its messages.
 * <p/>
 * Warning: message processing using chat instances should be changed.
 *
 * @author alexander.ivanov
 */
public class MessageManager implements OnLoadListener, OnPacketListener {

    private static MessageManager instance;
    private static final String LOG_TAG = MessageManager.class.getSimpleName();

    public static MessageManager getInstance() {
        if (instance == null) instance = new MessageManager();

        return instance;
    }

    @Override
    public void onLoad() {
        //todo if all will be fine, remove this code
//        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
//
//        RealmResults<MessageRealmObject> messagesToSend = realm.where(MessageRealmObject.class)
//                .equalTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.NOT_SENT.toString())
//                .findAll();
//
//        for (MessageRealmObject messageRealmObject : messagesToSend) {
//            AccountJid account = messageRealmObject.getAccount();
//            ContactJid user = messageRealmObject.getUser();
//
//            if (account != null && user != null) {
//                if (ChatManager.getInstance().getChat(account, user) == null) {
//                    ChatManager.getInstance().getChat(account, user);
//                }
//            }
//        }
//
//        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    /**
     * Sends message. Creates and registers new chat if necessary.
     */
    public void sendMessage(AccountJid account, ContactJid user, String text) {
        sendMessage(account, user, text, null);
    }

    public void sendMessage(AccountJid account, ContactJid user, String text, String markupText) {

        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        sendMessage(text, markupText, chat);

        // stop grace period
        AccountManager.getInstance().stopGracePeriod(account);

        for (OnNewMessageListener listener : Application.getInstance().getUIListeners(OnNewMessageListener.class)){
            listener.onNewMessage();
        }
    }

    private void sendMessage(final String text, final String markupText, final AbstractChat chat) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        realm.executeTransactionAsync(realm1 -> {
            MessageRealmObject newMessageRealmObject = chat.createNewMessageItem(text);

            if (markupText != null) newMessageRealmObject.setMarkupText(markupText);

            realm1.copyToRealm(newMessageRealmObject);

            if (chat.canSendMessage()) chat.sendMessages();
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        // mark incoming messages as read
        chat.markAsReadAll(true);
        for (OnChatUpdatedListener listener : Application.getInstance().getUIListeners(OnChatUpdatedListener.class)){
            listener.onChatUpdated();
        }
    }

    public String createFileMessage(AccountJid account, ContactJid user, List<File> files) {
        return createFileMessageWithForwards(account, user, files, null);
    }

    public String createFileMessageWithForwards(AccountJid account, ContactJid user, List<File> files, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(files, null, null, forwardIds);
    }

    public String createVoiceMessageWithForwards(AccountJid account, ContactJid user, List<File> files, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(files, null, "voice", forwardIds);
    }

    public String createFileMessageFromUrisWithForwards(AccountJid account, ContactJid user, List<Uri> uris, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(null, uris, null, forwardIds);
    }

    public void updateFileMessage(AccountJid account, ContactJid user, final String messageId,
                                  final HashMap<String, String> urls, final List<String> notUploadedFilesUrls) {
        final AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        if (chat == null) return;


        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        realm.executeTransaction(realm1 -> {
            MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
                    .findFirst();

            if (messageRealmObject != null) {
                RealmList<AttachmentRealmObject> attachmentRealmObjects = messageRealmObject.getAttachmentRealmObjects();

                // remove attachments that not uploaded
                for (String file : notUploadedFilesUrls) {
                    for (AttachmentRealmObject attachmentRealmObject : attachmentRealmObjects) {
                        if (file.equals(attachmentRealmObject.getFilePath())) {
                            attachmentRealmObjects.remove(attachmentRealmObject);
                            break;
                        }
                    }
                }

                for (AttachmentRealmObject attachmentRealmObject : attachmentRealmObjects) {
                    attachmentRealmObject.setFileUrl(urls.get(attachmentRealmObject.getFilePath()));
                }

                messageRealmObject.setText("");
                messageRealmObject.setMessageStatus(MessageStatus.NOT_SENT); //todo check this
                messageRealmObject.setErrorDescription("");
            }
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        chat.sendMessages();
    }

    public void updateMessageWithNewAttachments(final String messageId, final List<File> files) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransaction(realm1 ->  {
            MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
                    .findFirst();

            if (messageRealmObject != null) {
                RealmList<AttachmentRealmObject> attachmentRealmObjects = messageRealmObject.getAttachmentRealmObjects();

                // remove temporary attachments created from uri
                // to replace it with attachments created from files
                attachmentRealmObjects.deleteAllFromRealm();

                for (File file : files) {
                    AttachmentRealmObject attachmentRealmObject = new AttachmentRealmObject();
                    attachmentRealmObject.setFilePath(file.getPath());
                    attachmentRealmObject.setFileSize(file.length());
                    attachmentRealmObject.setTitle(file.getName());
                    attachmentRealmObject.setIsImage(FileManager.fileIsImage(file));
                    attachmentRealmObject.setMimeType(HttpFileUploadManager.getMimeType(file.getPath()));
                    attachmentRealmObject.setDuration((long) 0);

                    if (attachmentRealmObject.isImage()) {
                        HttpFileUploadManager.ImageSize imageSize = HttpFileUploadManager.getImageSizes(file.getPath());
                        attachmentRealmObject.setImageHeight(imageSize.getHeight());
                        attachmentRealmObject.setImageWidth(imageSize.getWidth());
                    }
                    attachmentRealmObjects.add(attachmentRealmObject);
                }
            }
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

    }

    public void updateMessageWithError(final String messageId, final String errorDescription) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransactionAsync(realm1 -> {
            MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
                    .findFirst();

            if (messageRealmObject != null) {
                messageRealmObject.setMessageStatus(MessageStatus.ERROR);
                messageRealmObject.setErrorDescription(errorDescription);
            }
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    public void removeErrorAndResendMessage(AccountJid account, ContactJid user, final String messageId) {
        AbstractChat abstractChat = ChatManager.getInstance().getChat(account, user);

        if (abstractChat == null) return;

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransaction(realm1 -> {
            MessageRealmObject messageRealmObject = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
                    .findFirst();

            if (messageRealmObject != null) {
                messageRealmObject.setMessageStatus(MessageStatus.NOT_SENT);
                messageRealmObject.setErrorDescription("");
            }
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        abstractChat.sendMessages();
    }

    /**
     * Removes all messages from chat.
     */
    public void clearHistory(final AccountJid account, final ContactJid user) {
        final long startTime = System.currentTimeMillis();
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        realm.executeTransactionAsync(realm1 -> {
            realm1.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, user.toString())
                    .findAll().deleteAllFromRealm();
            LogManager.d("REALM", Thread.currentThread().getName()
                    + " clear history: " + (System.currentTimeMillis() - startTime));
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    /**
     * Removes message from history.
     *
     */
    public void removeMessage(final String messageItemId) {
        Application.getInstance().runInBackgroundUserRequest(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    MessageRealmObject messageRealmObject = realm1
                            .where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageItemId)
                            .findFirst();
                    if (messageRealmObject != null) messageRealmObject.deleteFromRealm();
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    /**
     * Removes message from history.
     *
     */
    public void removeMessage(final List<String> messageIDs) {
        final String[] ids = messageIDs.toArray(new String[0]);
        Application.getInstance().runInBackgroundUserRequest(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<MessageRealmObject> items = realm1.where(MessageRealmObject.class)
                            .in(MessageRealmObject.Fields.PRIMARY_KEY, ids).findAll();

                    if (items != null && !items.isEmpty()) items.deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    /**
     * Called on action settings change.
     */
    public void onSettingsChanged() {}

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza.getFrom() == null) return;

        AccountJid account = connection.getAccount();

        final ContactJid contactJid;
        try {
            contactJid = ContactJid.from(stanza.getFrom()).getBareUserJid();
        } catch (ContactJid.ContactJidCreateException e) {
            return;
        }
        boolean processed = false;

        if (stanza.hasExtension(IncomingInviteExtensionElement.ELEMENT, IncomingInviteExtensionElement.NAMESPACE)) {
            IncomingInviteExtensionElement inviteElement = stanza.getExtension(IncomingInviteExtensionElement.ELEMENT,
                    IncomingInviteExtensionElement.NAMESPACE);
            long timestamp = 0;
            if (stanza.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                TimeElement timeElement = stanza.getExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE);
                timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.getStamp()).getTime();
            }
            GroupInviteManager.INSTANCE.processIncomingInvite(inviteElement, account, contactJid, timestamp);
            return;
        }

        if (stanza.hasExtension(GroupExtensionElement.NAMESPACE)
                && ChatManager.getInstance().hasChat(account.toString(), contactJid.toString())){
            AbstractChat abstractChat = ChatManager.getInstance().getChat(account, contactJid);
            if (abstractChat instanceof RegularChat){
                ChatManager.getInstance().removeChat(abstractChat);
                ChatManager.getInstance().createGroupChat(account, contactJid);
            }
        }

        try {
            List<AbstractChat> chatsCopy = new ArrayList<>(ChatManager.getInstance().getChats());
            for (AbstractChat chat : chatsCopy) {
                if (chat.onPacket(contactJid, stanza, false)) {
                    processed = true;
                    break;
                }
            }
        } catch (Exception e) { LogManager.exception(LOG_TAG, e); }

        if (!processed && stanza instanceof Message) {
            final Message message = (Message) stanza;
            final String body = message.getBody();
            if (body == null) return;

            //check for spam
            if (SettingsManager.spamFilterMode() != SettingsManager.SpamFilterMode.disabled
                    && RosterManager.getInstance().getRosterContact(account, contactJid) == null ) {

                String thread = ((Message) stanza).getThread();

                if (SettingsManager.spamFilterMode() == SettingsManager.SpamFilterMode.authCaptcha) {
                    // check if this message is captcha-answer
                    Captcha captcha = CaptchaManager.getInstance().getCaptcha(account, contactJid);
                    if (captcha != null) {
                        // attempt limit overhead
                        if (captcha.getAttemptCount() > CaptchaManager.CAPTCHA_MAX_ATTEMPT_COUNT) {
                            // remove this captcha
                            CaptchaManager.getInstance().removeCaptcha(account, contactJid);
                            // discard subscription
                            try {
                                PresenceManager.getInstance().discardSubscription(account, contactJid);
                            } catch (NetworkException e) {
                                e.printStackTrace();
                            }
                            sendMessageWithoutChat(contactJid.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_many_attempts));
                            return;
                        }
                        if (body.equals(captcha.getAnswer())) {
                            // captcha solved successfully
                            // remove this captcha
                            CaptchaManager.getInstance().removeCaptcha(account, contactJid);

                            // show auth
                            PresenceManager.getInstance().handleSubscriptionRequest(account, contactJid);
                            sendMessageWithoutChat(contactJid.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_correct));
                        } else {
                            // captcha solved unsuccessfully
                            // increment attempt count
                            captcha.setAttemptCount(captcha.getAttemptCount() + 1);
                            // send warning-message
                            sendMessageWithoutChat(contactJid.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_incorrect));
                        }
                    } else {
                        // no captcha exist and user not from roster
                        sendMessageWithoutChat(contactJid.getJid(), thread, account,
                                Application.getInstance().getResources().getString(R.string.spam_filter_limit_message));
                        // and skip received message as spam
                    }

                } else {
                    // if message from not-roster user
                    // send a warning message to sender
                    sendMessageWithoutChat(contactJid.getJid(), thread, account,
                            Application.getInstance().getResources().getString(R.string.spam_filter_limit_message));
                    // and skip received message as spam
                }
                return;
            }
            if (stanza.hasExtension(GroupExtensionElement.NAMESPACE)){
                ChatManager.getInstance().createGroupChat(account, contactJid).onPacket(contactJid, stanza, false);
            } else ChatManager.getInstance().createRegularChat(account, contactJid).onPacket(contactJid, stanza, false);
        }
    }

    // send messages without creating chat and adding to roster
    // used for service auto-generated messages
    public void sendMessageWithoutChat(Jid to, String threadId, AccountJid account, String text) {
        Message message = new Message();
        message.setTo(to);
        message.setType(Message.Type.chat);
        message.setBody(text);
        message.setThread(threadId);
        // send auto-generated messages without carbons
        CarbonManager.INSTANCE.setMessageToIgnoreCarbons(message);
        LogManager.d(LOG_TAG, "Message sent without chat. Invoke CarbonManager setMessageToIgnoreCarbons");
        try {
            StanzaSender.sendStanza(account, message);
        } catch (NetworkException e) {
            e.printStackTrace();
        }
    }

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        if (direction == CarbonExtension.Direction.sent) {
            ContactJid companion;
            try {
                companion = ContactJid.from(message.getTo()).getBareUserJid();
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
                return;
            }

            final String body = message.getBody();

            if (body == null) return;

            final AbstractChat finalChat = ChatManager.getInstance().getChat(account, companion);

            String text = body;
            String uid = UUID.randomUUID().toString();
            RealmList<ForwardIdRealmObject> forwardIdRealmObjects = finalChat.parseForwardedMessage(true, message, uid);
            String originalStanza = message.toXML().toString();
            String originalFrom = message.getFrom().toString();

            // forward comment (to support previous forwarded xep)
            String forwardComment = ForwardManager.parseForwardComment(message);
            if (forwardComment != null) text = forwardComment;

            // modify body with references
            Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
            text = bodies.first;
            String markupText = bodies.second;

            MessageRealmObject newMessageRealmObject = finalChat.createNewMessageItem(text);
            if (message.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)){
                String timestamp =
                        ((TimeElement) message.getExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)).getStamp();

                newMessageRealmObject.setTimestamp(StringUtils.parseReceivedReceiptTimestampString(timestamp).getTime());
            }
            newMessageRealmObject.setOriginId(UniqueIdsHelper.getOriginId(message));

            newMessageRealmObject.setMessageStatus(MessageStatus.DELIVERED);
//
//            newMessageRealmObject.setSent(true);
//            newMessageRealmObject.setForwarded(true);

            if (markupText != null) newMessageRealmObject.setMarkupText(markupText);

            // forwarding
            if (forwardIdRealmObjects != null) newMessageRealmObject.setForwardedIds(forwardIdRealmObjects);

            newMessageRealmObject.setOriginalStanza(originalStanza);
            newMessageRealmObject.setOriginalFrom(originalFrom);

            // attachments
            RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message);
            if (attachmentRealmObjects.size() > 0)
                newMessageRealmObject.setAttachmentRealmObjects(attachmentRealmObjects);

            // groupchat
            GroupMemberExtensionElement groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message);
            if (groupchatUser != null) {
                GroupMemberManager.getInstance().saveGroupUser(groupchatUser, message.getTo().asBareJid());
                newMessageRealmObject.setGroupchatUserId(groupchatUser.getId());
                newMessageRealmObject.setStanzaId(
                        UniqueIdsHelper.getStanzaIdBy(message, companion.getBareJid().toString()));
            } else if (message.hasExtension(GroupExtensionElement.ELEMENT, GroupsManager.SYSTEM_MESSAGE_NAMESPACE)){
                newMessageRealmObject.setGroupchatSystem(true);
                newMessageRealmObject.setStanzaId(
                        UniqueIdsHelper.getStanzaIdBy(message, companion.getBareJid().toString()));
            } else {
                newMessageRealmObject.setStanzaId(
                        UniqueIdsHelper.getStanzaIdBy(message, account.getBareJid().toString()));
            }

            MessageHandler.INSTANCE.saveOrUpdateMessage(newMessageRealmObject);

            // mark incoming messages as read
            finalChat.markAsReadAll(false);

            // start grace period
            AccountManager.getInstance().startGracePeriod(account);
            return;
        }

        ContactJid companion;
        try {
            companion = ContactJid.from(message.getFrom()).getBareUserJid();
        } catch (ContactJid.ContactJidCreateException e) {
            return;
        }

        //check for spam
        if (SettingsManager.spamFilterMode() != SettingsManager.SpamFilterMode.disabled
                && RosterManager.getInstance().getRosterContact(account, companion) == null ) {
            // just ignore carbons from not-authorized user
            return;
        }

        boolean processed = false;
        for (AbstractChat chat : ChatManager.getInstance().getChats(account)) {
            if (chat.onPacket(companion, message, true)) {
                processed = true;
                break;
            }
        }

        if (ChatManager.getInstance().getChat(account, companion) != null) return;

        if (processed) return;

        final String body = message.getBody();

        if (body == null) return;

        ChatManager.getInstance().getChat(account, companion).onPacket(companion, message, true);

    }

    /**
     * @return Whether message was delayed by server.
     */
    public static boolean isOfflineMessage(Domainpart server, Stanza stanza) {
        DelayInformation delayInformation = DelayInformation.from(stanza);
        return delayInformation != null && TextUtils.equals(delayInformation.getFrom(), server);
    }

    public static void setAttachmentLocalPathToNull(final String uniqId) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransactionAsync(realm1 -> {
            AttachmentRealmObject first = realm1.where(AttachmentRealmObject.class)
                    .equalTo(AttachmentRealmObject.Fields.UNIQUE_ID, uniqId)
                    .findFirst();
            if (first != null) first.setFilePath(null);
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

}