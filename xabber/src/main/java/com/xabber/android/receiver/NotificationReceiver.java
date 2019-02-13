package com.xabber.android.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.xabber.android.data.notification.MessageNotificationManager;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";
    public static final String KEY_REPLY_TEXT = "KEY_REPLY_TEXT";

    private static final String ACTION_CANCEL = "ACTION_CANCEL";
    private static final String ACTION_REPLY = "ACTION_REPLY";
    private static final String ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ";
    private static final String ACTION_MUTE = "ACTION_MUTE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 1);

        if (action == null) return;

        switch (action) {
            case ACTION_MUTE:
                MessageNotificationManager.getInstance().onNotificationMuted(notificationId);
                break;
            case ACTION_CANCEL:
                MessageNotificationManager.getInstance().onNotificationCanceled(notificationId);
                break;
            case ACTION_REPLY:
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null)
                    MessageNotificationManager.getInstance().onNotificationReplied(notificationId,
                            remoteInput.getCharSequence(KEY_REPLY_TEXT));
                break;
            case ACTION_MARK_AS_READ:
                MessageNotificationManager.getInstance().onNotificationMarkedAsRead(notificationId);
                break;
        }
    }

    public static PendingIntent createReplyIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_REPLY);
        intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, notificationId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createMarkAsReadIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_MARK_AS_READ);
        intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, notificationId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createMuteIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_MUTE);
        intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, notificationId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createDeleteIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_CANCEL);
        intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, notificationId,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
