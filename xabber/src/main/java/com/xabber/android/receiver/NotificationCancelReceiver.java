package com.xabber.android.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xabber.android.data.Application;
import com.xabber.android.data.notification.NotificationManager;

/**
 * Created by valery.miller on 05.03.18.
 */

public class NotificationCancelReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager.getInstance().onClearNotifications();
    }

    public static PendingIntent createPendingIntent() {
        Intent intent = new Intent(Application.getInstance().getApplicationContext(),
                NotificationCancelReceiver.class);

        return PendingIntent.getBroadcast(
                Application.getInstance().getApplicationContext(), 0, intent, 0);
    }
}
