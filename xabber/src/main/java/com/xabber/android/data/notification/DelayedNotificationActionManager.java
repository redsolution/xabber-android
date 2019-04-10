package com.xabber.android.data.notification;

import android.content.Intent;
import com.xabber.android.data.Application;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.receiver.NotificationReceiver;
import java.util.ArrayList;
import java.util.List;

public class DelayedNotificationActionManager implements OnConnectedListener {

    private static DelayedNotificationActionManager instance;
    private List<Intent> delayedNotificationActions = new ArrayList<>();

    public static DelayedNotificationActionManager getInstance() {
        if (instance == null)
            instance = new DelayedNotificationActionManager();
        return instance;
    }

    @Override
    public void onConnected(ConnectionItem connection) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onLoaded();
                    }
                });
            }
        });
    }

    private void onLoaded() {
        for (Intent intent : delayedNotificationActions) {
            NotificationReceiver.onNotificationAction(intent);
        }
        delayedNotificationActions.clear();
    }

    public void addAction(Intent intent) {
        if (!delayedNotificationActions.contains(intent))
            delayedNotificationActions.add(intent);
    }

}
