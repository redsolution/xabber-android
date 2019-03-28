/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.activity.ClearNotificationsActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Manage notifications about message, subscription and authentication.
 *
 * @author alexander.ivanov
 */
public class NotificationManager implements OnInitializedListener, OnAccountChangedListener,
        OnCloseListener, OnLoadListener, Runnable, OnAccountRemovedListener {

    public static final int PERSISTENT_NOTIFICATION_ID = 1;
    private static final int BASE_NOTIFICATION_PROVIDER_ID = 0x10;

    private static final long VIBRATION_DURATION = 500;
    private static final String LOG_TAG = NotificationManager.class.getSimpleName();
    private static NotificationManager instance;

    private final Application application;
    private final android.app.NotificationManager notificationManager;
    private final PendingIntent clearNotifications;
    private final Handler handler;
    /**
     * Runnable to start vibration.
     */
    private final Runnable startVibration;

    /**
     * Runnable to force stop vibration.
     */
    private final Runnable stopVibration;

    /**
     * List of providers for notifications.
     */
    private final List<NotificationProvider<? extends NotificationItem>> providers;

    /**
     * List of
     */
    private NotificationCompat.Builder persistentNotificationBuilder;
    private int persistentNotificationColor;

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }

        return instance;
    }

    private NotificationManager() {
        this.application = Application.getInstance();

        notificationManager = (android.app.NotificationManager)
                application.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannelUtils.createPresistentConnectionChannel(notificationManager);
            NotificationChannelUtils.createEventsChannel(notificationManager);
        }

        handler = new Handler();
        providers = new ArrayList<>();
        clearNotifications = PendingIntent.getActivity(
                application, 0, ClearNotificationsActivity.createIntent(application), 0);

        stopVibration = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(startVibration);
                handler.removeCallbacks(stopVibration);
                ((Vibrator) NotificationManager.this.application.
                        getSystemService(Context.VIBRATOR_SERVICE)).cancel();
            }
        };

        startVibration = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(startVibration);
                handler.removeCallbacks(stopVibration);
                ((Vibrator) NotificationManager.this.application
                        .getSystemService(Context.VIBRATOR_SERVICE)).cancel();
                ((Vibrator) NotificationManager.this.application
                        .getSystemService(Context.VIBRATOR_SERVICE))
                        .vibrate(VIBRATION_DURATION);
                handler.postDelayed(stopVibration, VIBRATION_DURATION);
            }
        };

        persistentNotificationBuilder = new NotificationCompat.Builder(application,
                NotificationChannelUtils.PERSISTENT_CONNECTION_CHANNEL_ID);
        initPersistentNotification();
        persistentNotificationColor = application.getResources().getColor(R.color.persistent_notification_color);
    }

    private void initPersistentNotification() {
        persistentNotificationBuilder.setContentTitle(application.getString(R.string.application_title_full));
        persistentNotificationBuilder.setDeleteIntent(clearNotifications);
        persistentNotificationBuilder.setOngoing(true);
        persistentNotificationBuilder.setWhen(System.currentTimeMillis());
        persistentNotificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        persistentNotificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
        persistentNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

    }

    @Override
    public void onLoad() {
        MessageNotificationManager.getInstance().onLoad();
    }

    @Override
    public void onInitialized() {
        application.addUIListener(OnAccountChangedListener.class, this);
        //updateMessageNotification(null);
        updatePersistentNotification();
    }

    /**
     * Register new provider for notifications.
     *
     * @param provider
     */
    public void registerNotificationProvider(NotificationProvider<? extends NotificationItem> provider) {
        providers.add(provider);
    }

    /**
     * Update notifications for specified provider.
     *
     * @param <T>
     * @param provider
     * @param notify   Ticker to be shown. Can be <code>null</code>.
     */
    public <T extends NotificationItem> void updateNotifications(
            NotificationProvider<T> provider, T notify) {
        int id = providers.indexOf(provider);

        if (id == -1) {
            throw new IllegalStateException(
                    "registerNotificationProvider() must be called from onLoaded() method.");
        }

        id += BASE_NOTIFICATION_PROVIDER_ID;
        Iterator<? extends NotificationItem> iterator = provider.getNotifications().iterator();

        if (!iterator.hasNext()) {
            notificationManager.cancel(id);
            return;
        }

        NotificationItem top;
        String ticker;

        if (notify == null) {
            top = iterator.next();
            ticker = null;
        } else {
            top = notify;
            ticker = top.getTitle();
        }

        String channelID = provider.getChannelID();
        if (channelID.equals(NotificationChannelUtils.DEFAULT_ATTENTION_CHANNEL_ID))
            channelID = NotificationChannelUtils.getChannelID(NotificationChannelUtils.ChannelType.attention);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(application, channelID);

        notificationBuilder.setSmallIcon(provider.getIcon());
        notificationBuilder.setTicker(ticker);

        if (!provider.canClearNotifications()) {
            notificationBuilder.setOngoing(true);
        }

        notificationBuilder.setContentTitle(top.getTitle());
        notificationBuilder.setContentText(top.getText());

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(application);
        taskStackBuilder.addNextIntentWithParentStack(top.getIntent());

        notificationBuilder.setContentIntent(taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        if (ticker != null) {
            setNotificationDefaults(notificationBuilder, SettingsManager.eventsLightning(), provider.getSound(), provider.getStreamType());
        }

        notificationBuilder.setDeleteIntent(clearNotifications);

        notificationBuilder.setColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());

        notify(id, notificationBuilder.build());
    }

    /**
     * Sound, vibration and lightning flags.
     *
     * @param notificationBuilder
     * @param streamType
     */
    public void setNotificationDefaults(NotificationCompat.Builder notificationBuilder, boolean led, Uri sound, int streamType) {
        notificationBuilder.setSound(sound, streamType);
        notificationBuilder.setDefaults(0);

        int defaults = 0;

        if (led) {
            defaults |= Notification.DEFAULT_LIGHTS;
        }

        notificationBuilder.setDefaults(defaults);
    }

    public void startVibration() {
        handler.post(startVibration);
    }

    private void updatePersistentNotification() {
        if (!SettingsManager.eventsPersistent()) {
            return;
        }

        if (XabberService.getInstance() == null) return;

        // we do not want to show persistent notification if there are no enabled accounts
        XabberService.getInstance().changeForeground();
        Collection<AccountJid> accountList = AccountManager.getInstance().getEnabledAccounts();
        if (accountList.isEmpty()) {
            return;
        }

        int waiting = 0;
        int connecting = 0;
        int connected = 0;


        for (AccountJid account : accountList) {

            AccountItem accountItem = AccountManager.getInstance().getAccount(account);
            if (accountItem == null) {
                continue;
            }
            ConnectionState state = accountItem.getState();

            LogManager.i(this, "updatePersistentNotification account " + account + " state " + state );

            switch (state) {

                case offline:
                    break;
                case waiting:
                    waiting++;
                    break;

                case connecting:
                case registration:
                case authentication:
                    connecting++;
                    break;

                case connected:
                    connected++;
                    break;
            }
        }

        final Intent persistentIntent;

        persistentIntent = ContactListActivity.createPersistentIntent(application);

        if (connected > 0) {
            persistentNotificationBuilder.setColor(persistentNotificationColor);
            persistentNotificationBuilder.setSmallIcon(R.drawable.ic_stat_online);
        } else {
            persistentNotificationBuilder.setColor(NotificationCompat.COLOR_DEFAULT);
            persistentNotificationBuilder.setSmallIcon(R.drawable.ic_stat_offline);
        }

        persistentNotificationBuilder.setContentText(getConnectionState(waiting, connecting, connected, accountList.size()));
        persistentNotificationBuilder.setContentIntent(PendingIntent.getActivity(application, 0, persistentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT));

        notify(PERSISTENT_NOTIFICATION_ID, persistentNotificationBuilder.build());
    }

    private String getConnectionState(int waiting, int connecting, int connected, int accountCount) {

        String accountQuantity;
        String connectionState;
        if (connected > 0) {
            accountQuantity = StringUtils.getQuantityString(
                    application.getResources(), R.array.account_quantity, accountCount);

            String connectionFormat = StringUtils.getQuantityString(
                    application.getResources(), R.array.connection_state_connected, connected);

            connectionState = String.format(connectionFormat, connected, accountCount, accountQuantity);

        } else if (connecting > 0) {

            accountQuantity = StringUtils.getQuantityString(
                    application.getResources(), R.array.account_quantity, accountCount);

            String connectionFormat = StringUtils.getQuantityString(
                    application.getResources(), R.array.connection_state_connecting, connecting);

            connectionState = String.format(connectionFormat, connecting, accountCount, accountQuantity);

        } else if (waiting > 0 && application.isInitialized()) {

            accountQuantity = StringUtils.getQuantityString(
                    application.getResources(), R.array.account_quantity, accountCount);

            String connectionFormat = StringUtils.getQuantityString(
                    application.getResources(), R.array.connection_state_waiting, waiting);

            connectionState = String.format(connectionFormat, waiting, accountCount, accountQuantity);

        } else {
            accountQuantity = StringUtils.getQuantityString(
                    application.getResources(), R.array.account_quantity_offline, accountCount);
            connectionState = application.getString(
                    R.string.connection_state_offline, accountCount, accountQuantity);
        }
        return connectionState;
    }

    private void notify(int id, Notification notification) {
        String msg = "Notification: " + id
                + ", sound: " + notification.sound
                + ", vibro: " + (notification.defaults & Notification.DEFAULT_VIBRATE)
                + ", light: " + (notification.defaults & Notification.DEFAULT_LIGHTS);
        LogManager.i(this, msg + ", ticker: " + notification.tickerText, msg + ", ticker: ***");
        try {
            notificationManager.notify(id, notification);
        } catch (SecurityException e) {
            LogManager.exception(this, e);
            // If no access to ringtone - reset channel to default
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotificationChannelUtils.resetNotificationChannel(notificationManager, notification.getChannelId());
        }
    }

    public void onMessageNotification(MessageItem messageItem) {
        MessageNotificationManager.getInstance().onNewMessage(messageItem);
    }

    /**
     * Updates message notification.
     */
    public void onMessageNotification() {
        MessageNotificationManager.getInstance().rebuildAllNotifications();
    }

    public void removeMessageNotification(final AccountJid account, final UserJid user) {
        MessageNotificationManager.getInstance().removeChat(account, user);
    }

    public void removeMessageNotificationsForAccount(final AccountJid account) {
        MessageNotificationManager.getInstance().removeNotificationsForAccount(account);
    }

    /**
     * Called when notifications was cleared by user.
     */
    public void onClearNotifications() {
        for (NotificationProvider<? extends NotificationItem> provider : providers)
            if (provider.canClearNotifications())
                provider.clearNotifications();
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        handler.post(this);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        LogManager.i(LOG_TAG, "onAccountRemoved " + accountItem.getAccount());

        for (NotificationProvider<? extends NotificationItem> notificationProvider : providers) {
            if (notificationProvider instanceof AccountNotificationProvider) {
                ((AccountNotificationProvider) notificationProvider)
                        .clearAccountNotifications(accountItem.getAccount());
                updateNotifications(notificationProvider, null);
            }
        }

        removeMessageNotificationsForAccount(accountItem.getAccount());
    }

    @Override
    public void run() {
        handler.removeCallbacks(this);
        //updateMessageNotification(null);
        updatePersistentNotification();
    }

    public Notification getPersistentNotification() {
        return persistentNotificationBuilder.build();
    }

    @Override
    public void onClose() {
        notificationManager.cancelAll();
    }
}
