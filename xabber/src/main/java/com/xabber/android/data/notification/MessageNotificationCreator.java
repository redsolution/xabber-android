package com.xabber.android.data.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;

import java.util.List;

public class MessageNotificationCreator {

    private static int UNIQUE_REQUEST_CODE = 0;
    private final Application application;
    private List<MessageNotification> messageNotifications;

    public MessageNotificationCreator() {
        application = Application.getInstance();
    }

    public android.app.Notification notifyMessageNotification(List<MessageNotification> messageNotifications,
                                                              MessageItem messageItem) {
        this.messageNotifications = messageNotifications;

        if (messageNotifications.isEmpty()) {
            return null;
        }

        int messageCount = 0;

        for (MessageNotification messageNotification : messageNotifications) {
            messageCount += messageNotification.getCount();
        }


        MessageNotification message = messageNotifications.get(messageNotifications.size() - 1);

        boolean showText = true;
        boolean isMUC = false;

        // muc
        if (MUCManager.getInstance().hasRoom(message.getAccount(), message.getUser().getJid().asEntityBareJidIfPossible())) {
            isMUC = true;
            showText = ChatManager.getInstance().isShowTextOnMuc(message.getAccount(), message.getUser());

        } else { // chat
            isMUC = false;
            showText = ChatManager.getInstance().isShowText(message.getAccount(), message.getUser());
        }

        // in-app notifications
        boolean isAppInForeground = isAppInForeground();
        if (isAppInForeground) {
            // disable message preview
            if (!SettingsManager.eventsInAppPreview()) showText = false;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(application);
        notificationBuilder.setContentTitle(getTitle(message, messageCount));
        notificationBuilder.setContentText(getText(message, showText));
        notificationBuilder.setSubText(message.getAccount().toString());

        notificationBuilder.setTicker(getText(message, showText));

        notificationBuilder.setSmallIcon(getSmallIcon());
        notificationBuilder.setLargeIcon(getLargeIcon(message));

        notificationBuilder.setWhen(message.getTimestamp().getTime());
        notificationBuilder.setColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(message.getAccount()));
        notificationBuilder.setStyle(getStyle(message, messageCount, showText));

        notificationBuilder.setContentIntent(getIntent(message));

        notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager.addEffects(notificationBuilder, messageItem, isMUC, checkVibrateMode(), isAppInForeground);

        return notificationBuilder.build();
    }

    private CharSequence getTitle(MessageNotification message, int messageCount) {
        if (isFromOneContact()) {
            return getSingleContactTitle(message, messageCount);
        } else {
            return getMultiContactTitle(messageCount);
        }
    }

    private CharSequence getSingleContactTitle(MessageNotification message, int messageCount) {
        if (messageCount > 1) {
            return application.getString(R.string.chat_messages_from_contact,
                    messageCount, getTextForMessages(messageCount), getContactName(message));
        } else {
            return getContactName(message);
        }
    }

    private String getContactName(MessageNotification message) {
        return RosterManager.getInstance().getBestContact(message.getAccount(), message.getUser()).getName();
    }

    private CharSequence getMultiContactTitle(int messageCount) {
        String messageText = getTextForMessages(messageCount);
        String contactText = StringUtils.getQuantityString(application.getResources(),
                R.array.chat_contact_quantity, messageNotifications.size());
        return application.getString(R.string.chat_status,
                messageCount, messageText, messageNotifications.size(), contactText);
    }

    private String getTextForMessages(int messageCount) {
        return StringUtils.getQuantityString(
                application.getResources(), R.array.chat_message_quantity, messageCount);
    }

    private CharSequence getText(MessageNotification message, boolean showText) {
        if (isFromOneContact()) {
            if (showText) {
                return message.getText();
            } else {
                return null;
            }
        } else {
            return getContactNameAndMessage(message, showText);
        }
    }

    private int getSmallIcon() {
        return R.drawable.ic_stat_chat;
    }

    private android.graphics.Bitmap getLargeIcon(MessageNotification message) {
        if (isFromOneContact()) {
            if (MUCManager.getInstance().hasRoom(message.getAccount(), message.getUser().getJid().asEntityBareJidIfPossible())) {
                return AvatarManager.getInstance().getRoomBitmap(message.getUser());
            } else {
                return AvatarManager.getInstance().getUserBitmap(message.getUser());
            }
        }
        return null;
    }

    private boolean isFromOneContact() {
        return messageNotifications.size() == 1;
    }

    private NotificationCompat.Style getStyle(MessageNotification message, int messageCount, boolean showText) {
        if (isFromOneContact()) {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

            bigTextStyle.setBigContentTitle(getSingleContactTitle(message, messageCount));
            if (showText) {
                bigTextStyle.bigText(message.getText());
            }
            bigTextStyle.setSummaryText(message.getAccount().toString());

            return bigTextStyle;
        } else {
            if (MUCManager.getInstance().hasRoom(message.getAccount(), message.getUser().getJid().asEntityBareJidIfPossible()))
                return getInboxStyle(messageCount, message.getAccount().toString(), true);
            else return getInboxStyle(messageCount, message.getAccount().toString(), false);
        }
    }

    private NotificationCompat.Style getInboxStyle(int messageCount, String accountName, boolean isMuc) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(getMultiContactTitle(messageCount));

        for (int i = 1; i <= messageNotifications.size(); i++) {
            MessageNotification messageNotification = messageNotifications.get(messageNotifications.size() - i);

            boolean showTextForThisContact
                    = ChatManager.getInstance().isShowText(messageNotification.getAccount(), messageNotification.getUser());
            if (isMuc)
                showTextForThisContact
                        = ChatManager.getInstance().isShowTextOnMuc(messageNotification.getAccount(), messageNotification.getUser());

            inboxStyle.addLine(getContactNameAndMessage(messageNotification, showTextForThisContact));
        }

        inboxStyle.setSummaryText(accountName);

        return inboxStyle;
    }

    private Spannable getContactNameAndMessage(MessageNotification messageNotification, boolean showText) {
        String userName = getContactName(messageNotification);

        Spannable spannableString;
        if (showText) {
            String contactAndMessage = application.getString(
                    R.string.chat_contact_and_message, userName, messageNotification.getText());
            spannableString = new SpannableString(contactAndMessage);

        } else {
            spannableString = new SpannableString(userName);
        }

        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, userName.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private PendingIntent getIntent(MessageNotification message) {
        Intent backIntent = ContactListActivity.createIntent(application);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = ChatActivity.createClearTopIntent(application, message.getAccount(), message.getUser());
        return PendingIntent.getActivities(application, UNIQUE_REQUEST_CODE++,
                new Intent[]{backIntent, intent}, PendingIntent.FLAG_ONE_SHOT);
    }

    private boolean checkVibrateMode() {
        AudioManager am = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        return am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = application.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
