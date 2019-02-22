package com.xabber.android.data.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.NotifChatRealm;
import com.xabber.android.data.database.realm.NotifMessageRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.filedownload.FileCategory;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MessageNotificationManager implements OnLoadListener {

    private final static int MESSAGE_BUNDLE_NOTIFICATION_ID = 2;
    private final Application context;
    private final NotificationManager notificationManager;
    private final MessageNotificationCreator creator;
    private static MessageNotificationManager instance;
    private List<Chat> chats = new ArrayList<>();
    private Message lastMessage = null;

    private MessageNotificationManager() {
        context = Application.getInstance();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        creator = new MessageNotificationCreator(context, notificationManager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelUtils.createMessageChannel(notificationManager,
                    NotificationChannelUtils.ChannelType.privateChat,
                    null, null, null);

            NotificationChannelUtils.createMessageChannel(notificationManager,
                    NotificationChannelUtils.ChannelType.groupChat,
                    null, null, null);

            NotificationChannelUtils.createMessageChannel(notificationManager,
                    NotificationChannelUtils.ChannelType.attention,
                    null, null, null);
        }

    }

    public static MessageNotificationManager getInstance() {
        if (instance == null) instance = new MessageNotificationManager();
        return instance;
    }

    /** LISTENER */

    public void onNotificationReplied(int notificationId, final CharSequence replyText) {
        final Chat chat = getChat(notificationId);
        if (chat != null) {
            // send message
            MessageManager.getInstance().sendMessage(
                    chat.getAccountJid(), chat.getUserJid(), replyText.toString());

            // update notification
            addMessage(chat, "", replyText, false);
            saveNotifChatToRealm(chat);
        }
    }

    public void onNotificationCanceled(int notificationId) {
        if (notificationId == MESSAGE_BUNDLE_NOTIFICATION_ID)
            removeAllMessageNotifications();
        else removeChat(notificationId);
    }

    public void onNotificationMuted(int notificationId) {
        Chat chatNotif = getChat(notificationId);
        if (chatNotif != null) {
            AbstractChat chat = MessageManager.getInstance().getChat(
                    chatNotif.getAccountJid(), chatNotif.getUserJid());
            if (chat != null) {
                chat.setNotificationState(new NotificationState(NotificationState.NotificationMode.snooze2h,
                        (int) (System.currentTimeMillis() / 1000L)), true);
                callUiUpdate();
            }
        }

        // cancel notification
        notificationManager.cancel(notificationId);
        onNotificationCanceled(notificationId);
    }

    public void onNotificationMarkedAsRead(int notificationId) {
        // mark chat as read
        Chat chatNotif = getChat(notificationId);
        if (chatNotif != null) {
            AbstractChat chat = MessageManager.getInstance().getChat(
                    chatNotif.getAccountJid(), chatNotif.getUserJid());
            if (chat != null) {
                chat.resetUnreadMessageCount();
                callUiUpdate();
            }
        }

        // cancel notification
        notificationManager.cancel(notificationId);
        onNotificationCanceled(notificationId);
    }

    @Override
    public void onLoad() {
        final List<Chat> chats = loadNotifChatsFromRealm();
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(chats);
            }
        });
    }

    /** PUBLIC METHODS */

    public void onNewMessage(MessageItem messageItem) {
        boolean isMUC = messageItem.isFromMUC();
        String chatTitle = RosterManager.getInstance().getBestContact(messageItem.getAccount(), messageItem.getUser()).getName();
        String author = isMUC ? messageItem.getResource().toString() : chatTitle;
        Chat chat = getChat(messageItem.getAccount(), messageItem.getUser());
        if (chat == null) {
            chat = new Chat(messageItem.getAccount(), messageItem.getUser(),
                    getNextChatNotificationId(), chatTitle, isMUC);
            chats.add(chat);
        }
        addMessage(chat, author, getNotificationText(messageItem), true);
        saveNotifChatToRealm(chat);
    }

    public void removeChatWithTimer(final AccountJid account, final UserJid user) {
        Chat chat = getChat(account, user);
        if (chat != null) chat.startRemoveTimer();
    }

    public void removeChat(final AccountJid account, final UserJid user) {
        Chat chat = getChat(account, user);
        if (chat != null) {
            chats.remove(chat);
            removeNotification(chat);
            removeNotifChatFromRealm(account, user);
        }
    }

    public void removeChat(int notificationId) {
        Chat chat = getChat(notificationId);
        if (chat != null) {
            chats.remove(chat);
            removeNotification(chat);
            removeNotifChatFromRealm(chat.accountJid, chat.userJid);
        }
    }

    public void removeNotificationsForAccount(final AccountJid account) {
        List<Chat> chatsToRemove = new ArrayList<>();
        Iterator it = chats.iterator();
        while (it.hasNext()) {
            Chat chat = (Chat) it.next();
            if (chat.getAccountJid().equals(account)) {
                chatsToRemove.add(chat);
                it.remove();
            }
        }
        removeNotifications(chatsToRemove);
        removeNotifChatFromRealm(account);
    }

    public void removeAllMessageNotifications() {
        List<Chat> chatsToRemove = new ArrayList<>();
        Iterator it = chats.iterator();
        while (it.hasNext()) {
            Chat chat = (Chat) it.next();
            chatsToRemove.add(chat);
            it.remove();
        }
        removeNotifications(chatsToRemove);
        removeAllNotifChatFromRealm();
    }

    public void rebuildAllNotifications() {
        notificationManager.cancelAll();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (Chat chat : chats) creator.createNotification(chat, true);
            if (chats.size() > 1) creator.createBundleNotification(chats, true);
        } else {
            if (chats.size() > 1) creator.createBundleNotification(chats, true);
            else if (chats.size() > 0) creator.createNotification(chats.get(0), true);
        }
    }

    /** PRIVATE METHODS */

    private void onLoaded(List<Chat> loadedChats) {
        this.chats.addAll(loadedChats);
        if (loadedChats != null && loadedChats.size() > 0) {
            List<Message> messages = loadedChats.get(loadedChats.size() - 1).getMessages();
            if (messages != null && messages.size() > 0) {
                lastMessage = messages.get(messages.size() - 1);
                rebuildAllNotifications();
            }
        }
    }

    private void addMessage(Chat notification, CharSequence author, CharSequence messageText, boolean alert) {
        lastMessage = new Message(author, messageText, System.currentTimeMillis());
        notification.addMessage(lastMessage);
        notification.stopRemoveTimer();
        addNotification(notification, alert);
    }

    private Chat getChat(AccountJid account, UserJid user) {
        for (Chat item : chats) {
            if (item.equals(account, user))
                return item;
        }
        return null;
    }

    private Chat getChat(int notificationId) {
        for (Chat item : chats) {
            if (item.getNotificationId() == notificationId)
                return item;
        }
        return null;
    }

    private void addNotification(Chat chat, boolean alert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (chats.size() > 1) creator.createBundleNotification(chats, true);
            creator.createNotification(chat, alert);
        } else {
            if (chats.size() > 1) {
                if (chats.size() == 2) {
                    notificationManager.cancel(chats.get(0).getNotificationId());
                    notificationManager.cancel(chats.get(1).getNotificationId());
                }
                creator.createBundleNotification(chats, true);
            }
            else if (chats.size() > 0) creator.createNotification(chats.get(0), true);
        }
    }

    private void removeNotification(Chat chat) {
        List<Chat> chatsToRemove = new ArrayList<>();
        chatsToRemove.add(chat);
        removeNotifications(chatsToRemove);
    }

    private void removeNotifications(List<Chat> chatsToRemove) {
        if (chatsToRemove == null || chatsToRemove.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (chats.size() > 1) creator.createBundleNotification(chats, true);
            for (Chat chat : chatsToRemove) {
                notificationManager.cancel(chat.getNotificationId());
            }
            if (chats.size() == 0) notificationManager.cancel(MESSAGE_BUNDLE_NOTIFICATION_ID);
        } else {
            if (chats.size() > 1) creator.createBundleNotification(chats, false);
            else if (chats.size() > 0) {
                notificationManager.cancel(MESSAGE_BUNDLE_NOTIFICATION_ID);
                creator.createNotification(chats.get(0), false);
            } else {
                for (Chat chat : chatsToRemove) {
                    notificationManager.cancel(chat.getNotificationId());
                }
            }
        }
    }

    private void callUiUpdate() {
        for (OnContactChangedListener onContactChangedListener : Application
                .getInstance().getUIListeners(OnContactChangedListener.class)) {
            onContactChangedListener.onContactsChanged(new ArrayList<RosterContact>());
        }
    }

    private int getNextChatNotificationId() {
        return 100 + chats.size() + 1;
    }

    private String getNotificationText(MessageItem message) {
        String text = message.getText();
        if (message.haveAttachments() && message.getAttachments().size() > 0) {
            Attachment attachment = message.getAttachments().get(0);
            FileCategory category = FileCategory.determineFileCategory(attachment.getMimeType());
            text = FileCategory.getCategoryName(category, false) + attachment.getTitle();
        }
        if (message.haveForwardedMessages() && message.getForwardedIds().size() > 0) {
            text = context.getString(R.string.forwarded_messages_count, message.getForwardedIds().size());
        }
        return text;
    }

    /** REALM */

    /** Called not from Main thread */
    private List<Chat> loadNotifChatsFromRealm() {
        List<Chat> results = new ArrayList<>();
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<NotifChatRealm> items = realm.where(NotifChatRealm.class).findAll();
        for (NotifChatRealm item : items) {
            Chat chat = new Chat(item.getId(), item.getAccount(), item.getUser(),
                    item.getNotificationID(), item.getChatTitle(), item.isGroupChat());
            for (NotifMessageRealm message : item.getMessages()) {
                chat.addMessage(new Message(message.getId(), message.getAuthor(), message.getText(), message.getTimestamp()));
            }
            results.add(chat);
        }
        realm.close();
        return results;
    }

    private void removeNotifChatFromRealm(final AccountJid accountJid, final UserJid userJid) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                RealmResults<NotifChatRealm> items = realm.where(NotifChatRealm.class)
                        .equalTo(NotifChatRealm.Fields.ACCOUNT, accountJid.toString())
                        .equalTo(NotifChatRealm.Fields.USER, userJid.toString()).findAll();
                removeNotifChatFromRealm(realm, items);
            }
        });
    }

    private void removeNotifChatFromRealm(final AccountJid accountJid) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                RealmResults<NotifChatRealm> items = realm.where(NotifChatRealm.class)
                        .equalTo(NotifChatRealm.Fields.ACCOUNT, accountJid.toString()).findAll();
                removeNotifChatFromRealm(realm, items);
            }
        });
    }

    private void removeAllNotifChatFromRealm() {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                RealmResults<NotifChatRealm> items = realm.where(NotifChatRealm.class).findAll();
                removeNotifChatFromRealm(realm, items);
            }
        });
    }

    private void removeNotifChatFromRealm(Realm realm, RealmResults<NotifChatRealm> items) {
        realm.beginTransaction();
        for (NotifChatRealm item : items) {
            item.getMessages().deleteAllFromRealm();
            item.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    private void saveNotifChatToRealm(final Chat chat) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotifChatRealm chatRealm = new NotifChatRealm(chat.getId());
                chatRealm.setAccount(chat.getAccountJid());
                chatRealm.setUser(chat.getUserJid());
                chatRealm.setChatTitle(chat.getChatTitle().toString());
                chatRealm.setNotificationID(chat.getNotificationId());
                chatRealm.setGroupChat(chat.isGroupChat);
                RealmList<NotifMessageRealm> messages = new RealmList<>();
                for (Message message : chat.getMessages()) {
                    messages.add(messageToRealm(message));
                }
                chatRealm.setMessages(messages);

                Realm realm = RealmManager.getInstance().getNewRealm();
                realm.beginTransaction();
                NotifChatRealm result = realm.copyToRealmOrUpdate(chatRealm);
                realm.commitTransaction();
            }
        });
    }

    private NotifMessageRealm messageToRealm(Message message) {
        NotifMessageRealm messageRealm = new NotifMessageRealm(message.getId());
        messageRealm.setAuthor(message.getAuthor().toString());
        messageRealm.setText(message.getMessageText().toString());
        messageRealm.setTimestamp(message.getTimestamp());
        return messageRealm;
    }

    /** INTERNAL CLASSES */

    public class Chat {
        private String id;
        private AccountJid accountJid;
        private UserJid userJid;
        private int notificationId;
        private CharSequence chatTitle;
        private boolean isGroupChat;
        private List<Message> messages = new ArrayList<>();
        private Timer removeTimer;

        public Chat(AccountJid accountJid, UserJid userJid, int notificationId,
                    CharSequence chatTitle, boolean isGroupChat) {
            this.accountJid = accountJid;
            this.userJid = userJid;
            this.notificationId = notificationId;
            this.chatTitle = chatTitle;
            this.id = UUID.randomUUID().toString();
            this.isGroupChat = isGroupChat;
        }

        public Chat(String id, AccountJid accountJid, UserJid userJid, int notificationId,
                    CharSequence chatTitle, boolean isGroupChat) {
            this.id = id;
            this.accountJid = accountJid;
            this.userJid = userJid;
            this.notificationId = notificationId;
            this.chatTitle = chatTitle;
            this.isGroupChat = isGroupChat;
        }

        public String getId() {
            return id;
        }

        public void addMessage(Message message) {
            messages.add(message);
        }

        public int getNotificationId() {
            return notificationId;
        }

        public AccountJid getAccountJid() {
            return accountJid;
        }

        public UserJid getUserJid() {
            return userJid;
        }

        public CharSequence getChatTitle() {
            return chatTitle;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public boolean isGroupChat() {
            return isGroupChat;
        }

        public long getLastMessageTimestamp() {
            return messages.get(messages.size() - 1).getTimestamp();
        }

        public Message getLastMessage() {
            return messages.get(messages.size() - 1);
        }

        public boolean equals(AccountJid account, UserJid user) {
            return this.accountJid.equals(account) && this.userJid.equals(user);
        }

        public void startRemoveTimer() {
            stopRemoveTimer();
            removeTimer = new Timer();
            removeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            removeChat(notificationId);
                        }
                    });
                }
            }, 500);
        }

        public void stopRemoveTimer() {
            if (removeTimer != null) removeTimer.cancel();
        }

    }

    public class Message {
        private String id;
        private CharSequence author;
        private CharSequence messageText;
        private long timestamp;

        public Message(CharSequence author, CharSequence messageText, long timestamp) {
            this.author = author;
            this.messageText = messageText;
            this.timestamp = timestamp;
            this.id = UUID.randomUUID().toString();
        }

        public Message(String id, CharSequence author, CharSequence messageText, long timestamp) {
            this.id = id;
            this.author = author;
            this.messageText = messageText;
            this.timestamp = timestamp;
        }

        public CharSequence getAuthor() {
            return author;
        }

        public CharSequence getMessageText() {
            return messageText;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getId() {
            return id;
        }
    }
}
