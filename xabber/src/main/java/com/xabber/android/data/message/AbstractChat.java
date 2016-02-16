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

import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.mam.SyncCache;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.carbon.CarbonManager;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Chat instance.
 *
 * @author alexander.ivanov
 */
public abstract class AbstractChat extends BaseEntity {

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
    protected boolean trackStatus;
    /**
     * Whether user never received notifications from this chat.
     */
    protected boolean firstNotification;

    protected Date creationTime = new Date();
    /**
     * Current thread id.
     */
    private String threadId;

    private boolean isPrivateMucChat;
    private boolean isPrivateMucChatAccepted;

    private boolean isLocalHistoryLoadedCompletely = false;

    private SyncCache syncCache;


    protected AbstractChat(final String account, final String user, boolean isPrivateMucChat) {
        super(account, isPrivateMucChat ? user : Jid.getBareAddress(user));
        LogManager.i("AbstractChat", "AbstractChat user: " + user);
        threadId = StringUtils.randomString(12);
        active = false;
        trackStatus = false;
        firstNotification = true;
        this.isPrivateMucChat = isPrivateMucChat;
        isPrivateMucChatAccepted = false;
        updateCreationTime();

        syncCache = new SyncCache();

    }

    public boolean isLocalHistoryLoadedCompletely() {
        return isLocalHistoryLoadedCompletely;
    }

    public boolean isActive() {
        if (isPrivateMucChat && !isPrivateMucChatAccepted) {
            return false;
        }

        return active;
    }

    void openChat() {
        if (!active) {
            updateCreationTime();
        }

        active = true;
        trackStatus = true;
    }

    void closeChat() {
        active = false;
        firstNotification = true;
    }

    boolean isStatusTrackingEnabled() {
        return trackStatus;
    }

    /**
     * @return Target address for sending message.
     */
    public abstract String getTo();

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
    protected boolean notifyAboutMessage() {
        return SettingsManager.eventsMessage() != SettingsManager.EventsMessage.none;
    }

    /**
     * @param text
     * @return New message instance.
     */
    abstract protected void newMessage(String text);

