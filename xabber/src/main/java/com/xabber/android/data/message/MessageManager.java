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
import android.os.Looper;
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
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.RefUser;
import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.reliablemessagedelivery.ReliableMessageDeliveryManager;
import com.xabber.android.data.groupchat.GroupchatUserManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.xmpp.sid.UniqStanzaHelper;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.Jid;

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
        if (instance == null) {
            instance = new MessageManager();
        }

        return instance;
    }

    @Override
    public void onLoad() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        RealmResults<MessageRealmObject> messagesToSend = realm.where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.SENT, false)
                .findAll();

        for (MessageRealmObject messageRealmObject : messagesToSend) {
            AccountJid account = messageRealmObject.getAccount();
            ContactJid user = messageRealmObject.getUser();

            if (account != null && user != null) {
                if (ChatManager.getInstance().getChat(account, user) == null) {
                    ChatManager.getInstance().getOrCreateChat(account, user);
                }
            }
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    /**
     * Sends message. Creates and registers new chat if necessary.
     *
     * @param account
     * @param user
     * @param text
     */
    public void sendMessage(AccountJid account, ContactJid user, String text) {

        EventBus.getDefault().post(new NewMessageEvent());

        AbstractChat chat = ChatManager.getInstance().getOrCreateChat(account, user);
        sendMessage(text, chat);

        // stop grace period
        AccountManager.getInstance().stopGracePeriod(account);
    }

    private void sendMessage(final String text, final AbstractChat chat) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransactionAsync(realm1 -> {
            MessageRealmObject newMessageRealmObject = chat.createNewMessageItem(text);
            realm1.copyToRealm(newMessageRealmObject);
            if (chat.canSendMessage())
                chat.sendMessages();
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        // mark incoming messages as read
        chat.markAsReadAll(true);
        EventBus.getDefault().post(new ChatManager.ChatUpdatedEvent());
    }

    public String createFileMessage(AccountJid account, ContactJid user, List<File> files) {
        return createFileMessageWithForwards(account, user, files, null);
    }

    public String createFileMessageWithForwards(AccountJid account, ContactJid user, List<File> files, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getOrCreateChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(files, null, null, forwardIds);
    }

    public String createVoiceMessageWithForwards(AccountJid account, ContactJid user, List<File> files, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getOrCreateChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(files, null, ReferenceElement.Type.voice.name(), forwardIds);
    }

    public String createFileMessageFromUrisWithForwards(AccountJid account, ContactJid user, List<Uri> uris, List<String> forwardIds) {
        AbstractChat chat = ChatManager.getInstance().getOrCreateChat(account, user);
        chat.openChat();
        return chat.newFileMessageWithFwr(null, uris, null, forwardIds);
    }

    public void updateFileMessage(AccountJid account, ContactJid user, final String messageId,
                                  final HashMap<String, String> urls, final List<String> notUploadedFilesUrls) {
        final AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        if (chat == null) {
            return;
        }

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        realm.executeTransaction(realm1 -> {
            MessageRealmObject messageRealmObject = realm1.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageId)
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
                messageRealmObject.setSent(false);
                messageRealmObject.setInProgress(false);
                messageRealmObject.setError(false);
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
                    .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageId)
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
                        HttpFileUploadManager.ImageSize imageSize =
                                HttpFileUploadManager.getImageSizes(file.getPath());
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
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransactionAsync(realm1 -> {
                updateMessageWithError(realm1, messageId, errorDescription);
            });
        } else {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 ->  {
                updateMessageWithError(realm1, messageId, errorDescription);
            });
            realm.close();
        }
    }

    private void updateMessageWithError(Realm realm, final String messageId, final String errorDescription) {
        MessageRealmObject messageRealmObject = realm.where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageId)
                .findFirst();

        if (messageRealmObject != null) {
            messageRealmObject.setError(true);
            messageRealmObject.setErrorDescription(errorDescription);
            messageRealmObject.setInProgress(false);
        }
    }

    public void removeErrorAndResendMessage(AccountJid account, ContactJid user, final String messageId) {
        AbstractChat abstractChat = ChatManager.getInstance().getChat(account, user);

        if (abstractChat == null) {
            return;
        }

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.executeTransaction(realm1 -> {
            MessageRealmObject messageRealmObject = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageId)
                    .findFirst();

            if (messageRealmObject != null) {
                messageRealmObject.setError(false);
                messageRealmObject.setSent(false);
                messageRealmObject.setErrorDescription("");
            }
        });

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        abstractChat.sendMessages();
    }

    /**
     * Removes all messages from chat.
     *
     * @param account
     * @param user
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
                            .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageItemId)
                            .findFirst();
                    if (messageRealmObject != null) messageRealmObject.deleteFromRealm();
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
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
                            .in(MessageRealmObject.Fields.UNIQUE_ID, ids).findAll();

                    if (items != null && !items.isEmpty())
                        items.deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }


    /**
     * Called on action settings change.
     */
    public void onSettingsChanged() {

    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza.getFrom() == null) {
            return;
        }
        AccountJid account = connection.getAccount();

        final ContactJid user;
        try {
            user = ContactJid.from(stanza.getFrom()).getBareUserJid();
        } catch (ContactJid.UserJidCreateException e) {
            return;
        }
        boolean processed = false;

        try {
            List<AbstractChat> chatsCopy = new ArrayList<>(ChatManager.getInstance().getChats());
            for (AbstractChat chat : chatsCopy) {
                if (chat.onPacket(user, stanza, false)) {
                    processed = true;
                    break;
                }
            }
        } catch (Exception e) { LogManager.exception(LOG_TAG, e); }

        if (!processed && stanza instanceof Message) {
            final Message message = (Message) stanza;
            final String body = message.getBody();
            if (body == null) {
                return;
            }

            //check for spam
            if (SettingsManager.spamFilterMode() != SettingsManager.SpamFilterMode.disabled
                    && RosterManager.getInstance().getRosterContact(account, user) == null ) {

                String thread = ((Message) stanza).getThread();

                if (SettingsManager.spamFilterMode() == SettingsManager.SpamFilterMode.authCaptcha) {
                    // check if this message is captcha-answer
                    Captcha captcha = CaptchaManager.getInstance().getCaptcha(account, user);
                    if (captcha != null) {
                        // attempt limit overhead
                        if (captcha.getAttemptCount() > CaptchaManager.CAPTCHA_MAX_ATTEMPT_COUNT) {
                            // remove this captcha
                            CaptchaManager.getInstance().removeCaptcha(account, user);
                            // discard subscription
                            try {
                                PresenceManager.getInstance().discardSubscription(account, user);
                            } catch (NetworkException e) {
                                e.printStackTrace();
                            }
                            sendMessageWithoutChat(user.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_many_attempts));
                            return;
                        }
                        if (body.equals(captcha.getAnswer())) {
                            // captcha solved successfully
                            // remove this captcha
                            CaptchaManager.getInstance().removeCaptcha(account, user);

                            // show auth
                            PresenceManager.getInstance().handleSubscriptionRequest(account, user);
                            sendMessageWithoutChat(user.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_correct));
                            return;
                        } else {
                            // captcha solved unsuccessfully
                            // increment attempt count
                            captcha.setAttemptCount(captcha.getAttemptCount() + 1);
                            // send warning-message
                            sendMessageWithoutChat(user.getJid(), thread, account,
                                    Application.getInstance().getResources().getString(R.string.spam_filter_captcha_incorrect));
                            return;
                        }
                    } else {
                        // no captcha exist and user not from roster
                        sendMessageWithoutChat(user.getJid(), thread, account,
                                Application.getInstance().getResources().getString(R.string.spam_filter_limit_message));
                        // and skip received message as spam
                        return;
                    }

                } else {
                    // if message from not-roster user
                    // send a warning message to sender
                    sendMessageWithoutChat(user.getJid(), thread, account,
                            Application.getInstance().getResources().getString(R.string.spam_filter_limit_message));
                    // and skip received message as spam
                    return;
                }
            }

            for (ExtensionElement packetExtension : message.getExtensions()) {
                if (packetExtension instanceof MUCUser) {
                    return;
                }
            }

            ChatManager.getInstance().getOrCreateChat(account, user).onPacket(user, stanza, false);
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
        CarbonManager.getInstance().setMessageToIgnoreCarbons(message);
        LogManager.d(LOG_TAG, "Message sent without chat. Invoke CarbonManager setMessageToIgnoreCarbons");
        try {
            StanzaSender.sendStanza(account, message);
        } catch (NetworkException e) {
            e.printStackTrace();
        }
    }

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        LogManager.d(LOG_TAG, "invoked processCarbonsMessage");
        if (direction == CarbonExtension.Direction.sent) {
            ContactJid companion;
            try {
                companion = ContactJid.from(message.getTo()).getBareUserJid();
            } catch (ContactJid.UserJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
                return;
            }

            final String body = message.getBody();
            if (body == null) {
                LogManager.d(LOG_TAG, "... but message body is null!");
                return;
            }

            final AbstractChat finalChat = ChatManager.getInstance().getOrCreateChat(account, companion);

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
            newMessageRealmObject.setStanzaId(AbstractChat.getStanzaId(message));
            newMessageRealmObject.setOriginId(UniqStanzaHelper.getOriginId(message));
            if (ReliableMessageDeliveryManager.getInstance().isSupported(account))
                newMessageRealmObject.setAcknowledged(true);
            newMessageRealmObject.setSent(true);
            newMessageRealmObject.setForwarded(true);
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
            RefUser groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message);
            if (groupchatUser != null) {
                GroupchatUserManager.getInstance().saveGroupchatUser(groupchatUser);
                newMessageRealmObject.setGroupchatUserId(groupchatUser.getId());
            }

            BackpressureMessageSaver.getInstance().saveMessageItem(newMessageRealmObject);

            // mark incoming messages as read
            finalChat.markAsReadAll(false);

            // start grace period
            AccountManager.getInstance().startGracePeriod(account);
            return;
        }

        ContactJid companion = null;
        try {
            companion = ContactJid.from(message.getFrom()).getBareUserJid();
        } catch (ContactJid.UserJidCreateException e) {
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
        if (ChatManager.getInstance().getOrCreateChat(account, companion) != null) {
            return;
        }
        if (processed) {
            return;
        }
        final String body = message.getBody();
        if (body == null) {
            return;
        }
        ChatManager.getInstance().getOrCreateChat(account, companion).onPacket(companion, message, true);

    }

    public static void setAttachmentLocalPathToNull(final String uniqId) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        realm.executeTransactionAsync(realm1 -> {
            AttachmentRealmObject first = realm1.where(AttachmentRealmObject.class)
                    .equalTo(AttachmentRealmObject.Fields.UNIQUE_ID, uniqId)
                    .findFirst();
            if (first != null) {
                first.setFilePath(null);
            }
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }
}