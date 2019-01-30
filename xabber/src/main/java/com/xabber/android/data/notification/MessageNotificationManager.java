package com.xabber.android.data.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.Person;
import android.support.v4.app.RemoteInput;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.NotifChatRealm;
import com.xabber.android.data.database.realm.NotifMessageRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.NotificationReceiver;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MessageNotificationManager implements OnLoadListener {

    private final static String MESSAGE_CHANNEL_ID = "MESSAGE_CHANNEL";
    private final static String MESSAGE_GROUP_ID = "MESSAGE_GROUP";
    private final static int MESSAGE_GROUP_NOTIFICATION_ID = 0;
    private static final int COLOR = 299031;
    private static final String DISPLAY_NAME = "You";

    private final Application context;
    private final NotificationManager notificationManager;
    private static MessageNotificationManager instance;
    private List<Chat> chats = new ArrayList<>();
    private Message lastMessage = null;

    public MessageNotificationManager() {
        context = Application.getInstance();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();
    }

    public static MessageNotificationManager getInstance() {
        if (instance == null) instance = new MessageNotificationManager();
        return instance;
    }

    /** LISTENER */

    public void onNotificationReplied(int notificationId, CharSequence replyText) {
        // send message to xmpp

        // update notification
        Chat chat = getChat(notificationId);
        if (chat != null) {
            addMessage(chat, DISPLAY_NAME, replyText);
            saveNotifChatToRealm(chat);
        }
    }

    public void onNotificationCanceled(int notificationId) {
        removeChat(notificationId);
    }

    public void onNotificationMarkedAsRead(int notificationId) {
        // mute chat
        Log.d("NOTIFICATION_TEST", "mark as read " + notificationId);

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
        String author = RosterManager.getInstance().getBestContact(messageItem.getAccount(), messageItem.getUser()).getName();
        Chat chat = getChat(messageItem.getAccount(), messageItem.getUser());
        if (chat == null) {
            chat = new Chat(messageItem.getAccount(), messageItem.getUser(), getNextChatNotificationId(), author);
            chats.add(chat);
        }
        addMessage(chat, author, messageItem.getText());
        saveNotifChatToRealm(chat);
    }

    public void removeChat(final AccountJid account, final UserJid user) {
        Chat chat = getChat(account, user);
        if (chat != null) {
            chats.remove(chat);
            rebuildAllNotifications();
            removeNotifChatFromRealm(account, user);
        }
    }

    public void removeChat(int notificationId) {
        Chat chat = getChat(notificationId);
        if (chat != null) {
            chats.remove(chat);
            rebuildAllNotifications();
            removeNotifChatFromRealm(chat.accountJid, chat.userJid);
        }
    }

    public void removeNotificationsForAccount(final AccountJid account) {
        for (Chat chat : chats) {
            if (chat.getAccountJid().equals(account))
                chats.remove(chat);
        }
        rebuildAllNotifications();
        removeNotifChatFromRealm(account);
    }

    public void onClearNotifications() {
        notificationManager.cancelAll();
        chats.clear();
        removeAllNotifChatFromRealm();
    }

    /** PRIVATE METHODS */

    private void onLoaded(List<Chat> loadedChats) {
        this.chats.addAll(loadedChats);
        rebuildAllNotifications();
    }

    private void addMessage(Chat notification, CharSequence author, CharSequence messageText) {
        lastMessage = new Message(author, messageText, System.currentTimeMillis());
        notification.addMessage(lastMessage);
        rebuildAllNotifications();
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

    public void rebuildAllNotifications() {
        notificationManager.cancelAll();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (Chat chat : chats) createChatNotification(chat);
            if (chats.size() > 1) createGroupNotification();
        } else {
            if (chats.size() > 1) createGroupNotificationOldAPI();
            else createChatNotificationOldAPI(chats.get(0));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createChatNotification(Chat chat) {
        NotificationCompat.Style messageStyle = new NotificationCompat.MessagingStyle(DISPLAY_NAME);
        for (Message message : chat.getMessages()) {

            Person person = new Person.Builder()
                    .setName(message.getAuthor())
                    .build();

            ((NotificationCompat.MessagingStyle) messageStyle).addMessage(
                    new NotificationCompat.MessagingStyle.Message(message.getMessageText(), message.getTimestamp(), person));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                .setColor(COLOR)
                .setSmallIcon(R.drawable.ic_message)
                .setLargeIcon(getLargeIcon(chat))
                .setStyle(messageStyle)
                .setGroup(MESSAGE_GROUP_ID)
                .addAction(createReplyAction(chat.getNotificationId()))
                .addAction(createMarkAsReadAction(chat.getNotificationId()))
                .addAction(createMuteAction(chat.getNotificationId()))
                .setContentIntent(createContentIntent(chat))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getNotificationId()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, chat.getNotificationId());
    }

    private void createChatNotificationOldAPI(Chat chat) {
        int messageCount = chat.getMessages().size();
        CharSequence title;
        if (messageCount > 1)
            title = messageCount + " messages from " + chat.getChatTitle();
        else title = chat.getChatTitle();

        CharSequence content = lastMessage.getMessageText();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                .setColor(COLOR)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(createInboxStyle(chat))
                .setGroup(MESSAGE_GROUP_ID)
                .addAction(createMarkAsReadAction(chat.getNotificationId()))
                .addAction(createMuteAction(chat.getNotificationId()))
                .setContentIntent(createContentIntent(chat))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getNotificationId()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, chat.getNotificationId());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createGroupNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                        .setColor(COLOR)
                        .setSmallIcon(R.drawable.ic_message)
                        .setSubText(getMessageCount() + " new messages")
                        .setGroup(MESSAGE_GROUP_ID)
                        .setGroupSummary(true)
                        .setContentIntent(createGroupContentIntent())
                        .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, MESSAGE_GROUP_NOTIFICATION_ID))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, MESSAGE_GROUP_NOTIFICATION_ID);
    }

    private void createGroupNotificationOldAPI() {

        int messageCount = getMessageCount();
        int chatCount = chats.size();

        CharSequence title = messageCount + " messages from " + chatCount + " chats";
        CharSequence content = createLine(lastMessage.getAuthor(), lastMessage.getMessageText());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                .setColor(COLOR)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(createInboxStyleForGroup())
                .setGroup(MESSAGE_GROUP_ID)
                .setContentIntent(createGroupContentIntent())
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, MESSAGE_GROUP_NOTIFICATION_ID))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, MESSAGE_GROUP_NOTIFICATION_ID);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        CharSequence name = "New message notification";
        String description = "Shows notifications about new messages";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(MESSAGE_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    /** REALM */

    /** Called not from Main thread */
    private List<Chat> loadNotifChatsFromRealm() {
        List<Chat> results = new ArrayList<>();
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<NotifChatRealm> items = realm.where(NotifChatRealm.class).findAll();
        for (NotifChatRealm item : items) {
            Chat chat = new Chat(item.getId(), item.getAccount(), item.getUser(),
                    item.getNotificationID(), item.getChatTitle());
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

    /** ACTIONS */

    private NotificationCompat.Action createReplyAction(int notificationId) {
        RemoteInput remoteInput = new RemoteInput.Builder(NotificationReceiver.KEY_REPLY_TEXT)
                .setLabel("Input your message here")
                .build();

        return new NotificationCompat.Action.Builder(R.drawable.ic_message,
                "Reply", NotificationReceiver.createReplyIntent(context, notificationId))
                .addRemoteInput(remoteInput)
                .build();
    }

    private NotificationCompat.Action createMarkAsReadAction(int notificationId) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_message,
                "Mark as read", NotificationReceiver.createMarkAsReadIntent(context, notificationId))
                .build();
    }

    private NotificationCompat.Action createMuteAction(int notificationId) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_message,
                "Mute", NotificationReceiver.createMuteIntent(context, notificationId))
                .build();
    }

    private PendingIntent createContentIntent(Chat chat) {
        Intent backIntent = ContactListActivity.createIntent(Application.getInstance());
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = ChatActivity.createClearTopIntent(Application.getInstance(), chat.getAccountJid(), chat.getUserJid());
        return PendingIntent.getActivities(Application.getInstance(), chat.getNotificationId(),
                new Intent[]{backIntent, intent}, PendingIntent.FLAG_ONE_SHOT);
    }

    private PendingIntent createGroupContentIntent() {
        return PendingIntent.getActivity(context, 0,
                ContactListActivity.createPersistentIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** UTILS */

    private void sendNotification(NotificationCompat.Builder builder, int notificationId) {
        notificationManager.notify(notificationId, builder.build());
    }

    private NotificationCompat.Style createInboxStyle(Chat chat) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int startPos = chat.getMessages().size() <= 7 ? 0 : chat.getMessages().size() - 7;
        for (int i = startPos; i < chat.getMessages().size(); i++) {
            Message message = chat.getMessages().get(i);
            inboxStyle.addLine(message.getMessageText());

        }
        return inboxStyle;
    }

    private NotificationCompat.Style createInboxStyleForGroup() {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        Collections.sort(chats, Collections.reverseOrder(new SortByLastMessage()));
        int count = 0;
        for (Chat chat : chats) {
            if (count >= 7) break;
            Message message = chat.getMessages().get(chat.getMessages().size() - 1);
            inboxStyle.addLine(createLine(chat.getChatTitle(), message.getMessageText()));
            count++;
        }
        inboxStyle.setSummaryText("valery.miller@xabber.com");
        return inboxStyle;
    }

    private Spannable createLine(CharSequence name, CharSequence message) {
        String contactAndMessage = context.getString(R.string.chat_contact_and_message, name, message);
        Spannable spannable =  new SpannableString(contactAndMessage);
        spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private android.graphics.Bitmap getLargeIcon(Chat chat) {
        String name = RosterManager.getInstance().getName(chat.getAccountJid(), chat.getUserJid());
        if (MUCManager.getInstance().hasRoom(chat.getAccountJid(), chat.getUserJid().getJid().asEntityBareJidIfPossible())) {
            return AvatarManager.getInstance().getRoomBitmap(chat.getUserJid());
        } else {
            return AvatarManager.getInstance().getUserBitmap(chat.getUserJid(), name);
        }
    }

    private int getMessageCount() {
        int result = 0;
        for (Chat notification : chats) {
            result += notification.getMessages().size();
        }
        return result;
    }

    private int getNextChatNotificationId() {
        return 100 + chats.size() + 1;
    }

    /** INTERNAL CLASSES */

    private class Chat {
        private String id;
        private AccountJid accountJid;
        private UserJid userJid;
        private int notificationId;
        private CharSequence chatTitle;
        private List<Message> messages = new ArrayList<>();

        public Chat(AccountJid accountJid, UserJid userJid, int notificationId, CharSequence chatTitle) {
            this.accountJid = accountJid;
            this.userJid = userJid;
            this.notificationId = notificationId;
            this.chatTitle = chatTitle;
            this.id = UUID.randomUUID().toString();
        }

        public Chat(String id, AccountJid accountJid, UserJid userJid, int notificationId, CharSequence chatTitle) {
            this.id = id;
            this.accountJid = accountJid;
            this.userJid = userJid;
            this.notificationId = notificationId;
            this.chatTitle = chatTitle;
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

        public long getLastMessageTimestamp() {
            return messages.get(messages.size() - 1).getTimestamp();
        }

        public boolean equals(AccountJid account, UserJid user) {
            return this.accountJid.equals(account) && this.userJid.equals(user);
        }
    }

    private class Message {
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

    private class SortByLastMessage implements Comparator<Chat> {
        @Override
        public int compare(Chat chatA, Chat chatB) {
            return (int) (chatA.getLastMessageTimestamp() - chatB.getLastMessageTimestamp());
        }
    }
}
