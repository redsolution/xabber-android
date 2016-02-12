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

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
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
import com.xabber.android.data.extension.mam.SyncInfo;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import io.realm.RealmChangeListener;
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

    /**
     * for Message Archive Management (XEP-313)
     */
    private SyncInfo syncInfo;

    private SyncCache syncCache;
    private final Realm realm;
    private final RealmResults<MessageItem> messages;
    private final RealmResults<MessageItem> messagesToSend;


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

        realm = DatabaseManager.getInstance().getRealm();

        getSyncInfo(account, this.user);

        messages = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account)
                .equalTo(MessageItem.Fields.USER, this.user)
                .findAllSortedAsync(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        messages.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                LogManager.i("AbstractChat", "messages changed size: " + messages.size() + " user: " + user);
            }
        });

        messagesToSend = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account)
                .equalTo(MessageItem.Fields.USER, this.user)
                .equalTo(MessageItem.Fields.SENT, false)
                .findAllSortedAsync(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        messagesToSend.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                LogManager.i("AbstractChat", "messagesToSend changed user: " + user);
                sendMessages();
            }
        });
    }

    private void getSyncInfo(String account, String user) {
        syncInfo = realm.where(SyncInfo.class).equalTo(SyncInfo.FIELD_ACCOUNT, account).equalTo(SyncInfo.FIELD_USER, user).findFirst();

        if (syncInfo == null) {
            realm.beginTransaction();

            syncInfo = realm.createObject(SyncInfo.class);
            syncInfo.setAccount(account);
            syncInfo.setUser(user);
            realm.commitTransaction();
        }
    }

    public RealmResults<MessageItem> getMessages() {
        return messages;
    }



    public boolean isLocalHistoryLoadedCompletely() {
        return isLocalHistoryLoadedCompletely;
    }

    public void onMessageDownloaded(final Collection<MessageItem> messagesFromServer) {

        if (messagesFromServer == null || messagesFromServer.isEmpty()) {
            return;
        }

        LogManager.i(this, "onMessageDownloaded: " + messagesFromServer.size());


        LogManager.i(this, "Adding new messages from MAM: " + messagesFromServer.size());

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<MessageItem> localMessages = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.ACCOUNT, account)
                        .equalTo(MessageItem.Fields.USER, user)
                        .findAll();

                Iterator<MessageItem> iterator = messagesFromServer.iterator();
                while (iterator.hasNext()) {
                    MessageItem remoteMessage = iterator.next();

                    if (localMessages.where()
                            .equalTo(MessageItem.Fields.STANZA_ID, remoteMessage.getStanzaId())
                            .count() > 0) {
                        LogManager.i(this, "Sync. Found messages with same Stanza ID. removing. Remote message:"
                                + " Text: " + remoteMessage.getText()
                                + " Timestamp: " + remoteMessage.getTimestamp()
                                + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                                + " StanzaId: " + remoteMessage.getStanzaId());
                        iterator.remove();
                        continue;
                    }

                    if (remoteMessage.getText() == null || remoteMessage.getTimestamp() == null) {
                        continue;
                    }

                    Long remoteMessageDelayTimestamp = remoteMessage.getDelayTimestamp();
                    Long remoteMessageTimestamp = remoteMessage.getTimestamp();

                    RealmResults<MessageItem> sameTextMessages = localMessages.where()
                            .equalTo(MessageItem.Fields.TEXT, remoteMessage.getText()).findAll();

                    if (isTimeStampSimilar(sameTextMessages, remoteMessageTimestamp)) {
                        LogManager.i(this, "Sync. Found messages with similar remote timestamp. Removing. Remote message:"
                                + " Text: " + remoteMessage.getText()
                                + " Timestamp: " + remoteMessage.getTimestamp()
                                + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                                + " StanzaId: " + remoteMessage.getStanzaId());
                        iterator.remove();
                        continue;
                    }

                    if (remoteMessageDelayTimestamp != null
                            && isTimeStampSimilar(sameTextMessages, remoteMessageDelayTimestamp)) {
                        LogManager.i(this, "Sync. Found messages with similar remote delay timestamp. Removing. Remote message:"
                                + " Text: " + remoteMessage.getText()
                                + " Timestamp: " + remoteMessage.getTimestamp()
                                + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                                + " StanzaId: " + remoteMessage.getStanzaId());
                        iterator.remove();
                        continue;
                    }
                }

                realm.copyToRealm(messagesFromServer);
            }
        }, null);
    }

    private boolean isTimeStampSimilar(RealmResults<MessageItem> sameTextMessages, long remoteMessageTimestamp) {
        long start = remoteMessageTimestamp - (1000 * 5);
        long end = remoteMessageTimestamp + (1000 * 5);

        if (sameTextMessages.where()
                .between(MessageItem.Fields.TIMESTAMP, start, end)
                .count() > 0) {
            LogManager.i(this, "Sync. Found messages with similar local timestamp");
            return true;
        }

        if (sameTextMessages.where()
                .between(MessageItem.Fields.DELAY_TIMESTAMP, start, end)
                .count() > 0) {
            LogManager.i(this, "Sync. Found messages with similar local delay timestamp.");
            return true;
        }
        return false;
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

        messagesToSend.removeChangeListeners();
        messages.removeChangeListeners();
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

        }
    }

    protected MessageItem newFileMessage(String text, String filePath, boolean isError) {
        Date timestamp = new Date();

        MessageItem messageItem = new MessageItem();
        messageItem.setAccount(account);
        messageItem.setUser(user);
        messageItem.setText(text);
        messageItem.setTimestamp(timestamp.getTime());
        messageItem.setRead(true);
        if (isError) {
            messageItem.setError(true);
        }
        messageItem.setFilePath(filePath);

        realm.beginTransaction();
        messageItem = realm.copyToRealm(messageItem);
        realm.commitTransaction();

        return messageItem;
    }

    private void updateSyncInfo() {
        LogManager.i(this, "updateSyncInfo messages size");

        final String firstMamMessageStanzaId = syncInfo.getFirstMamMessageStanzaId();

        Integer firstLocalMessagePosition = null;
        Integer firstMamMessagePosition = null;
        Date firstLocalMessageTimestamp = null;

        for (int i = 0; i < messages.size(); i++) {
            MessageItem messageItem = messages.get(i);
            String stanzaId = messageItem.getStanzaId();
//
//            if (firstLocalMessagePosition == null && messageItem.getId() != null) {
//                firstLocalMessagePosition = i;
//                firstLocalMessageTimestamp = messageItem.getTimestamp();
//                LogManager.i(this, "firstLocalMessagePosition " + firstLocalMessagePosition + " firstLocalMessageTimestamp " + firstLocalMessageTimestamp);
//            }


            if (firstMamMessagePosition == null && firstMamMessageStanzaId != null && stanzaId != null
                    && firstMamMessageStanzaId.equals(stanzaId)) {
                firstMamMessagePosition = i;

                LogManager.i(this, "firstMamMessagePosition " + i);
            }


            if (firstLocalMessagePosition != null) {
                if (firstMamMessagePosition != null) {
                    break;
                }
                if (firstMamMessageStanzaId == null) {
                    break;
                }
            }

        }

        syncCache.setFirstLocalMessagePosition(firstLocalMessagePosition);
        syncCache.setFirstLocalMessageTimeStamp(firstLocalMessageTimestamp);
        syncCache.setFirstMamMessagePosition(firstMamMessagePosition);

    }

    void removeMessage(final MessageItem messageItem) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                messageItem.removeFromRealm();
            }
        }, null);
    }

    void removeAllMessages() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                messages.clear();
            }
        }, null);

    }

    /**
     * @param bareAddress bareAddress of the user.
     * @param user        full jid.
     * @return Whether chat accepts packets from specified user.
     */
    boolean accept(String bareAddress, String user) {
        return this.user.equals(bareAddress);
    }

    /**
     * @return Whether chat can send messages.
     */
    protected boolean canSendMessage() {
        return !messagesToSend.isEmpty();
    }

    public String getLastText() {
        if (!messages.isEmpty()) {
            return messages.last().getText();
        } else {
            return "";
        }
    }

    /**
     * @return Time of last message in chat. Can be <code>null</code>.
     */
    public Date getLastTime() {
        if (!messages.isEmpty()) {
            return new Date(messages.last().getTimestamp());
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

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

                    realm.beginTransaction();
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
                    realm.commitTransaction();
                    EventBus.getDefault().post(new MessageUpdateEvent(messageItem.getAccount(), messageItem.getUser(), messageItem.getUniqueId()));
                }

            }
        });

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

    public boolean isLastMessageIncoming() {
        if (!messages.isEmpty()) {
            return messages.last().isIncoming();
        } else {
            return false;
        }
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

    public SyncInfo getSyncInfo() {
        return syncInfo;
    }

    public SyncCache getSyncCache() {
        return syncCache;
    }

    protected Realm getRealm() {
        return realm;
    }
}