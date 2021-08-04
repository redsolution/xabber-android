package com.xabber.android.data.notification;

import com.xabber.android.data.Application;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import java.util.ArrayList;
import java.util.List;

public class DelayedNotificationActionManager implements OnConnectedListener {

    private static DelayedNotificationActionManager instance;
    private final List<FullAction> delayedActions = new ArrayList<>();

    public static DelayedNotificationActionManager getInstance() {
        if (instance == null)
            instance = new DelayedNotificationActionManager();
        return instance;
    }

    @Override
    public void onConnected(ConnectionItem connection) {
        Application.getInstance().runOnUiThreadDelay(this::onLoaded, 3000);
    }

    private void onLoaded() {
        for (FullAction action : delayedActions) {
            MessageNotificationManager.INSTANCE.performAction(action);
        }
        delayedActions.clear();
    }

    public void addAction(FullAction action) {
        delayedActions.add(action);
    }

}
