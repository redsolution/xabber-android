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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.captcha.Captcha;
import com.xabber.android.data.extension.captcha.CaptchaManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.delivery.TimeElement;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.groups.GroupInviteManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnChatUpdatedListener;
import com.xabber.android.ui.OnNewMessageListener;
import com.xabber.xmpp.groups.invite.incoming.IncomingInviteExtensionElement;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Domainpart;
import org.jxmpp.util.XmppDateTime;

import java.io.File;
import java.util.Date;
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
public class MessageManager implements OnPacketListener {

    private static MessageManager instance;
    private static final String LOG_TAG = MessageManager.class.getSimpleName();

    public static MessageManager getInstance() {
        if (instance == null) instance = new MessageManager();

        return instance;
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
            listener.onAction();
        }
    }

    private void sendMessage(final String text, final String markupText, final AbstractChat chat) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        // TODO: 23.04.21 filemessages and forwards
        realm.executeTransactionAsync(realm1 -> {

            MessageRealmObject message = MessageRealmObject.createMessageRealmObjectWithOriginId(
                    chat.getAccount(), chat.getContactJid(), UUID.randomUUID().toString());
            message.setText(text.replaceAll("\0", ""));
            message.setIncoming(false);
            message.setOffline(false);
            message.setEncrypted(false);
            message.setForwarded(false);
            message.setGroupchatSystem(false);
            message.setTimestamp(new Date().getTime());
            message.setMessageStatus(MessageStatus.NOT_SENT);
            if (markupText != null) message.setMarkupText(markupText);

            realm1.copyToRealm(message);

            if (chat.canSendMessage()) chat.sendMessages();
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        // mark incoming messages as read
        chat.markAsReadAll(true);
        for (OnChatUpdatedListener listener : Application.getInstance().getUIListeners(OnChatUpdatedListener.class)){
            listener.onAction();
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
                messageRealmObject.setMessageStatus(MessageStatus.NOT_SENT);
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

        if (stanza.hasExtension(IncomingInviteExtensionElement.ELEMENT, IncomingInviteExtensionElement.NAMESPACE)
                && !account.getBareJid().toString().equals(contactJid.getBareJid().toString())) {
            IncomingInviteExtensionElement inviteElement = stanza.getExtension(IncomingInviteExtensionElement.ELEMENT,
                    IncomingInviteExtensionElement.NAMESPACE);
            long timestamp = 0;
            if (stanza.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                TimeElement timeElement = stanza.getExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE);
                try{
                    timestamp = XmppDateTime.parseDate(timeElement.getTimeStamp()).getTime();
                } catch (Exception ignored) { }
            }
            GroupInviteManager.INSTANCE.processIncomingInvite(inviteElement, account, contactJid, timestamp);
            return;
        }

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
                                PresenceManager.INSTANCE.discardSubscription(account, contactJid);
                            } catch (NetworkException e) {
                                LogManager.exception(getClass().getSimpleName(), e);
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
                            PresenceManager.INSTANCE.handleSubscriptionRequest(account, contactJid);
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
            if (account.getBareJid().toString().contains(contactJid.getBareJid().toString())) return;
            MessageHandler.INSTANCE.parseMessage(account, contactJid, (Message) stanza, null);
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
            LogManager.exception(getClass().getSimpleName(), e);
        }
    }

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        ContactJid companion = null;
        if (direction == CarbonExtension.Direction.sent) {
            try {
                companion = ContactJid.from(message.getTo()).getBareUserJid();
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
                return;
            }

            //check for spam
            if (SettingsManager.spamFilterMode() != SettingsManager.SpamFilterMode.disabled
                    && RosterManager.getInstance().getRosterContact(account, companion) == null ) {
                // just ignore carbons from not-authorized user
                return;
            }
            AccountManager.getInstance().startGracePeriod(account);

        } else if (direction == CarbonExtension.Direction.received) {

            try {
                companion = ContactJid.from(message.getFrom()).getBareUserJid();
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
                return;
            }
        }
        if (companion != null) MessageHandler.INSTANCE.parseMessage(account, companion, message, null);
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