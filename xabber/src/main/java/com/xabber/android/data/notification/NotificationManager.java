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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
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
import com.xabber.android.data.database.sqlite.NotificationTable;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.activity.ClearNotificationsActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;

import org.jxmpp.stringprep.XmppStringprepException;

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
        OnCloseListener, OnLoadListener, Runnable, OnAccountRemovedListener {

    public static final int PERSISTENT_NOTIFICATION_ID = 1;
    public static final int MESSAGE_NOTIFICATION_ID = 2;
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
    private final List<MessageNotification> messageNotifications;
    private NotificationCompat.Builder persistentNotificationBuilder;
    private MessageNotificationCreator messageNotificationCreator;
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


        handler = new Handler();
        providers = new ArrayList<>();
        messageNotifications = new ArrayList<>();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            persistentNotificationBuilder = new NotificationCompat.Builder(application, createNotificationChannelService());
        else persistentNotificationBuilder = new NotificationCompat.Builder(application, "");
        initPersistentNotification();


        messageNotificationCreator = new MessageNotificationCreator();

        persistentNotificationColor = application.getResources().getColor(R.color.persistent_notification_color);
    }

    public static void addEffects(NotificationCompat.Builder notificationBuilder,
                                  MessageItem messageItem, boolean isMUC, boolean isPhoneInVibrateMode, boolean isAppInForeground) {
        if (messageItem == null) {
            return;
        }
        if (MessageManager.getInstance().getChat(messageItem.getAccount(), messageItem.getUser()).getFirstNotification() || !SettingsManager.eventsFirstOnly()) {
            Uri sound = PhraseManager.getInstance().getSound(messageItem.getAccount(),
                    messageItem.getUser(), messageItem.getText(), isMUC);
            boolean makeVibration = ChatManager.getInstance().isMakeVibro(messageItem.getAccount(),
                    messageItem.getUser());

            boolean led;
            if (isMUC) led = SettingsManager.eventsLightningForMuc();
            else led = SettingsManager.eventsLightning();

            NotificationManager.getInstance().setNotificationDefaults(notificationBuilder, led, sound, AudioManager.STREAM_NOTIFICATION);

            // vibration
            if (makeVibration)
                setVibration(isMUC, isPhoneInVibrateMode, notificationBuilder);

            // in-app notifications
            if (isAppInForeground) {
                // disable vibrate
                if (!SettingsManager.eventsInAppVibrate()) {
                    notificationBuilder.setVibrate(new long[] {0, 0});
                }
                // disable sounds
                if (!SettingsManager.eventsInAppSounds()) {
                    notificationBuilder.setSound(null);
                }
            }
        }
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
                    try {
                        messageNotifications.add(new MessageNotification(
                        AccountJid.from(NotificationTable.getAccount(cursor)),
                        UserJid.from(NotificationTable.getUser(cursor)),
                        NotificationTable.getText(cursor),
                        NotificationTable.getTimeStamp(cursor),
                        NotificationTable.getCount(cursor)));
                    } catch (UserJid.UserJidCreateException | XmppStringprepException e) {
                        LogManager.exception(this, e);
                    }
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

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationBuilder = new NotificationCompat.Builder(application, createNotificationChannel());
        else notificationBuilder = new NotificationCompat.Builder(application, "");

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
        }
    }

    private MessageNotification getMessageNotification(AccountJid account, UserJid user) {
        for (MessageNotification messageNotification : messageNotifications) {
            if (messageNotification.equals(account, user)) {
                return messageNotification;
            }
        }
        return null;
    }

    public void onMessageNotification(MessageItem messageItem) {
        MessageNotification messageNotification = getMessageNotification(
                messageItem.getAccount(), messageItem.getUser());
        if (messageNotification == null) {
            messageNotification = new MessageNotification(
                    messageItem.getAccount(), messageItem.getUser(), null, null, 0);
        } else {
            messageNotifications.remove(messageNotification);
        }
        messageNotification.addMessage(messageItem.getText());
        messageNotifications.add(messageNotification);

        final AccountJid account = messageNotification.getAccount();
        final UserJid user = messageNotification.getUser();
        final String text = messageNotification.getText();
        final Date timestamp = messageNotification.getTimestamp();
        final int count = messageNotification.getCount();

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().write(account.toString(), user.toString(), text, timestamp, count);
            }
        });

        updateMessageNotification(messageItem);
    }

    /**
     * Updates message notification.
     */
    public void onMessageNotification() {
        updateMessageNotification(null);
    }

    public int getNotificationMessageCount(AccountJid account, UserJid user) {
        MessageNotification messageNotification = getMessageNotification(
                account, user);
        if (messageNotification == null)
            return 0;
        return messageNotification.getCount();
    }

    public void removeMessageNotification(final AccountJid account, final UserJid user) {
        MessageNotification messageNotification = getMessageNotification(account, user);
        if (messageNotification == null)
            return;
        messageNotifications.remove(messageNotification);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().remove(account.toString(), user.toString());
            }
        });

        updateMessageNotification(null);
    }

    public void removeMessageNotificationsForAccount(final AccountJid account) {
        Iterator<MessageNotification> iterator = messageNotifications.iterator();
        while(iterator.hasNext()) {
            MessageNotification messageNotification = iterator.next();

            if (messageNotification.getAccount().equals(account)) {
                iterator.remove();
            }
        }

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().remove(account);
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
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotificationTable.getInstance().clear();
            }
        });
        updateMessageNotification(null);
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
        updateMessageNotification(null);
    }

    public Notification getPersistentNotification() {
        return persistentNotificationBuilder.build();
    }

    @Override
    public void onClose() {
        notificationManager.cancelAll();
    }

    private static void setVibration(boolean isMUC, boolean isPhoneInVibrateMode, NotificationCompat.Builder notificationBuilder) {
        SettingsManager.VibroMode vibroMode;
        if (isMUC) vibroMode = SettingsManager.eventsVibroMuc();
        else vibroMode = SettingsManager.eventsVibroChat();

        switch (vibroMode) {
            case disabled:
                notificationBuilder.setVibrate(new long[] {0, 0});
                break;
            case defaultvibro:
                notificationBuilder.setVibrate(new long[] {0, 500});
                break;
            case shortvibro:
                notificationBuilder.setVibrate(new long[] {0, 250});
                break;
            case longvibro:
                notificationBuilder.setVibrate(new long[] {0, 1000});
                break;
            case onlyifsilent:
                if (isPhoneInVibrateMode)
                    notificationBuilder.setVibrate(new long[] {0, 500});
                break;
            default:
                notificationBuilder.setVibrate(new long[] {0, 500});
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannelService() {
        String channelId = "xabber_service";
        String channelName = "Xabber Background Service";
        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(channelId, channelName,
                        android.app.NotificationManager.IMPORTANCE_NONE);
        android.app.NotificationManager service = (android.app.NotificationManager)
                Application.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) service.createNotificationChannel(channel);
        return channelId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String createNotificationChannel() {
        String channelId = "xabber_notification";
        String channelName = "Xabber Notification";
        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(channelId, channelName,
                        android.app.NotificationManager.IMPORTANCE_HIGH);
        android.app.NotificationManager service = (android.app.NotificationManager)
                Application.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) service.createNotificationChannel(channel);
        return channelId;
    }
}