    /**
     * Creates new action.
     *
     * @param resource can be <code>null</code>.
     * @param text     can be <code>null</code>.
     * @param action
     */
    public void newAction(String resource, String text, ChatAction action) {
        newMessage(resource, text, action, null, true, false, false, false, true, null);
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
     * @param unencrypted    Whether not encrypted message in OTR chat was received.
     * @param offline        Whether message was received from server side offline storage.
     * @param record         Whether record server side is enabled.
     * @return
     */
    protected void newMessage(String resource, String text,
                                     final ChatAction action, final Date delayTimestamp, final boolean incoming,
                                     boolean notify, final boolean unencrypted, final boolean offline, boolean record, final String stanzaId) {
        boolean save;
        final boolean visible = MessageManager.getInstance().isVisibleChat(this);
        boolean read = incoming ? visible : true;
        boolean send = incoming;
        if (action == null && text == null) {
            throw new IllegalArgumentException();
        }
        if (resource == null) {
            resource = "";
        }
        if (text == null) {
            text = "";
        }
        if (action != null) {
            read = true;
            send = true;
            save = false;
        } else {
            ArchiveMode archiveMode = AccountManager.getInstance().getArchiveMode(account);
            if (archiveMode == ArchiveMode.dontStore) {
                save = false;
            } else {
                save = archiveMode.saveLocally() || !send
                        || (!read && archiveMode == ArchiveMode.unreadOnly);
            }
            if (save) {
                save = ChatManager.getInstance().isSaveMessages(account, user);
            }
        }
        if (save && (unencrypted || (!SettingsManager.securityOtrHistory()
                && OTRManager.getInstance().getSecurityLevel(account, user) != SecurityLevel.plain))) {
            save = false;
        }
        final Date timestamp = new Date();

        if (text.trim().isEmpty()) {
            notify = false;
        }

        if (notify || !incoming) {
            openChat();
        }
        if (!incoming) {
            notify = false;
        }

        if (isPrivateMucChat) {
            if (!isPrivateMucChatAccepted
                    || PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account).contains(user)) {
                notify = false;
            }
        }

        if (save && !isPrivateMucChat) {
            final boolean finalRead = read;
            final boolean finalSend = send;
            final String finalResource = resource;
            final String finalText = text;
            final boolean finalNotify = notify;
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    MessageItem messageItem = new MessageItem();

                    messageItem.setAccount(account);
                    messageItem.setUser(user);
                    messageItem.setResource(finalResource);
                    if (action != null) {
                        messageItem.setAction(action.toString());
                    }
                    messageItem.setText(finalText);
                    messageItem.setTimestamp(timestamp.getTime());
                    if (delayTimestamp != null) {
                        messageItem.setDelayTimestamp(delayTimestamp.getTime());
                    }
                    messageItem.setIncoming(incoming);
                    messageItem.setRead(finalRead);
                    messageItem.setSent(finalSend);
                    messageItem.setUnencrypted(unencrypted);
                    messageItem.setOffline(offline);
                    messageItem.setStanzaId(stanzaId);
                    FileManager.processFileMessage(messageItem, true);

                    messageItem = realm.copyToRealm(messageItem);

                    if (finalNotify && notifyAboutMessage()) {
                        if (visible) {
                            if (ChatManager.getInstance().isNotifyVisible(account, user)) {
                                NotificationManager.getInstance().onCurrentChatMessageNotification(messageItem);
                            }
                        } else {
                            NotificationManager.getInstance().onMessageNotification(messageItem);
                        }
                    }


                }
            }, null);
            realm.close();

        }
    }

    protected String newFileMessage(final File file) {
        Realm realm = Realm.getDefaultInstance();

        final String messageId = UUID.randomUUID().toString();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageItem messageItem = new MessageItem(messageId);
                messageItem.setAccount(account);
                messageItem.setUser(user);
                messageItem.setText(file.getName());
                messageItem.setFilePath(file.getPath());
                messageItem.setTimestamp(System.currentTimeMillis());
                messageItem.setRead(true);
                messageItem.setSent(true);
                messageItem.setError(false);
                messageItem.setIncoming(false);
                realm.copyToRealm(messageItem);
            }
        }, null);


        realm.close();

        return messageId;
    }

    private void updateSyncInfo() {
//        LogManager.i(this, "updateSyncInfo messages size");
//
//        final String firstMamMessageStanzaId = syncInfo.getFirstMamMessageStanzaId();
//
//        Integer firstLocalMessagePosition = null;
//        Integer firstMamMessagePosition = null;
//        Date firstLocalMessageTimestamp = null;
//
//        for (int i = 0; i < messages.size(); i++) {
//            MessageItem messageItem = messages.get(i);
//            String stanzaId = messageItem.getStanzaId();
//
//            if (firstLocalMessagePosition == null && messageItem.getId() != null) {
//                firstLocalMessagePosition = i;
//                firstLocalMessageTimestamp = messageItem.getTimestamp();
//                LogManager.i(this, "firstLocalMessagePosition " + firstLocalMessagePosition + " firstLocalMessageTimestamp " + firstLocalMessageTimestamp);
//            }
//
//
//            if (firstMamMessagePosition == null && firstMamMessageStanzaId != null && stanzaId != null
//                    && firstMamMessageStanzaId.equals(stanzaId)) {
//                firstMamMessagePosition = i;
//
//                LogManager.i(this, "firstMamMessagePosition " + i);
//            }
//
//
//            if (firstLocalMessagePosition != null) {
//                if (firstMamMessagePosition != null) {
//                    break;
//                }
//                if (firstMamMessageStanzaId == null) {
//                    break;
//                }
//            }
//
//        }
//
//        syncCache.setFirstLocalMessagePosition(firstLocalMessagePosition);
//        syncCache.setFirstLocalMessageTimeStamp(firstLocalMessageTimestamp);
//        syncCache.setFirstMamMessagePosition(firstMamMessagePosition);

    }

    void removeMessage(final MessageItem messageItem) {
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                messageItem.removeFromRealm();
//            }
//        }, null);
    }

    void removeAllMessages() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
               realm.where(MessageItem.class)
                       .equalTo(MessageItem.Fields.ACCOUNT, account)
                       .equalTo(MessageItem.Fields.USER, user)
                       .findAll().clear();
            }
        }, null);
        realm.close();
    }

    /**
     * @param bareAddress bareAddress of the user.
     * @param user        full jid.
     * @return Whether chat accepts packets from specified user.
     */
    boolean accept(String bareAddress, String user) {
        return this.user.equals(bareAddress);
    }

    public MessageItem getLastMessage() {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<MessageItem> allSorted = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account)
                .equalTo(MessageItem.Fields.USER, user)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        realm.close();

        if (allSorted.isEmpty()) {
            return null;
        } else {
            return allSorted.last();
        }
    }

    /**
     * @return Time of last message in chat. Can be <code>null</code>.
     */
    public Date getLastTime() {
        Realm realm = Realm.getDefaultInstance();
        Number max = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account)
                .equalTo(MessageItem.Fields.USER, user)
                .max(MessageItem.Fields.TIMESTAMP);
        realm.close();

        if (max != null) {
            return new Date(max.longValue());
        } else {
            return null;
        }
    }

    /**
     * @param body
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

    /**
     * Prepare text to be send.
     *
     * @param text
     * @return <code>null</code> if text shouldn't be send.
     */
    protected String prepareText(String text) {
        return text;
    }


    public void sendMessages() {
        LogManager.i(this, "sendMessages. user: " + user);

        final Realm realm = Realm.getDefaultInstance();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<MessageItem> messagesToSend = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.ACCOUNT, account)
                        .equalTo(MessageItem.Fields.USER, user)
                        .equalTo(MessageItem.Fields.SENT, false)
                        .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
                if (messagesToSend.isEmpty()) {
                    return;
                }

                List<MessageItem> messageItemList = new ArrayList<>(messagesToSend);

                LogManager.i(AbstractChat.this, "sendMessages " + messageItemList.size());

                for (final MessageItem messageItem : messageItemList) {
                    String text = prepareText(messageItem.getText());
                    Long timestamp = messageItem.getTimestamp();

                    Date currentTime = new Date(System.currentTimeMillis());
                    Date delayTimestamp = null;

                    if (timestamp != null) {
                        if (currentTime.getTime() - timestamp > 60000) {
                            delayTimestamp = currentTime;
                        }
                    }

                    Message message = null;
                    if (text != null) {

                        LogManager.i(AbstractChat.this, "Sending message user: " + messageItem.getUser() + " text: " + messageItem.getText());
                        message = createMessagePacket(text);
                    }

                    if (message != null) {
                        ChatStateManager.getInstance().updateOutgoingMessage(AbstractChat.this, message);
                        CarbonManager.getInstance().updateOutgoingMessage(AbstractChat.this, message);
                        if (delayTimestamp != null) {
                            message.addExtension(new DelayInformation(delayTimestamp));
                        }

                        try {
                            ConnectionManager.getInstance().sendStanza(account, message);
                        } catch (NetworkException e) {
                            break;
                        }
                    }

                    if (message == null) {
                        messageItem.setError(true);
                    } else {
                        messageItem.setStanzaId(message.getStanzaId());
                    }

                    if (delayTimestamp != null) {
                        messageItem.setDelayTimestamp(delayTimestamp.getTime());
                    }
                    if (messageItem.getTimestamp() == null) {
                        messageItem.setTimestamp(currentTime.getTime());
                    }
                    messageItem.setSent(true);
                }
            }
        }, null);
        realm.close();
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
     * @param bareAddress
     * @param packet
     * @return Whether packet was directed to this chat.
     */
    protected boolean onPacket(String bareAddress, Stanza packet) {
        return accept(bareAddress, packet.getFrom());
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
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void updateCreationTime() {
        creationTime.setTime(System.currentTimeMillis());
    }

    public void setIsPrivateMucChatAccepted(boolean isPrivateMucChatAccepted) {
        this.isPrivateMucChatAccepted = isPrivateMucChatAccepted;
    }

    public boolean isPrivateMucChat() {
        return isPrivateMucChat;
    }

    public boolean isPrivateMucChatAccepted() {
        return isPrivateMucChatAccepted;
    }

    public SyncCache getSyncCache() {
        return syncCache;
    }
}