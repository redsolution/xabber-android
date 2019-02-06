package com.xabber.android.data.notification;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.Person;
import android.support.v4.app.RemoteInput;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.NotificationReceiver;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.preferences.NotificationChannelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NewMessageNotifCreator {

    private final static String MESSAGE_GROUP_ID = "MESSAGE_GROUP";
    private final static int MESSAGE_BUNDLE_NOTIFICATION_ID = 2;
    private static final int COLOR = 299031;
    private static final String DISPLAY_NAME = "You";

    private final Application context;
    private final NotificationManager notificationManager;

    public NewMessageNotifCreator(Application context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    public void createNotification(MessageNotificationManager.Chat chat, boolean alert) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationChannelUtils.getChannelID(
                        chat.isGroupChat() ? NotificationChannelUtils.ChannelType.groupChat
                                : NotificationChannelUtils.ChannelType.privateChat))
                .setColor(COLOR)
                .setSmallIcon(R.drawable.ic_message)
                .setLargeIcon(getLargeIcon(chat))
                .setGroup(MESSAGE_GROUP_ID)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setOnlyAlertOnce(!alert)
                .addAction(createReplyAction(chat.getNotificationId()))
                .addAction(createMarkAsReadAction(chat.getNotificationId()))
                .addAction(createMuteAction(chat.getNotificationId()))
                .setContentIntent(createContentIntent(chat))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getNotificationId()))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationCompat.Style messageStyle = new NotificationCompat.MessagingStyle(DISPLAY_NAME);
            for (MessageNotificationManager.Message message : chat.getMessages()) {

                Person person = new Person.Builder()
                        .setName(message.getAuthor())
                        .build();

                ((NotificationCompat.MessagingStyle) messageStyle).addMessage(
                        new NotificationCompat.MessagingStyle.Message(message.getMessageText(), message.getTimestamp(), person));
            }

            builder.setStyle(messageStyle);
        } else {
            int messageCount = chat.getMessages().size();
            CharSequence title;
            if (messageCount > 1)
                title = messageCount + " messages from " + chat.getChatTitle();
            else title = chat.getChatTitle();

            CharSequence content = chat.getLastMessage().getMessageText();

            builder.setContentTitle(title)
                    .setContentText(content)
                    .setStyle(createInboxStyle(chat))
                    .setAutoCancel(true);

            if (alert) addEffects(builder, content.toString(), chat.getAccountJid(), chat.getUserJid(),
                    chat.isGroupChat(), checkVibrateMode(), isAppInForeground());
        }

        sendNotification(builder, chat.getNotificationId());
    }

    public void createBundleNotification(List<MessageNotificationManager.Chat> chats, boolean alert) {

        boolean isGroup = false;
        MessageNotificationManager.Chat lastChat = getLastChat(chats);
        if (lastChat != null) isGroup = lastChat.isGroupChat();
        int messageCount = getMessageCount(chats);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context,
                        NotificationChannelUtils.getChannelID(
                                isGroup ? NotificationChannelUtils.ChannelType.groupChat
                                        : NotificationChannelUtils.ChannelType.privateChat))
                        .setColor(COLOR)
                        .setSmallIcon(R.drawable.ic_message)
                        .setSubText(messageCount + " new messages")
                        .setGroup(MESSAGE_GROUP_ID)
                        .setGroupSummary(true)
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                        .setContentIntent(createBundleContentIntent())
                        .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, MESSAGE_BUNDLE_NOTIFICATION_ID))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(messageCount + " new messages")
                    .setGroupSummary(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
        } else {
            if (lastChat != null) {
                MessageNotificationManager.Message lastMessage = lastChat.getLastMessage();

                if (lastMessage != null) {
                    CharSequence title = messageCount + " messages from " + chats.size() + " chats";
                    CharSequence content = createLine(lastMessage.getAuthor(), lastMessage.getMessageText());
                    builder.setContentTitle(title)
                            .setContentText(content)
                            .setOnlyAlertOnce(!alert)
                            .setStyle(createInboxStyleForBundle(chats));

                    if (alert) addEffects(builder, content.toString(), lastChat.getAccountJid(),
                            lastChat.getUserJid(), lastChat.isGroupChat(), checkVibrateMode(),
                            isAppInForeground());
                }
            }
        }

        sendNotification(builder, MESSAGE_BUNDLE_NOTIFICATION_ID);
    }

    /** UTILS */

    private int getMessageCount(List<MessageNotificationManager.Chat> chats) {
        int result = 0;
        for (MessageNotificationManager.Chat notification : chats) {
            result += notification.getMessages().size();
        }
        return result;
    }

    private MessageNotificationManager.Chat getLastChat(List<MessageNotificationManager.Chat> chats) {
        List<MessageNotificationManager.Chat> sortedChat = new ArrayList<>(chats);
        Collections.sort(sortedChat, Collections.reverseOrder(new SortByLastMessage()));
        if (sortedChat.size() > 0) {
            return sortedChat.get(0);
        } else return null;
    }

    private android.graphics.Bitmap getLargeIcon(MessageNotificationManager.Chat chat) {
        String name = RosterManager.getInstance().getName(chat.getAccountJid(), chat.getUserJid());
        if (MUCManager.getInstance().hasRoom(chat.getAccountJid(), chat.getUserJid().getJid().asEntityBareJidIfPossible())) {
            return AvatarManager.getInstance().getRoomBitmap(chat.getUserJid());
        } else {
            return AvatarManager.getInstance().getUserBitmap(chat.getUserJid(), name);
        }
    }

    private void sendNotification(NotificationCompat.Builder builder, int notificationId) {
        notificationManager.notify(notificationId, builder.build());
    }

    private NotificationCompat.Style createInboxStyle(MessageNotificationManager.Chat chat) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int startPos = chat.getMessages().size() <= 7 ? 0 : chat.getMessages().size() - 7;
        for (int i = startPos; i < chat.getMessages().size(); i++) {
            MessageNotificationManager.Message message = chat.getMessages().get(i);
            inboxStyle.addLine(message.getMessageText());

        }
        return inboxStyle;
    }

    private NotificationCompat.Style createInboxStyleForBundle(List<MessageNotificationManager.Chat> chats) {
        List<MessageNotificationManager.Chat> sortedChat = new ArrayList<>(chats);
        Collections.sort(sortedChat, Collections.reverseOrder(new SortByLastMessage()));
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int count = 0;
        for (MessageNotificationManager.Chat chat : sortedChat) {
            if (count >= 7) break;
            MessageNotificationManager.Message message = chat.getMessages().get(chat.getMessages().size() - 1);
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

    public static void addEffects(NotificationCompat.Builder notificationBuilder, String text,
                                  AccountJid account, UserJid user, boolean isMUC,
                                  boolean isPhoneInVibrateMode, boolean isAppInForeground) {

        if (account == null || user == null) return;

        if (MessageManager.getInstance().getChat(account, user).getFirstNotification()
                || !SettingsManager.eventsFirstOnly()) {
            Uri sound = PhraseManager.getInstance().getSound(account,
                    user, text, isMUC);
            boolean makeVibration = ChatManager.getInstance().isMakeVibro(account, user);

            boolean led;
            if (isMUC) led = SettingsManager.eventsLightningForMuc();
            else led = SettingsManager.eventsLightning();

            com.xabber.android.data.notification.NotificationManager.getInstance()
                    .setNotificationDefaults(notificationBuilder, led, sound, AudioManager.STREAM_NOTIFICATION);

            // vibration
            if (makeVibration)
                com.xabber.android.data.notification.NotificationManager
                        .setVibration(isMUC, isPhoneInVibrateMode, notificationBuilder);

            // in-app notifications
            if (isAppInForeground) {
                // disable vibrate
                if (!SettingsManager.eventsInAppVibrate()) {
                    notificationBuilder.setVibrate(new long[] {0, 0});
                }
                // disable sounds
                if (!SettingsManager.eventsInAppSounds()) {
                    notificationBuilder.setSound(null);
                }
            }
        }
    }

    private boolean checkVibrateMode() {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) return am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
        else return false;
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
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

    private PendingIntent createContentIntent(MessageNotificationManager.Chat chat) {
        Intent backIntent = ContactListActivity.createIntent(Application.getInstance());
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = ChatActivity.createClearTopIntent(Application.getInstance(), chat.getAccountJid(), chat.getUserJid());
        return PendingIntent.getActivities(Application.getInstance(), chat.getNotificationId(),
                new Intent[]{backIntent, intent}, PendingIntent.FLAG_ONE_SHOT);
    }

    private PendingIntent createBundleContentIntent() {
        return PendingIntent.getActivity(context, MESSAGE_BUNDLE_NOTIFICATION_ID,
                ContactListActivity.createCancelNotificationIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public class SortByLastMessage implements Comparator<MessageNotificationManager.Chat> {
        @Override
        public int compare(MessageNotificationManager.Chat chatA, MessageNotificationManager.Chat chatB) {
            return (int) (chatA.getLastMessageTimestamp() - chatB.getLastMessageTimestamp());
        }
    }
}
