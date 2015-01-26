package com.xabber.android.data.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ChatViewer;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

import java.util.List;

public class MessageNotificationCreator {

    private final Application application;

    private List<MessageNotification> messageNotifications;
    private NotificationCompat.Builder notificationBuilder;

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

        boolean showText  = ChatManager.getInstance().isShowText(message.getAccount(), message.getUser());

        notificationBuilder = new NotificationCompat.Builder(application);
        notificationBuilder.setContentTitle(getTitle(message, messageCount));
        notificationBuilder.setContentText(getText(message, showText));
        if (showText) {
            notificationBuilder.setTicker(message.getText());
        }
        notificationBuilder.setSmallIcon(getSmallIcon());
        notificationBuilder.setLargeIcon(getLargeIcon(message));

        notificationBuilder.setWhen(message.getTimestamp().getTime());
        notificationBuilder.setColor(NotificationManager.COLOR_MATERIAL_RED_500);
        notificationBuilder.setStyle(getStyle(message, messageCount));

        notificationBuilder.setContentIntent(getIntent(message));

        addEffects(messageItem);

        return notificationBuilder.build();
    }

    private CharSequence getTitle(MessageNotification message, int messageCount) {
        if (isFromOneContact()) {
            return RosterManager.getInstance().getName(message.getAccount(), message.getUser());
        } else {
            String messageText = StringUtils.getQuantityString(
                    application.getResources(), R.array.chat_message_quantity, messageCount);
            String contactText = StringUtils.getQuantityString(application.getResources(),
                    R.array.chat_contact_quantity, messageNotifications.size());
            return application.getString(R.string.chat_status,
                    messageCount, messageText, messageNotifications.size(), contactText);
        }
    }

    private CharSequence getText(MessageNotification message, boolean showText) {
        if (showText && isFromOneContact()) {
            return message.getText();
        } else {
            return "";
        }
    }

    private int getSmallIcon() {
        return R.drawable.ic_stat_chat;
    }

    private android.graphics.Bitmap getLargeIcon(MessageNotification message) {
        if (isFromOneContact()) {
            if (MUCManager.getInstance().hasRoom(message.getAccount(), message.getUser())) {
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

    private NotificationCompat.BigTextStyle getStyle(MessageNotification message, int messageCount) {
        if (messageCount == 1) {
            String title = RosterManager.getInstance().getName(message.getAccount(), message.getUser());

            String text = message.getText();

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(title);
            bigTextStyle.bigText(text);
            return bigTextStyle;
        }
        return null;
    }

    private PendingIntent getIntent(MessageNotification message) {
        Intent chatIntent
                = ChatViewer.createClearTopIntent(application, message.getAccount(), message.getUser());

        return PendingIntent.getActivity(application, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addEffects(MessageItem messageItem) {
        if (messageItem == null) {
            return;
        }
        if (messageItem.getChat().getFirstNotification() || !SettingsManager.eventsFirstOnly()) {
            Uri sound = PhraseManager.getInstance().getSound(messageItem.getChat().getAccount(),
                    messageItem.getChat().getUser(), messageItem.getText());
            boolean makeVibration = ChatManager.getInstance().isMakeVibro(messageItem.getChat().getAccount(),
                    messageItem.getChat().getUser());

            NotificationManager.getInstance().setNotificationDefaults(notificationBuilder,
                    makeVibration, sound, AudioManager.STREAM_NOTIFICATION);
        }
    }
}
