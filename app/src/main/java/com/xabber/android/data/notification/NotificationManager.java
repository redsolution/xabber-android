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
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.account.OnAccountArchiveModeChangedListener;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ClearNotifications;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.ReconnectionActivity;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Manage notifications about message, subscription and authentication.
 *
 * @author alexander.ivanov
 */
public class NotificationManager implements OnInitializedListener, OnAccountChangedListener,
        OnCloseListener, OnLoadListener, Runnable, OnAccountRemovedListener,
        OnAccountArchiveModeChangedListener {

    public static final int PERSISTENT_NOTIFICATION_ID = 1;
    public static final int MESSAGE_NOTIFICATION_ID = 2;
    private static final int BASE_NOTIFICATION_PROVIDER_ID = 0x10;

    private static final long VIBRATION_DURATION = 500;
    private final static NotificationManager instance;

    static {
        instance = new NotificationManager();
        Application.getInstance().addManager(instance);
    }

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
    private final List<MessageNotification> messageNotifications;
    private final AccountPainter accountPainter;
    private NotificationCompat.Builder persistentNotificationBuilder;
    private MessageNotificationCreator messageNotificationCreator;
    private int persistentNotificationColor;

    private NotificationManager() {
        this.application = Application.getInstance();

        notificationManager = (android.app.NotificationManager)
                application.getSystemService(Context.NOTIFICATION_SERVICE);


        handler = new Handler();
        providers = new ArrayList<>();
        messageNotifications = new ArrayList<>();
        clearNotifications = PendingIntent.getActivity(
                application, 0, ClearNotifications.createIntent(application), 0);

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

        persistentNotificationBuilder = new NotificationCompat.Builder(application);
        initPersistentNotification();


        messageNotificationCreator = new MessageNotificationCreator();

        accountPainter = new AccountPainter(application);
        persistentNotificationColor = application.getResources().getColor(R.color.red_500);
    }

    public static NotificationManager getInstance() {
        return instance;
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
        final Collection<MessageNotification> messageNotifications = new ArrayList<>();
        Cursor cursor = NotificationTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    messageNotifications.add(new MessageNotification(
                            NotificationTable.getAccount(cursor),
                            NotificationTable.getUser(cursor),
                            NotificationTable.getText(cursor),
                            NotificationTable.getTimeStamp(cursor),
                            NotificationTable.getCount(cursor)));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(messageNotifications);
            }
        });
    }

    private void onLoaded(Collection<MessageNotification> messageNotifications) {
        this.messageNotifications.addAll(messageNotifications);
        for (MessageNotification messageNotification : messageNotifications) {
            MessageManager.getInstance().openChat(
                    messageNotification.getAccount(),
                    messageNotification.getUser());
        }
    }

    @Override
    public void onInitialized() {
        application.addUIListener(OnAccountChangedListener.class, this);
        updateMessageNotification(null);
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

        Intent intent = top.getIntent();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(application);

        notificationBuilder.setSmallIcon(provider.getIcon());
        notificationBuilder.setTicker(ticker);

        if (!provider.canClearNotifications()) {
            notificationBuilder.setOngoing(true);
        }

        notificationBuilder.setContentTitle(top.getTitle());
        notificationBuilder.setContentText(top.getText());

        notificationBuilder.setContentIntent(PendingIntent.getActivity(application, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT));

        if (ticker != null) {
            setNotificationDefaults(notificationBuilder, SettingsManager.eventsVibro(), provider.getSound(), provider.getStreamType());
        }

        notificationBuilder.setDeleteIntent(clearNotifications);

        notificationBuilder.setColor(accountPainter.getDefaultMainColor());

        notify(id, notificationBuilder.build());
    }

    /**
     * Sound, vibration and lightning flags.
     *
     * @param notificationBuilder
     * @param streamType
     */
    public void setNotificationDefaults(NotificationCompat.Builder notificationBuilder, boolean vibration, Uri sound, int streamType) {
        notificationBuilder.setSound(sound, streamType);
        notificationBuilder.setDefaults(0);

        int defaults = 0;

        if (vibration) {
            if (SettingsManager.eventsIgnoreSystemVibro()) {
                startVibration();
            } else {
                defaults |= Notification.DEFAULT_VIBRATE;

            }
        }

        if (SettingsManager.eventsLightning()) {
            defaults |= Notification.DEFAULT_LIGHTS;
        }

        notificationBuilder.setDefaults(defaults);
    }

    public void startVibration() {
        handler.post(startVibration);
    }

    /**
     * Chat was changed:
     * <ul>
     * <li>incoming message</li>
     * <li>chat was opened</li>
     * <li>account was changed</li>
     * </ul>
     * <p/>
     * Update chat and persistent notifications.
     *
     * @param ticker message to be shown.
     * @return
     */
    private void updateMessageNotification(MessageItem ticker) {
        updatePersistentNotification();

        Notification messageNotification = messageNotificationCreator.notifyMessageNotification(messageNotifications, ticker);

        if (messageNotification != null) {
            notify(MESSAGE_NOTIFICATION_ID, messageNotification);
        } else {
            notificationManager.cancel(MESSAGE_NOTIFICATION_ID);
        }
    }

    private void updatePersistentNotification() {
        int waiting = 0;
        int connecting = 0;
        int connected = 0;

        Collection<String> accountList = AccountManager.getInstance().getAccounts();
        for (String account : accountList) {
            ConnectionState state = AccountManager.getInstance().getAccount(account).getState();

            if (RosterManager.getInstance().isRosterReceived(account)) {
                connected++;
            } else if (state == ConnectionState.connecting || state == ConnectionState.authentication) {
                connecting++;
            } else if (state == ConnectionState.waiting) {
                waiting++;
            }
        }

        final Intent persistentIntent;

        if (waiting > 0 && application.isInitialized()) {
            persistentIntent = ReconnectionActivity.createIntent(application);
        } else {
            persistentIntent = ContactList.createPersistentIntent(application);
        }

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
        LogManager.i(this, "Notification: " + id
                + ", ticker: " + notification.tickerText
                + ", sound: " + notification.sound
                + ", vibro: " + (notification.defaults & Notification.DEFAULT_VIBRATE)
                + ", light: " + (notification.defaults & Notification.DEFAULT_LIGHTS));
        try {
            notificationManager.notify(id, notification);
        } catch (SecurityException e) {
            LogManager.exception(this, e);
        }
    }

    private MessageNotification getMessageNotification(String account, String user) {
        for (MessageNotification messageNotification : messageNotifications) {
            if (messageNotification.equals(account, user)) {
                return messageNotification;
            }
        }
        return null;
    }

    /**
     * Shows ticker with the message and updates message notification.
     *
     * @param messageItem
     * @param addNotification Whether notification should be stored.
     */
    public void onMessageNotification(MessageItem messageItem, boolean addNotification) {
        if (addNotification) {
            MessageNotification messageNotification = getMessageNotification(
                    messageItem.getChat().getAccount(), messageItem.getChat().getUser());
            if (messageNotification == null) {
                messageNotification = new MessageNotification(
                        messageItem.getChat().getAccount(), messageItem.getChat().getUser(), null, null, 0);
            } else {
                messageNotifications.remove(messageNotification);
            }
            messageNotification.addMessage(messageItem.getText());
            messageNotifications.add(messageNotification);

            final String account = messageNotification.getAccount();
            final String user = messageNotification.getUser();
            final String text = messageNotification.getText();
            final Date timestamp = messageNotification.getTimestamp();
            final int count = messageNotification.getCount();

            if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.dontStore) {
                Application.getInstance().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        NotificationTable.getInstance().write(account, user, text, timestamp, count);
                    }
                });
            }
        }
        updateMessageNotification(messageItem);
    }

    /**
     * Updates message notification.
     */
    public void onMessageNotification() {
        updateMessageNotification(null);
    }

    public int getNotificationMessageCount(String account, String user) {
        MessageNotification messageNotification = getMessageNotification(
                account, user);
        if (messageNotification == null)
            return 0;
        return messageNotification.getCount();
    }

    public void removeMessageNotification(final String account, final String user) {
        MessageNotification messageNotification = getMessageNotification(account, user);
        if (messageNotification == null)
            return;
        messageNotifications.remove(messageNotification);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().remove(account, user);
            }
        });
        updateMessageNotification(null);
    }

    /**
     * Called when notifications was cleared by user.
     */
    public void onClearNotifications() {
        for (NotificationProvider<? extends NotificationItem> provider : providers)
            if (provider.canClearNotifications())
                provider.clearNotifications();
        messageNotifications.clear();
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().clear();
            }
        });
        updateMessageNotification(null);
    }

    @Override
    public void onAccountArchiveModeChanged(AccountItem accountItem) {
        final String account = accountItem.getAccount();
        if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.dontStore)
            return;
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().removeAccount(account);
            }
        });
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        handler.post(this);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        for (NotificationProvider<? extends NotificationItem> notificationProvider : providers) {
            if (notificationProvider instanceof AccountNotificationProvider) {
                ((AccountNotificationProvider) notificationProvider)
                        .clearAccountNotifications(accountItem.getAccount());
                updateNotifications(notificationProvider, null);
            }
        }
    }

    @Override
    public void run() {
        handler.removeCallbacks(this);
        updateMessageNotification(null);
    }

    public Notification getPersistentNotification() {
        return persistentNotificationBuilder.build();
    }

    @Override
    public void onClose() {
        notificationManager.cancelAll();
    }
}
