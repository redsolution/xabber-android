package com.xabber.android.data.notification;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.NotifyPrefs;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.NotificationReceiver;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageNotificationCreator {

    private final static String MESSAGE_GROUP_ID = "MESSAGE_GROUP";
    private final static int MESSAGE_BUNDLE_NOTIFICATION_ID = 2;

    private final Application context;
    private final NotificationManager notificationManager;
    private CharSequence messageHidden;

    public MessageNotificationCreator(Application context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.messageHidden = context.getString(R.string.message_hidden);
    }

    public void createNotification(MessageNotificationManager.Chat chat, boolean alert) {
        boolean inForeground = isAppInForeground(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getChannelID(chat))
                .setColor(context.getResources().getColor(R.color.persistent_notification_color))
                .setWhen(chat.getLastMessageTimestamp())
                .setSmallIcon(R.drawable.ic_stat_chat)
                .setLargeIcon(getLargeIcon(chat))
                .setGroup(MESSAGE_GROUP_ID)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setOnlyAlertOnce(!alert)
                .setContentIntent(createContentIntent(chat))
                .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, chat.getNotificationId()))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority((inForeground || inGracePeriod(chat)) ? NotificationCompat.PRIORITY_DEFAULT
                        : NotificationCompat.PRIORITY_HIGH);

        boolean showText = isNeedShowTextInNotification(chat);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.addAction(createReplyAction(chat.getNotificationId(), chat.getAccountJid()))
                    .setStyle(createMessageStyle(chat, showText));
        } else {
            builder.setContentTitle(createTitleSingleChat(chat.getMessages().size(), chat.getChatTitle()))
                    .setContentText(createMessageLine(chat.getLastMessage(), chat.isGroupChat(), showText))
                    .setStyle(createInboxStyle(chat, showText))
                    .setAutoCancel(true);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && alert && !inGracePeriod(chat))
            addEffects(builder, chat.getLastMessage().getMessageText().toString(), chat, context);

        builder.addAction(createMarkAsReadAction(chat.getNotificationId(), chat.getAccountJid()))
                .addAction(createMuteAction(chat.getNotificationId(), chat.getAccountJid()));
        sendNotification(builder, chat.getNotificationId());
    }

    public void createBundleNotification(List<MessageNotificationManager.Chat> chats, boolean alert) {
        boolean inForeground = isAppInForeground(context);
        List<MessageNotificationManager.Chat> sortedChats = new ArrayList<>(chats);
        Collections.sort(sortedChats, Collections.reverseOrder(new SortByLastMessage()));

        MessageNotificationManager.Chat lastChat = sortedChats.size() > 0 ? sortedChats.get(0) : null;
        int messageCount = getMessageCount(sortedChats);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, getChannelID(lastChat))
                        .setColor(context.getResources().getColor(R.color.persistent_notification_color))
                        .setWhen(lastChat != null ? lastChat.getLastMessageTimestamp() : System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_stat_chat)
                        .setContentIntent(createBundleContentIntent())
                        .setDeleteIntent(NotificationReceiver.createDeleteIntent(context, MESSAGE_BUNDLE_NOTIFICATION_ID))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority((inForeground || inGracePeriod(lastChat)) ? NotificationCompat.PRIORITY_DEFAULT
                                : NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(createNewMessagesTitle(messageCount))
                    .setGroup(MESSAGE_GROUP_ID)
                    .setGroupSummary(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);

        } else {
            builder.setContentTitle(createNewMessagesTitle(messageCount))
                    .setOnlyAlertOnce(!alert)
                    .setStyle(createInboxStyleForBundle(sortedChats))
                    .setContentText(createSummarizedContentForBundle(sortedChats));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && alert) {
            MessageNotificationManager.Message lastMessage = lastChat != null ? lastChat.getLastMessage() : null;
            if (lastMessage != null && alert && !inGracePeriod(lastChat))
                addEffects(builder, lastMessage.getMessageText().toString(), lastChat, context);
        }

        sendNotification(builder, MESSAGE_BUNDLE_NOTIFICATION_ID);
    }

    private String getChannelID(MessageNotificationManager.Chat chat) {
        if (inGracePeriod(chat))
            return NotificationChannelUtils.SILENT_CHANNEL_ID;

        NotifyPrefs customPrefs = null;
        boolean isGroup = false;
        if (chat != null) {
            isGroup = chat.isGroupChat();
            customPrefs = getCustomPrefs(chat);
        }
        return customPrefs != null ? customPrefs.getChannelID() : NotificationChannelUtils.getChannelID(
                isGroup ? NotificationChannelUtils.ChannelType.groupChat
                        : NotificationChannelUtils.ChannelType.privateChat);
    }

    private void sendNotification(NotificationCompat.Builder builder, int notificationId) {
        MessageNotificationManager.getInstance().setLastNotificationTime();
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            LogManager.exception(this, e);
            // If no access to ringtone - reset channel to default
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotificationChannelUtils.resetNotificationChannel(notificationManager, builder.getNotification().getChannelId());
        }
    }

    /** UTILS */
    private static boolean inGracePeriod(MessageNotificationManager.Chat chat) {
        if (!MessageNotificationManager.getInstance().isTimeToNewFullNotification()) return true;
        if (chat == null) return false;
        AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccountJid());
        if (accountItem != null) return accountItem.inGracePeriod();
        else return false;
    }

    private CharSequence createNewMessagesTitle(int messageCount) {
        return context.getString(R.string.new_chat_messages, messageCount,
                StringUtils.getQuantityString(context.getResources(), R.array.chat_message_quantity, messageCount));
    }

    private CharSequence createTitleSingleChat(int messageCount, CharSequence chatTitle) {
        if (messageCount == 1) return chatTitle;
        else return context.getString(R.string.new_chat_messages_from_contact, messageCount,
                StringUtils.getQuantityString(context.getResources(), R.array.chat_message_quantity, messageCount), chatTitle);
    }

    private NotificationCompat.Style createMessageStyle(MessageNotificationManager.Chat chat, boolean showText) {
        NotificationCompat.MessagingStyle messageStyle = new NotificationCompat.MessagingStyle(
                new Person.Builder().setName(context.getString(R.string.sender_is_you)).build());
        for (MessageNotificationManager.Message message : chat.getMessages()) {
            Person person = null;
            if (message.getAuthor() != null && message.getAuthor().length() > 0) {
                person = new Person.Builder()
                        .setName(message.getAuthor())
                        .setIcon(IconCompat.createWithBitmap(getLargeIcon(chat)))
                        .build();
            }
            messageStyle.addMessage(new NotificationCompat.MessagingStyle.Message(
                            showText ? message.getMessageText() : messageHidden,
                            message.getTimestamp(), person));
        }
        messageStyle.setConversationTitle(chat.getChatTitle());
        messageStyle.setGroupConversation(chat.isGroupChat());
        return messageStyle;
    }

    private CharSequence createSummarizedContentForBundle(List<MessageNotificationManager.Chat> sortedChats) {
        StringBuilder builder = new StringBuilder();
        CharSequence divider = ", ";
        for (MessageNotificationManager.Chat chat : sortedChats) {
            builder.append(chat.getChatTitle());
            builder.append(divider);
        }
        String result = builder.toString();
        return result.substring(0, result.length() - divider.length());
    }

    private boolean isNeedShowTextInNotification(MessageNotificationManager.Chat chat) {
        NotifyPrefs prefs = getCustomPrefs(chat);
        if (prefs != null) return prefs.isShowPreview();
        else return chat.isGroupChat() ?
                ChatManager.getInstance().isShowTextOnMuc(chat.getAccountJid(), chat.getUserJid())
                : ChatManager.getInstance().isShowText(chat.getAccountJid(), chat.getUserJid());
    }

    private int getMessageCount(List<MessageNotificationManager.Chat> chats) {
        int result = 0;
        for (MessageNotificationManager.Chat notification : chats) {
            result += notification.getMessages().size();
        }
        return result;
    }

    private android.graphics.Bitmap getLargeIcon(MessageNotificationManager.Chat chat) {
        String name = RosterManager.getInstance().getName(chat.getAccountJid(), chat.getUserJid());
        if (MUCManager.getInstance().hasRoom(chat.getAccountJid(), chat.getUserJid().getJid().asEntityBareJidIfPossible()))
            return AvatarManager.getInstance().getRoomBitmap(chat.getUserJid());
        else return AvatarManager.getInstance().getUserBitmap(chat.getUserJid(), name);
    }

    private NotificationCompat.Style createInboxStyle(MessageNotificationManager.Chat chat, boolean showText) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int startPos = chat.getMessages().size() <= 7 ? 0 : chat.getMessages().size() - 7;
        for (int i = startPos; i < chat.getMessages().size(); i++) {
            MessageNotificationManager.Message message = chat.getMessages().get(i);
            inboxStyle.addLine(createMessageLine(message, chat.isGroupChat(), showText));
        }
        return inboxStyle;
    }

    private NotificationCompat.Style createInboxStyleForBundle(List<MessageNotificationManager.Chat> sortedChats) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int count = 0;
        for (MessageNotificationManager.Chat chat : sortedChats) {
            if (count >= 7) break;
            inboxStyle.addLine(createChatLine(chat));
            count++;
        }
        return inboxStyle;
    }

    private String createMessageLine(MessageNotificationManager.Message message, boolean isGroupChat, boolean showText) {
        return (isGroupChat ? message.getAuthor() + ": " : "") + (showText ? message.getMessageText() : messageHidden);
    }

    private Spannable createChatLine(MessageNotificationManager.Chat chat) {
        boolean showText = isNeedShowTextInNotification(chat);
        CharSequence chatTitle = chat.getChatTitle();
        CharSequence author = chat.getLastMessage().getAuthor();
        CharSequence message = showText ? chat.getLastMessage().getMessageText() : messageHidden;
        String contactAndMessage = (chat.isGroupChat() ? chatTitle + ": " : "") + author + " " + message;
        Spannable spannable =  new SpannableString(contactAndMessage);
        spannable.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0,
                contactAndMessage.length() - message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private void addEffects(NotificationCompat.Builder notificationBuilder, String text,
                                  MessageNotificationManager.Chat notifChat, Context context) {

        AccountJid account = notifChat.getAccountJid();
        UserJid user = notifChat.getUserJid();
        boolean isMUC = notifChat.isGroupChat();

        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        if (chat != null && (chat.getFirstNotification() || !SettingsManager.eventsFirstOnly())) {

            Uri sound = getSound(notifChat, text, isMUC);
            boolean makeVibration = ChatManager.getInstance().isMakeVibro(account, user);
            boolean led = isMUC ? SettingsManager.eventsLightningForMuc() : SettingsManager.eventsLightning();

            com.xabber.android.data.notification.NotificationManager.getInstance()
                    .setNotificationDefaults(notificationBuilder, led, sound, AudioManager.STREAM_NOTIFICATION);

            // vibration
            if (makeVibration) setVibration(notifChat, isMUC, context, notificationBuilder);
        }
    }

    private Uri getSound(MessageNotificationManager.Chat chat, String text, boolean isMUC) {
        NotifyPrefs prefs = getCustomPrefs(chat);
        if (prefs != null) return Uri.parse(prefs.getSound());
        else {
            if (isMUC) return SettingsManager.eventsSoundMuc();
            return SettingsManager.eventsSound();
        }
    }

    public static void setVibration(MessageNotificationManager.Chat chat, boolean isMUC, Context context,
                                    NotificationCompat.Builder notificationBuilder) {
        NotifyPrefs prefs = getCustomPrefs(chat);
        if (prefs != null)
            notificationBuilder.setVibrate(getVibroValue(prefs.getVibro(), context));
        else notificationBuilder.setVibrate(getVibroValue(isMUC ? SettingsManager.eventsVibroMuc()
                : SettingsManager.eventsVibroChat(), context));
    }

    public static long[] getVibroValue(SettingsManager.VibroMode vibroMode, Context context) {
        switch (vibroMode) {
            case disabled:
                return new long[] {0, 0};
            case shortvibro:
                return new long[] {0, 250};
            case longvibro:
                return new long[] {0, 1000};
            case onlyifsilent:
                if (checkVibrateMode(context)) return new long[] {0, 500};
                else return new long[] {0, 0};
            default:
                return new long[] {0, 500};
        }
    }

    public static long[] getVibroValue(String vibroMode, Context context) {
        switch (vibroMode) {
            case "disable":
                return getVibroValue(SettingsManager.VibroMode.disabled, context);
            case "short":
                return getVibroValue(SettingsManager.VibroMode.shortvibro, context);
            case "long":
                return getVibroValue(SettingsManager.VibroMode.longvibro, context);
            case "if silent":
                return getVibroValue(SettingsManager.VibroMode.onlyifsilent, context);
            default:
                return getVibroValue(SettingsManager.VibroMode.defaultvibro, context);
        }
    }

    public static boolean checkVibrateMode(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) return am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
        else return false;
    }

    private boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager != null ? activityManager.getRunningAppProcesses() : null;
        if (appProcesses == null) return false;
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /** ACTIONS */

    private NotificationCompat.Action createReplyAction(int notificationId, AccountJid accountJid) {
        RemoteInput remoteInput = new RemoteInput.Builder(NotificationReceiver.KEY_REPLY_TEXT)
                .setLabel(context.getString(R.string.chat_input_hint))
                .build();

        return new NotificationCompat.Action.Builder(R.drawable.ic_message_forwarded_14dp,
                context.getString(R.string.action_reply), NotificationReceiver.createReplyIntent(context, notificationId, accountJid))
                .addRemoteInput(remoteInput)
                .build();
    }

    private NotificationCompat.Action createMarkAsReadAction(int notificationId, AccountJid accountJid) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_mark_as_read,
                context.getString(R.string.action_mark_as_read), NotificationReceiver.createMarkAsReadIntent(context, notificationId, accountJid))
                .build();
    }

    private NotificationCompat.Action createMuteAction(int notificationId, AccountJid accountJid) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_snooze,
                context.getString(R.string.action_snooze), NotificationReceiver.createMuteIntent(context, notificationId, accountJid))
                .build();
    }

    private PendingIntent createContentIntent(MessageNotificationManager.Chat chat) {
        Intent backIntent = ContactListActivity.createIntent(Application.getInstance());
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = ChatActivity.createClearTopIntent(Application.getInstance(), chat.getAccountJid(), chat.getUserJid());
        intent.putExtra(ChatActivity.EXTRA_NEED_SCROLL_TO_UNREAD, true);
        return PendingIntent.getActivities(Application.getInstance(), chat.getNotificationId(),
                new Intent[]{backIntent, intent}, PendingIntent.FLAG_ONE_SHOT);
    }

    private PendingIntent createBundleContentIntent() {
        return PendingIntent.getActivity(context, MESSAGE_BUNDLE_NOTIFICATION_ID,
                ContactListActivity.createClearStackIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static NotifyPrefs getCustomPrefs(MessageNotificationManager.Chat chat) {
        Collection<String> groups = RosterManager.getInstance().getGroups(chat.getAccountJid(), chat.getUserJid());
        Long phraseID = PhraseManager.getInstance().getPhraseID(chat.getAccountJid(), chat.getUserJid(),
                chat.getLastMessage().getMessageText().toString());
        return CustomNotifyPrefsManager.getInstance().getNotifyPrefsIfExist(chat.getAccountJid(),
                chat.getUserJid(), groups != null && groups.size() > 0 ? groups.iterator().next() : "", phraseID);
    }

    public class SortByLastMessage implements Comparator<MessageNotificationManager.Chat> {
        @Override
        public int compare(MessageNotificationManager.Chat chatA, MessageNotificationManager.Chat chatB) {
            return (int) (chatA.getLastMessageTimestamp() - chatB.getLastMessageTimestamp());
        }
    }
}
