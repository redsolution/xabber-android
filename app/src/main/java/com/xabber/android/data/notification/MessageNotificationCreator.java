package com.xabber.android.data.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ChatViewer;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.utils.StringUtils;

import java.util.List;

public class MessageNotificationCreator {

    private final Application application;
    private final AccountPainter accountPainter;
    private List<MessageNotification> messageNotifications;
    private NotificationCompat.Builder notificationBuilder;

    public MessageNotificationCreator() {
        application = Application.getInstance();
        accountPainter = new AccountPainter(application);

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
        notificationBuilder.setSubText(message.getAccount());

        notificationBuilder.setTicker(getText(message, showText));

        notificationBuilder.setSmallIcon(getSmallIcon());
        notificationBuilder.setLargeIcon(getLargeIcon(message));

        notificationBuilder.setWhen(message.getTimestamp().getTime());
        notificationBuilder.setColor(accountPainter.getAccountMainColor(message.getAccount()));
        notificationBuilder.setStyle(getStyle(message, messageCount, showText));

        notificationBuilder.setContentIntent(getIntent(message));

        notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        addEffects(messageItem);

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
        return RosterManager.getInstance().getName(message.getAccount(), message.getUser());
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

    private NotificationCompat.Style getStyle(MessageNotification message, int messageCount, boolean showText) {
        if (isFromOneContact()) {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

            bigTextStyle.setBigContentTitle(getSingleContactTitle(message, messageCount));
            if (showText) {
                bigTextStyle.bigText(message.getText());
            }
            bigTextStyle.setSummaryText(message.getAccount());

            return bigTextStyle;
        } else {
            return getInboxStyle(messageCount, message.getAccount());
        }
    }

    private NotificationCompat.Style getInboxStyle(int messageCount, String accountName) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(getMultiContactTitle(messageCount));

        for (int i = 1; i <= messageNotifications.size(); i++) {
            MessageNotification messageNotification = messageNotifications.get(messageNotifications.size() - i);

            boolean showTextForThisContact
                    = ChatManager.getInstance().isShowText(messageNotification.getAccount(), messageNotification.getUser());

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
