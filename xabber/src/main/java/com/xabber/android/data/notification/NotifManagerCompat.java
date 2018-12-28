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
import android.support.v4.app.TaskStackBuilder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.receiver.NotificationReceiver;
import com.xabber.android.ui.activity.ContactListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotifManagerCompat {

    private final static String MESSAGE_CHANNEL_ID = "MESSAGE_CHANNEL";
    private final static String MESSAGE_GROUP_ID = "MESSAGE_GROUP";
    private final static int MESSAGE_GROUP_NOTIFICATION_ID = 0;
    private static final int COLOR = 299031;
    private static final String DISPLAY_NAME = "You";

    private final Application context;
    private final NotificationManager notificationManager;
    private static NotifManagerCompat instance;
    private Map<Integer, Chat> chats = new HashMap<>();
    private Message lastMessage = null;

    public NotifManagerCompat() {
        context = Application.getInstance();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();
    }

    public static NotifManagerCompat getInstance() {
        if (instance == null) instance = new NotifManagerCompat();
        return instance;
    }

    /** LISTENER */

    public void onNotificationClick(int notificationId) {
        // if (notificationId == MESSAGE_GROUP_NOTIFICATION_ID)
            // Chat lastChat = chats.get(chats.size() - 1);
            // if (lastChat != null) openChat(lastChat.getChatId());
    }

    public void onNotificationReplied(int notificationId, CharSequence replyText) {
        // send message to xmpp

        // update notification
        addMessage(notificationId, "", DISPLAY_NAME, replyText);
    }

    public void onNotificationCanceled(int notificationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            chats.remove(notificationId);

            if (chats.size() > 1) createGroupNotification();
            else {
                notificationManager.cancel(MESSAGE_GROUP_NOTIFICATION_ID);
                if (chats.size() == 1) {
                    Map.Entry<Integer, Chat> entry = chats.entrySet().iterator().next();
                    createChatNotification(entry.getValue());
                }
            }

        } else chats.clear();
    }

    public void onNotificationMarkedAsRead(int notificationId) {
        // mute chat
        Log.d("NOTIFICATION_TEST", "mark as read " + notificationId);

        // cancel notification
        notificationManager.cancel(notificationId);
        onNotificationCanceled(notificationId);
    }

    /** MAIN METHODS */

    public void addMessage(int chatId, CharSequence chatTitle, CharSequence author, CharSequence messageText) {
        Chat chat = chats.get(chatId);
        if (chat == null) {
            chat = new Chat(chatId, chatTitle);
            chats.put(chatId, chat);
        }
        lastMessage = new Message(author, messageText, System.currentTimeMillis());
        chat.addMessage(lastMessage);

        updateNotifications(chat);
    }

    private void updateNotifications(Chat newChat) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createChatNotification(newChat);
            if (chats.size() > 1) createGroupNotification();
        } else {
            if (chats.size() > 1) {
                notificationManager.cancelAll();
                createGroupNotificationOldAPI();
            } else createChatNotificationOldAPI(newChat);
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
                .setLargeIcon(drawableToBitmap(context.getDrawable(R.mipmap.ic_launcher_round)))
                .setStyle(messageStyle)
                .setGroup(MESSAGE_GROUP_ID)
                .addAction(createReplyAction(chat.getChatId()))
                .addAction(createMarkAsReadAction(chat.getChatId()))
                .addAction(createMuteAction(chat.getChatId()))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getChatId()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, chat.getChatId());
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
                .addAction(createMarkAsReadAction(chat.getChatId()))
                .addAction(createMuteAction(chat.getChatId()))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getChatId()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        sendNotification(builder, chat.getChatId());
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

    /** UTILS */

    private void sendNotification(NotificationCompat.Builder builder, int chatId) {
        Intent resultIntent = new Intent(context, ContactListActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ContactListActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(chatId,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        notificationManager.notify(chatId, builder.build());
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
        ArrayList<Chat> chats = new ArrayList<>(this.chats.values());
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

    private int getMessageCount() {
        int result = 0;
        for (Map.Entry<Integer, Chat> entry : chats.entrySet()) {
            result += entry.getValue().getMessages().size();
        }
        return result;
    }

    private static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /** INTERNAL CLASSES */

    private class Chat {
        private int chatId;
        private CharSequence chatTitle;
        private List<Message> messages = new ArrayList<>();

        public Chat(int chatId, CharSequence chatTitle) {
            this.chatId = chatId;
            this.chatTitle = chatTitle;
        }

        public void addMessage(Message message) {
            messages.add(message);
        }

        public int getChatId() {
            return chatId;
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
    }

    private class Message {
        private CharSequence author;
        private CharSequence messageText;
        private long timestamp;

        public Message(CharSequence author, CharSequence messageText, long timestamp) {
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
    }

    private class SortByLastMessage implements Comparator<Chat> {
        @Override
        public int compare(Chat chatA, Chat chatB) {
            return (int) (chatA.getLastMessageTimestamp() - chatB.getLastMessageTimestamp());
        }
    }
}
