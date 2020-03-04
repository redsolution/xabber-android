package com.xabber.android.data.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.realmobjects.NotifChatRealmObject;
import com.xabber.android.data.database.realmobjects.NotifMessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.filedownload.FileCategory;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
    private HashMap<Integer, Action> delayedActions = new HashMap<>();
    private long lastNotificationTime = 0;

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

    public boolean isTimeToNewFullNotification() {
        return System.currentTimeMillis() > (lastNotificationTime + 1000);
    }

    public void setLastNotificationTime() {
        this.lastNotificationTime = System.currentTimeMillis();
    }

    /** LISTENER */

    public void onNotificationAction(Action action) {
        if (action.getActionType() != Action.ActionType.cancel) {
            Chat chat = getChat(action.getNotificationID());
            if (chat != null) {
                performAction(new FullAction(action, chat.getAccountJid(), chat.getUserJid()));

                // update notification
                if (action.getActionType() == Action.ActionType.reply) {
                    addMessage(chat, "", action.getReplyText(), false);
                    saveNotifChatToRealm(chat);
                }
            }
        }

        // cancel notification
        if (action.getActionType() != Action.ActionType.reply) {
            notificationManager.cancel(action.getNotificationID());
            onNotificationCanceled(action.getNotificationID());
        }
    }

    public void onDelayedNotificationAction(Action action) {
        notificationManager.cancel(action.getNotificationID());
        delayedActions.put(action.getNotificationID(), action);
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

    public void onNewMessage(MessageRealmObject messageRealmObject) {
        boolean isMUC = messageRealmObject.isFromMUC();
        String chatTitle = RosterManager.getInstance().getBestContact(messageRealmObject.getAccount(), messageRealmObject.getUser()).getName();
        String author = isMUC ? messageRealmObject.getResource().toString() : chatTitle;
        Chat chat = getChat(messageRealmObject.getAccount(), messageRealmObject.getUser());
        if (chat == null) {
            chat = new Chat(messageRealmObject.getAccount(), messageRealmObject.getUser(),
                    getNextChatNotificationId(), chatTitle, isMUC);
            chats.add(chat);
        }
        addMessage(chat, author, getNotificationText(messageRealmObject), true);
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

    private void onNotificationCanceled(int notificationId) {
        if (notificationId == MESSAGE_BUNDLE_NOTIFICATION_ID)
            removeAllMessageNotifications();
        else removeChat(notificationId);
    }

    public void performAction(FullAction action) {
        AccountJid accountJid = action.getAccountJid();
        UserJid userJid = action.getUserJid();

        switch (action.getActionType()) {
            case read:
                AbstractChat chat = MessageManager.getInstance().getChat(accountJid, userJid);
                if (chat != null) {
                    AccountManager.getInstance().stopGracePeriod(chat.getAccount());
                    chat.markAsReadAll(true);
                    callUiUpdate();
                }
                break;
            case snooze:
                AbstractChat chat1 = MessageManager.getInstance().getChat(accountJid, userJid);
                if (chat1 != null) {
                    chat1.setNotificationState(new NotificationState(NotificationState.NotificationMode.snooze2h,
                            (int) (System.currentTimeMillis() / 1000L)), true);
                    callUiUpdate();
                }
                break;
            case reply:
                MessageManager.getInstance().sendMessage(accountJid, userJid, action.getReplyText().toString());
        }
    }

    private void onLoaded(List<Chat> loadedChats) {
        for (Chat chat : loadedChats) {
            if (delayedActions.containsKey(chat.notificationId)) {
                Action action = delayedActions.get(chat.notificationId);
                if (action != null) {
                    notificationManager.cancel(action.getNotificationID());
                    DelayedNotificationActionManager.getInstance().addAction(
                            new FullAction(action, chat.getAccountJid(), chat.getUserJid()));
                    removeNotifChatFromRealm(chat.accountJid, chat.userJid);
                }
            } else chats.add(chat);
        }
        delayedActions.clear();

        if (chats != null && chats.size() > 0) {
            List<Message> messages = chats.get(chats.size() - 1).getMessages();
            if (messages != null && messages.size() > 0) {
                lastMessage = messages.get(messages.size() - 1);
                //rebuildAllNotifications();
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
        return (int) System.currentTimeMillis();
    }

    private String getNotificationText(MessageRealmObject message) {
        String text = message.getText().trim();
        if (message.haveAttachments() && message.getAttachmentRealmObjects().size() > 0) {
            AttachmentRealmObject attachmentRealmObject = message.getAttachmentRealmObjects().get(0);
            if (attachmentRealmObject.isVoice()) {
                StringBuilder sb = new StringBuilder(Application.getInstance().getResources().getString(R.string.voice_message));
                if (attachmentRealmObject.getDuration() != null && attachmentRealmObject.getDuration() != 0) {
                    sb.append(String.format(Locale.getDefault(), ", %s",
                            StringUtils.getDurationStringForVoiceMessage(null, attachmentRealmObject.getDuration())));
                }
                text = sb.toString();
            } else {
                FileCategory category = FileCategory.determineFileCategory(attachmentRealmObject.getMimeType());
                text = FileCategory.getCategoryName(category, false) + attachmentRealmObject.getTitle();
            }
        }
        if (message.haveForwardedMessages() && message.getForwardedIds().size() > 0 && text.isEmpty()) {
            String forwardText = message.getFirstForwardedMessageText();
            if (forwardText != null && !forwardText.isEmpty()) text = forwardText;
            else text = context.getString(R.string.forwarded_messages_count, message.getForwardedIds().size());
        }
        return text;
    }

    /** REALM */

    /** Called not from Main thread */
    private List<Chat> loadNotifChatsFromRealm() {
        List<Chat> results = new ArrayList<>();
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<NotifChatRealmObject> items = realm
                .where(NotifChatRealmObject.class)
                .findAll();
        for (NotifChatRealmObject item : items) {
            Chat chat = new Chat(item.getId(), item.getAccount(), item.getUser(),
                    item.getNotificationID(), item.getChatTitle(), item.isGroupChat());
            for (NotifMessageRealmObject message : item.getMessages()) {
                chat.addMessage(new Message(message.getId(), message.getAuthor(), message.getText(),
                        message.getTimestamp()));
            }
            results.add(chat);
        }

        if (Looper.getMainLooper() != Looper.myLooper()) realm.close();
        return results;
    }

    private void removeNotifChatFromRealm(final AccountJid accountJid, final UserJid userJid) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<NotifChatRealmObject> items = realm1
                            .where(NotifChatRealmObject.class)
                            .equalTo(NotifChatRealmObject.Fields.ACCOUNT, accountJid.toString())
                            .equalTo(NotifChatRealmObject.Fields.USER, userJid.toString())
                            .findAll();
                    for (NotifChatRealmObject item : items) {
                        item.getMessages().deleteAllFromRealm();
                        item.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception("MessageNotificationManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void removeNotifChatFromRealm(final AccountJid accountJid) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<NotifChatRealmObject> items = realm1
                            .where(NotifChatRealmObject.class)
                            .equalTo(NotifChatRealmObject.Fields.ACCOUNT, accountJid.toString())
                            .findAll();
                    for (NotifChatRealmObject item : items) {
                        item.getMessages().deleteAllFromRealm();
                        item.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception("MessageNotificationManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void removeAllNotifChatFromRealm() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<NotifChatRealmObject> items = realm1
                            .where(NotifChatRealmObject.class)
                            .findAll();
                    for (NotifChatRealmObject item : items) {
                        item.getMessages().deleteAllFromRealm();
                        item.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception("MessageNotificationManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void saveNotifChatToRealm(final Chat chat) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    NotifChatRealmObject chatRealm = new NotifChatRealmObject(chat.getId());
                    chatRealm.setAccount(chat.getAccountJid());
                    chatRealm.setUser(chat.getUserJid());
                    chatRealm.setChatTitle(chat.getChatTitle().toString());
                    chatRealm.setNotificationID(chat.getNotificationId());
                    chatRealm.setGroupChat(chat.isGroupChat);
                    RealmList<NotifMessageRealmObject> messages = new RealmList<>();
                    for (Message message : chat.getMessages()) {
                        messages.add(messageToRealm(message));
                    }
                    chatRealm.setMessages(messages);
                    realm1.copyToRealmOrUpdate(chatRealm);
                });
            } catch (Exception e){
                LogManager.exception("MessageNotificationManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private NotifMessageRealmObject messageToRealm(Message message) {
        NotifMessageRealmObject messageRealm = new NotifMessageRealmObject(message.getId());
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
        private Handler removeTimer;

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
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopRemoveTimer();
                    removeTimer = new Handler();
                    removeTimer.postDelayed(new Runnable() {
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
            });
        }

        public void stopRemoveTimer() {
            if (removeTimer != null) removeTimer.removeCallbacksAndMessages(null);
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
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
                    messageText = URLDecoder.decode(messageText.toString(), StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                LogManager.exception(this, e);
                messageText = messageText.toString();
            }
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
