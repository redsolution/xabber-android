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

import android.os.Environment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsShowStatusChange;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountArchiveModeChangedListener;
import com.xabber.android.data.account.OnAccountDisabledListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.message.chat.MucPrivateChatNotification;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.OnStatusChangeListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.address.Jid;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Manage chats and its messages.
 * <p/>
 * Warning: message processing using chat instances should be changed.
 *
 * @author alexander.ivanov
 */
public class MessageManager implements OnLoadListener, OnPacketListener, OnDisconnectListener,
        OnAccountRemovedListener, OnAccountDisabledListener, OnRosterReceivedListener,
        OnAccountArchiveModeChangedListener, OnStatusChangeListener {

    private final static MessageManager instance;

    private final EntityNotificationProvider<MucPrivateChatNotification> mucPrivateChatRequestProvider;

    static {
        instance = new MessageManager();
        Application.getInstance().addManager(instance);
    }

    /**
     * Registered chats for bareAddresses in accounts.
     */
    private final NestedMap<AbstractChat> chats;
    /**
     * Visible chat.
     * <p/>
     * Will be <code>null</code> if there is no one.
     */
    private AbstractChat visibleChat;

    private MessageManager() {
        chats = new NestedMap<>();

        mucPrivateChatRequestProvider = new EntityNotificationProvider<>
                (R.drawable.ic_stat_muc_private_chat_request_white_24dp);
        mucPrivateChatRequestProvider.setCanClearNotifications(false);
    }

    public static MessageManager getInstance() {
        return instance;
    }


    @Override
    public void onLoad() {
        Realm realm = Realm.getDefaultInstance();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<MessageItem> messagesToSend = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.SENT, false)
                        .findAll();

                for (MessageItem messageItem : messagesToSend) {
                    String account = messageItem.getAccount();
                    String user = messageItem.getUser();
                    if (getChat(account, user) == null) {
                        createChat(account, user);
                    }
                }
            }
        }, null);
        realm.close();

        NotificationManager.getInstance().registerNotificationProvider(mucPrivateChatRequestProvider);
    }

    /**
     * @param account
     * @param user
     * @return <code>null</code> if there is no such chat.
     */
    public AbstractChat getChat(String account, String user) {
        return chats.get(account, user);
    }

    public Collection<AbstractChat> getChats() {
        final Map<String, List<String>> blockedContacts = BlockingManager.getInstance().getBlockedContacts();
        final Map<String, Collection<String>> blockedMucContacts = PrivateMucChatBlockingManager.getInstance().getBlockedContacts();
        List<AbstractChat> unblockedChats = new ArrayList<>();
        for (AbstractChat chat : chats.values()) {
            final List<String> blockedContactsForAccount = blockedContacts.get(chat.getAccount());
            if (blockedContactsForAccount != null) {
                if (blockedContactsForAccount.contains(chat.getUser())) {
                    continue;
                }
            }

            final Collection<String> blockedMucContactsForAccount = blockedMucContacts.get(chat.getAccount());
            if (blockedMucContactsForAccount != null) {
                if (blockedMucContactsForAccount.contains(chat.getUser())) {
                    continue;
                }
            }

            unblockedChats.add(chat);
        }

        return Collections.unmodifiableCollection(unblockedChats);
    }

    /**
     * Creates and adds new regular chat to be managed.
     *
     * @param account
     * @param user
     * @return
     */
    private RegularChat createChat(String account, String user) {
        RegularChat chat = new RegularChat(account, user, false);
        addChat(chat);
        return chat;
    }

    private RegularChat createPrivateMucChat(String account, String user) {
        RegularChat chat = new RegularChat(account, user, true);
        addChat(chat);
        return chat;
    }

    /**
     * Adds chat to be managed.
     *
     * @param chat
     */
    public void addChat(AbstractChat chat) {
        if (getChat(chat.getAccount(), chat.getUser()) != null) {
            throw new IllegalStateException();
        }
        chats.put(chat.getAccount(), chat.getUser(), chat);
    }

    /**
     * Removes chat from managed.
     *
     * @param chat
     */
    public void removeChat(AbstractChat chat) {
        chats.remove(chat.getAccount(), chat.getUser());
    }

    /**
     * Sends message. Creates and registers new chat if necessary.
     *
     * @param account
     * @param user
     * @param text
     */
    public void sendMessage(String account, String user, String text) {
        AbstractChat chat = getChat(account, user);
        if (chat == null) {
            chat = createChat(account, user);
        }
        sendMessage(text, chat);
    }

    private void sendMessage(final String text, final AbstractChat chat) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageItem newMessageItem = chat.createNewMessageItem(text);
                realm.copyToRealm(newMessageItem);
            }
        }, new Realm.Transaction.Callback() {
            @Override
            public void onSuccess() {
                super.onSuccess();
                chat.sendMessages();
            }
        });
        realm.close();
    }

    public String createFileMessage(String account, String user, File file) {
        AbstractChat chat = getChat(account, user);
        if (chat == null) {
            chat = createChat(account, user);
        }

        chat.openChat();
        return chat.newFileMessage(file);
    }

    public void updateFileMessage(String account, String user, final String messageId, final String text) {
        final AbstractChat chat = getChat(account, user);
        if (chat == null) {
            return;
        }

        Realm realm = Realm.getDefaultInstance();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageItem messageItem = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                        .findFirst();

                if (messageItem != null) {
                    messageItem.setText(text);
                    messageItem.setSent(false);
                }
            }
        }, new Realm.Transaction.Callback() {
            @Override
            public void onSuccess() {
                chat.sendMessages();
            }
        });

        realm.close();
    }

    public void updateMessageWithError(final String messageId) {
        Realm realm = Realm.getDefaultInstance();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageItem messageItem = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                        .findFirst();

                if (messageItem != null) {
                    messageItem.setError(true);
                }
            }
        }, null);

        realm.close();
    }

    /**
     * @param account
     * @param user
     * @return Where there is active chat.
     */
    public boolean hasActiveChat(String account, String user) {
        AbstractChat chat = getChat(account, user);
        return chat != null && chat.isActive();
    }

    /**
     * @return Collection with active chats.
     */
    public Collection<AbstractChat> getActiveChats() {
        Collection<AbstractChat> collection = new ArrayList<>();
        for (AbstractChat chat : chats.values()) {
            if (chat.isActive()) {
                collection.add(chat);
            }
        }
        return Collections.unmodifiableCollection(collection);
    }

    /**
     * Returns existed chat or create new one.
     *
     * @param account
     * @param user
     * @return
     */
    public AbstractChat getOrCreateChat(String account, String user) {
        String bareAddress = Jid.getBareAddress(user);

        if (MUCManager.getInstance().isMucPrivateChat(account, user)) {
            return getOrCreatePrivateMucChat(account, user);
        }

        AbstractChat chat = getChat(account, bareAddress);
        if (chat == null) {
            chat = createChat(account, bareAddress);
        }
        return chat;
    }

    public AbstractChat getOrCreatePrivateMucChat(String account, String user) {
        AbstractChat chat = getChat(account, user);
        if (chat == null) {
            chat = createPrivateMucChat(account, user);
        }
        return chat;
    }


    /**
     * Force open chat (make it active).
     *
     * @param account
     * @param user
     */
    public void openChat(String account, String user) {
        getOrCreateChat(account, user).openChat();
    }

    public void openPrivateMucChat(String account, String user) {
        getOrCreatePrivateMucChat(account, user).openChat();
    }

    /**
     * Closes specified chat (make it inactive).
     *
     * @param account
     * @param user
     */
    public void closeChat(String account, String user) {
        AbstractChat chat = getChat(account, user);
        if (chat == null) {
            return;
        }
        chat.closeChat();
    }

    /**
     * Sets currently visible chat.
     */
    public void setVisibleChat(BaseEntity visibleChat) {
        final boolean remove = !AccountManager.getInstance().getArchiveMode(visibleChat.getAccount()).saveLocally();
        AbstractChat chat = getChat(visibleChat.getAccount(), visibleChat.getUser());
        if (chat == null) {
            chat = createChat(visibleChat.getAccount(), visibleChat.getUser());
        } else {
            final String account = chat.getAccount();
            final String user = chat.getUser();

            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<MessageItem> unreadMessages = realm.where(MessageItem.class)
                            .equalTo(MessageItem.Fields.ACCOUNT, account)
                            .equalTo(MessageItem.Fields.USER, user)
                            .equalTo(MessageItem.Fields.READ, false)
                            .findAll();

                    List<MessageItem> unreadMessagesList = new ArrayList<>(unreadMessages);

                    for (MessageItem messageItem : unreadMessagesList) {
                        messageItem.setRead(true);
                    }

                    if (remove) {
                        unreadMessages.clear();
                    }
                }
            }, null);
            realm.close();
        }
        this.visibleChat = chat;
    }

    /**
     * All chats become invisible.
     */
    public void removeVisibleChat() {
        visibleChat = null;
    }

    /**
     * @param chat
     * @return Whether specified chat is currently visible.
     */
    boolean isVisibleChat(AbstractChat chat) {
        return visibleChat == chat;
    }

    /**
     * Removes all messages from chat.
     *
     * @param account
     * @param user
     */
    public void clearHistory(final String account, final String user) {

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
     * Removes message from history.
     *
     */
    public void removeMessage(final String messageItemId) {
        Realm realm = Realm.getDefaultInstance();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageItem first = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.UNIQUE_ID, messageItemId).findFirst();
                first.removeFromRealm();
            }
        });

        realm.close();
    }


    /**
     * Called on action settings change.
     */
    public void onSettingsChanged() {
//        ChatsShowStatusChange showStatusChange = SettingsManager.chatsShowStatusChange();
//        Collection<BaseEntity> changedEntities = new ArrayList<>();
//        for (AbstractChat chat : chats.values()) {
//            if ((chat instanceof RegularChat && showStatusChange != ChatsShowStatusChange.always)
//                    || (chat instanceof RoomChat && showStatusChange == ChatsShowStatusChange.never)) {
//                // Remove actions with status change.
//                ArrayList<MessageItem> remove = new ArrayList<>();
//                for (MessageItem messageItem : chat.getMessages()) {
//                    if (messageItem.getAction() != null && ChatAction.valueOf(messageItem.getAction()).isStatusChage()) {
//                        remove.add(messageItem);
//                    }
//                }
//                if (remove.isEmpty()) {
//                    continue;
//                }
//                for (MessageItem messageItem : remove) {
//                    chat.removeMessage(messageItem);
//                }
//                changedEntities.add(chat);
//            }
//        }
//        RosterManager.getInstance().onContactsChanged(changedEntities);
    }

    @Override
    public void onAccountArchiveModeChanged(AccountItem accountItem) {
        // TODO:
//        final ArchiveMode archiveMode = AccountManager.getInstance().getArchiveMode(accountItem.getAccount());
//        if (archiveMode.saveLocally()) {
//            return;
//        }
//        final String account = accountItem.getAccount();
//        Realm realm = DatabaseManager.getInstance().getRealm();
//        realm.beginTransaction();
//        for (AbstractChat chat : chats.getNested(account).values()) {
//            for (MessageItem messageItem : chat.getMessages()) {
//                if (archiveMode == ArchiveMode.dontStore || ((messageItem.isRead()
//                        || archiveMode != ArchiveMode.unreadOnly) && messageItem.isSent())) {
//                    messageItem.removeFromRealm();
//                }
//            }
//        }
//        realm.commitTransaction();
//        // If message was read or received after removeMessageItems
//        // was created then it's ID will be not null. DB actions with
//        // such message will have no effect as if it was removed.
//        // History ids becomes invalid and will be cleared on next
//        // history load.
//
//        AccountManager.getInstance().onAccountChanged(accountItem.getAccount());
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();
        if (bareAddress == null) {
            return;
        }

        String contact = bareAddress;

        if (packet instanceof Message) {
            Message message = (Message) packet;
            if (MUCManager.getInstance().hasRoom(account, bareAddress)
                    && message.getType() != Message.Type.groupchat ) {
                contact = packet.getFrom();
            }
        }



        final String user = packet.getFrom();
        boolean processed = false;
        for (AbstractChat chat : chats.getNested(account).values()) {
            if (chat.onPacket(contact, packet)) {
                processed = true;
                break;
            }
        }

        final AbstractChat chat = getChat(account, user);

        if (chat != null && packet instanceof Message) {
            if (chat.isPrivateMucChat() && !chat.isPrivateMucChatAccepted()) {
                if (mucPrivateChatRequestProvider.get(chat.getAccount(), chat.getUser()) == null) {
                    if (!PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account).contains(chat.getUser())) {
                        mucPrivateChatRequestProvider.add(new MucPrivateChatNotification(account, user), true);
                    }
                }
            }


            return;
        }
        if (!processed && packet instanceof Message) {
            final Message message = (Message) packet;
            final String body = message.getBody();
            if (body == null) {
                return;
            }

            if (message.getType() == Message.Type.chat && MUCManager.getInstance().hasRoom(account, Jid.getBareAddress(user))) {
                createPrivateMucChat(account, user).onPacket(contact, packet);
                if (!PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account).contains(user)) {
                    mucPrivateChatRequestProvider.add(new MucPrivateChatNotification(account, user), true);
                }
                return;
            }

            for (ExtensionElement packetExtension : message.getExtensions()) {
                if (packetExtension instanceof MUCUser) {
                    return;
                }
            }

            createChat(account, user).onPacket(contact, packet);
        }
    }

    public void displayForwardedMessage(ConnectionItem connection, final Message message, CarbonExtension.Direction direction) {

        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();

        if (direction == CarbonExtension.Direction.sent) {
            String companion = Jid.getBareAddress(message.getTo());
            if (companion == null) {
                return;
            }
            AbstractChat chat = getChat(account, companion);
            if (chat == null) {
                chat = createChat(account, companion);
            }
            final String body = message.getBody();
            if (body == null) {
                return;
            }

            Realm realm = Realm.getDefaultInstance();
            final AbstractChat finalChat = chat;
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    MessageItem newMessageItem = finalChat.createNewMessageItem(body);
                    newMessageItem.setStanzaId(message.getStanzaId());
                    newMessageItem.setSent(true);
                    realm.copyToRealm(newMessageItem);
                }
            }, null);
            realm.close();
            return;
        }

        String companion = Jid.getBareAddress(message.getFrom());
        boolean processed = false;
        for (AbstractChat chat : chats.getNested(account).values()) {
            if (chat.onPacket(companion, message)) {
                processed = true;
                break;
            }
        }
        if (getChat(account, companion) != null) {
            return;
        }
        if (processed) {
            return;
        }
        final String body = message.getBody();
        if (body == null) {
            return;
        }
        createChat(account, companion).onPacket(companion, message);

    }
    @Override
    public void onRosterReceived(AccountItem accountItem) {
        String account = accountItem.getAccount();
        for (AbstractChat chat : chats.getNested(account).values()) {
            chat.onComplete();
        }
    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();
        for (AbstractChat chat : chats.getNested(account).values()) {
            chat.onDisconnect();
        }
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        chats.clear(accountItem.getAccount());
    }

    @Override
    public void onAccountDisabled(AccountItem accountItem) {
        chats.clear(accountItem.getAccount());
    }

    /**
     * Export chat to file with specified name.
     *
     * @param account
     * @param user
     * @param fileName
     * @throws NetworkException
     */
    public File exportChat(String account, String user, String fileName) throws NetworkException {
        final File file = new File(Environment.getExternalStorageDirectory(), fileName);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            final String titleName = RosterManager.getInstance().getName(account, user) + " (" + user + ")";
            out.write("<html><head><title>");
            out.write(StringUtils.escapeHtml(titleName));
            out.write("</title></head><body>");
            final AbstractChat abstractChat = getChat(account, user);
            if (abstractChat != null) {
                final boolean isMUC = abstractChat instanceof RoomChat;
                final String accountName = AccountManager.getInstance().getNickName(account);
                final String userName = RosterManager.getInstance().getName(account, user);

                Realm realm = Realm.getDefaultInstance();
                RealmResults<MessageItem> messageItems = realm.where(MessageItem.class)
                        .equalTo(MessageItem.Fields.ACCOUNT, account)
                        .equalTo(MessageItem.Fields.USER, user)
                        .findAllSorted(MessageItem.Fields.TIMESTAMP);

                for (MessageItem messageItem : messageItems) {
                    if (messageItem.getAction() != null) {
                        continue;
                    }
                    final String name;
                    if (isMUC) {
                        name = messageItem.getResource();
                    } else {
                        if (messageItem.isIncoming()) {
                            name = userName;
                        } else {
                            name = accountName;
                        }
                    }
                    out.write("<b>");
                    out.write(StringUtils.escapeHtml(name));
                    out.write("</b>&nbsp;(");
                    out.write(StringUtils.getDateTimeText(new Date(messageItem.getTimestamp())));
                    out.write(")<br />\n<p>");
                    out.write(StringUtils.escapeHtml(messageItem.getText()));
                    out.write("</p><hr />\n");
                }
                realm.close();
            }
            out.write("</body></html>");
            out.close();
        } catch (IOException e) {
            throw new NetworkException(R.string.FILE_NOT_FOUND);
        }
        return file;
    }

    /**
     * Notifies registered {@link OnChatChangedListener}.
     *
     * @param account
     * @param user
     * @param incoming
     */
    public void onChatChanged(final String account, final String user,
                              final boolean incoming) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnChatChangedListener onChatChangedListener
                        : Application.getInstance().getUIListeners(OnChatChangedListener.class)) {
                    onChatChangedListener.onChatChanged(account, user, incoming);
                }
            }
        });
    }

    private boolean isStatusTrackingEnabled(String account, String bareAddress) {
        if (SettingsManager.chatsShowStatusChange() != ChatsShowStatusChange.always) {
            return false;
        }
        AbstractChat abstractChat = getChat(account, bareAddress);
        return abstractChat != null && abstractChat instanceof RegularChat && abstractChat.isStatusTrackingEnabled();
    }

    @Override
    public void onStatusChanged(String account, String bareAddress, String resource, String statusText) {
        if (isStatusTrackingEnabled(account, bareAddress)) {
            getChat(account, bareAddress).newAction(resource, statusText, ChatAction.status);
        }
    }

    @Override
    public void onStatusChanged(String account, String bareAddress, String resource,
                                StatusMode statusMode, String statusText) {
        if (isStatusTrackingEnabled(account, bareAddress)) {
            getChat(account, bareAddress).newAction(resource, statusText, ChatAction.getChatAction(statusMode));
        }
    }

    public void acceptMucPrivateChat(String account, String user) {
        mucPrivateChatRequestProvider.remove(account, user);
        getOrCreatePrivateMucChat(account, user).setIsPrivateMucChatAccepted(true);
    }

    public void discardMucPrivateChat(String account, String user) {
        mucPrivateChatRequestProvider.remove(account, user);
    }
}