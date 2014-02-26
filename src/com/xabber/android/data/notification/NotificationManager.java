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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.RemoteViews;

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
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ChatViewer;
import com.xabber.android.ui.ClearNotifications;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.ReconnectionActivity;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

/**
 * Manage notifications about message, subscription and authentication.
 * 
 * @author alexander.ivanov
 */
public class NotificationManager implements OnInitializedListener,
		OnAccountChangedListener, OnCloseListener, OnLoadListener, Runnable,
		OnAccountRemovedListener, OnAccountArchiveModeChangedListener {

	public static final int PERSISTENT_NOTIFICATION_ID = 1;
	private static final int CHAT_NOTIFICATION_ID = 2;
	private static final int BASE_NOTIFICATION_PROVIDER_ID = 0x10;

	private static final long VIBRATION_DURATION = 500;
	private static final int MAX_NOTIFICATION_TEXT = 80;
	private final long startTime;
	private final Application application;
	private final android.app.NotificationManager notificationManager;
	private final Notification persistentNotification;
	private final PendingIntent clearNotifications;
	private final Handler handler;

	/**
	 * Runnable to start vibration.
	 */
	private final Runnable startVibro;

	/**
	 * Runnable to force stop vibration.
	 */
	private final Runnable stopVibro;

	/**
	 * List of providers for notifications.
	 */
	private final List<NotificationProvider<? extends NotificationItem>> providers;

	/**
	 * List of
	 */
	private final List<MessageNotification> messageNotifications;

	private final static NotificationManager instance;

	static {
		instance = new NotificationManager();
		Application.getInstance().addManager(instance);
	}

	public static NotificationManager getInstance() {
		return instance;
	}

	private NotificationManager() {
		this.application = Application.getInstance();
		notificationManager = (android.app.NotificationManager) application
				.getSystemService(Context.NOTIFICATION_SERVICE);
		persistentNotification = new Notification();
		handler = new Handler();
		providers = new ArrayList<NotificationProvider<? extends NotificationItem>>();
		messageNotifications = new ArrayList<MessageNotification>();
		startTime = System.currentTimeMillis();
		clearNotifications = PendingIntent.getActivity(application, 0,
				ClearNotifications.createIntent(application), 0);
		stopVibro = new Runnable() {
			@Override
			public void run() {
				handler.removeCallbacks(startVibro);
				handler.removeCallbacks(stopVibro);
				((Vibrator) NotificationManager.this.application
						.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
			}
		};
		startVibro = new Runnable() {
			@Override
			public void run() {
				handler.removeCallbacks(startVibro);
				handler.removeCallbacks(stopVibro);
				((Vibrator) NotificationManager.this.application
						.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
				((Vibrator) NotificationManager.this.application
						.getSystemService(Context.VIBRATOR_SERVICE))
						.vibrate(VIBRATION_DURATION);
				handler.postDelayed(stopVibro, VIBRATION_DURATION);
			}
		};
	}

	@Override
	public void onLoad() {
		final Collection<MessageNotification> messageNotifications = new ArrayList<MessageNotification>();
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
		for (MessageNotification messageNotification : messageNotifications)
			MessageManager.getInstance().openChat(
					messageNotification.getAccount(),
					messageNotification.getUser());
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
	public void registerNotificationProvider(
			NotificationProvider<? extends NotificationItem> provider) {
		providers.add(provider);
	}

	/**
	 * Update notifications for specified provider.
	 * 
	 * @param <T>
	 * @param provider
	 * @param notify
	 *            Ticker to be shown. Can be <code>null</code>.
	 */
	public <T extends NotificationItem> void updateNotifications(
			NotificationProvider<T> provider, T notify) {
		int id = providers.indexOf(provider);
		if (id == -1)
			throw new IllegalStateException(
					"registerNotificationProvider() must be called from onLoaded() method.");
		else
			id += BASE_NOTIFICATION_PROVIDER_ID;
		Iterator<? extends NotificationItem> iterator = provider
				.getNotifications().iterator();
		if (!iterator.hasNext()) {
			notificationManager.cancel(id);
		} else {
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
			Notification notification = new Notification(provider.getIcon(),
					ticker, System.currentTimeMillis());
			if (!provider.canClearNotifications())
				notification.flags |= Notification.FLAG_NO_CLEAR;
			notification.setLatestEventInfo(application, top.getTitle(), top
					.getText(), PendingIntent.getActivity(application, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT));
			if (ticker != null)
				setNotificationDefaults(notification,
						SettingsManager.eventsVibro(), provider.getSound(),
						provider.getStreamType());
			notification.deleteIntent = clearNotifications;
			notify(id, notification);
		}
	}

	/**
	 * Sound, vibration and lightning flags.
	 * 
	 * @param notification
	 * @param streamType
	 */
	private void setNotificationDefaults(Notification notification,
			boolean vibro, Uri sound, int streamType) {
		notification.audioStreamType = streamType;
		notification.defaults = 0;
		notification.sound = sound;
		if (vibro) {
			if (SettingsManager.eventsIgnoreSystemVibro())
				handler.post(startVibro);
			else
				notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		if (SettingsManager.eventsLightning()) {
			notification.defaults |= Notification.DEFAULT_LIGHTS;
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
	}

	/**
	 * Chat was changed:
	 * <ul>
	 * <li>incoming message</li>
	 * <li>chat was opened</li>
	 * <li>account was changed</li>
	 * </ul>
	 * 
	 * Update chat and persistent notifications.
	 * 
	 * @param ticker
	 *            message to be shown.
	 * @return
	 */
	private void updateMessageNotification(MessageItem ticker) {
		Collection<String> accountList = AccountManager.getInstance()
				.getAccounts();
		int accountCount = accountList.size();
		boolean started = application.isInitialized();
		int waiting = 0;
		int connecting = 0;
		int connected = 0;
		for (String account : accountList) {
			ConnectionState state = AccountManager.getInstance()
					.getAccount(account).getState();
			if (RosterManager.getInstance().isRosterReceived(account))
				connected++;
			else if (state == ConnectionState.connecting
					|| state == ConnectionState.authentication)
				connecting++;
			else if (state == ConnectionState.waiting)
				waiting++;
		}

		String accountQuantity;
		String connectionState;
		if (connected > 0) {
			accountQuantity = StringUtils.getQuantityString(
					application.getResources(), R.array.account_quantity,
					accountCount);
			String connectionFormat = StringUtils.getQuantityString(
					application.getResources(),
					R.array.connection_state_connected, connected);
			connectionState = String.format(connectionFormat, connected,
					accountCount, accountQuantity);
		} else if (connecting > 0) {
			accountQuantity = StringUtils.getQuantityString(
					application.getResources(), R.array.account_quantity,
					accountCount);
			String connectionFormat = StringUtils.getQuantityString(
					application.getResources(),
					R.array.connection_state_connecting, connecting);
			connectionState = String.format(connectionFormat, connecting,
					accountCount, accountQuantity);
		} else if (waiting > 0 && started) {
			accountQuantity = StringUtils.getQuantityString(
					application.getResources(), R.array.account_quantity,
					accountCount);
			String connectionFormat = StringUtils.getQuantityString(
					application.getResources(),
					R.array.connection_state_waiting, waiting);
			connectionState = String.format(connectionFormat, waiting,
					accountCount, accountQuantity);
		} else {
			accountQuantity = StringUtils.getQuantityString(
					application.getResources(),
					R.array.account_quantity_offline, accountCount);
			connectionState = application.getString(
					R.string.connection_state_offline, accountCount,
					accountQuantity);
		}

		final Intent persistentIntent;
		if (waiting > 0 && started)
			persistentIntent = ReconnectionActivity.createIntent(application);
		else
			persistentIntent = ContactList.createPersistentIntent(application);

		if (messageNotifications.isEmpty()) {
			notificationManager.cancel(CHAT_NOTIFICATION_ID);
		} else {
			int messageCount = 0;
			for (MessageNotification messageNotification : messageNotifications)
				messageCount += messageNotification.getCount();
			MessageNotification message = messageNotifications
					.get(messageNotifications.size() - 1);

			RemoteViews chatViews = new RemoteViews(
					application.getPackageName(), R.layout.chat_notification);

			Intent chatIntent = ChatViewer.createClearTopIntent(application,
					message.getAccount(), message.getUser());
			if (MUCManager.getInstance().hasRoom(message.getAccount(),
					message.getUser()))
				chatViews.setImageViewBitmap(R.id.icon, AvatarManager
						.getInstance().getRoomBitmap(message.getUser()));
			else
				chatViews.setImageViewBitmap(R.id.icon, AvatarManager
						.getInstance().getUserBitmap(message.getUser()));
			chatViews.setTextViewText(R.id.title, RosterManager.getInstance()
					.getName(message.getAccount(), message.getUser()));
			String text;
			if (ChatManager.getInstance().isShowText(message.getAccount(),
					message.getUser()))
				text = trimText(message.getText());
			else
				text = "";
			chatViews.setTextViewText(R.id.text2,
					Emoticons.getSmiledText(application, text));
			chatViews.setTextViewText(R.id.time,
					StringUtils.getSmartTimeText(message.getTimestamp()));

			String messageText = StringUtils.getQuantityString(
					application.getResources(), R.array.chat_message_quantity,
					messageCount);
			String contactText = StringUtils.getQuantityString(
					application.getResources(), R.array.chat_contact_quantity,
					messageNotifications.size());
			String status = application.getString(R.string.chat_status,
					messageCount, messageText, messageNotifications.size(),
					contactText);
			chatViews.setTextViewText(R.id.text, status);

			Notification notification = new Notification();
			if (Build.VERSION.SDK_INT >= 14 && SettingsManager.eventsPersistent()) {
				// Ongoing icons are in the left side, so hide this one.
				notification.icon = R.drawable.ic_placeholder;
				notification.when = Long.MIN_VALUE;
			} else {
				// Ongoing icons are in the right side, so show this one.
				updateNotification(notification, ticker);
				notification.icon = connected > 0 ? R.drawable.ic_stat_message
						: R.drawable.ic_stat_message_offline;
				notification.when = System.currentTimeMillis();
			}
			notification.contentView = chatViews;
			notification.contentIntent = PendingIntent.getActivity(application,
					0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.deleteIntent = clearNotifications;

			try {
				notify(CHAT_NOTIFICATION_ID, notification);
			} catch (RuntimeException e) {
				LogManager.exception(this, e);
				// Try to remove avatar in order to avoid
				// OutOfMemoryError on the Android system side.
				chatViews.setImageViewResource(R.id.icon,
						R.drawable.ic_placeholder);
				notify(CHAT_NOTIFICATION_ID, notification);
			}
		}

		persistentNotification.icon = R.drawable.ic_stat_normal;
		persistentNotification.setLatestEventInfo(application, application
				.getString(R.string.application_name), connectionState,
				PendingIntent.getActivity(application, 0, persistentIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));
		persistentNotification.flags = Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_NO_CLEAR;
		persistentNotification.defaults = 0;
		persistentNotification.sound = null;
		persistentNotification.tickerText = null;
		if (Build.VERSION.SDK_INT >= 14 && SettingsManager.eventsPersistent()) {
			// Ongoing icons are in the left side, so always use it.
			persistentNotification.when = startTime;
			if (messageNotifications.isEmpty()) {
				persistentNotification.icon = connected > 0 ? R.drawable.ic_stat_normal
						: R.drawable.ic_stat_offline;
			} else {
				persistentNotification.icon = connected > 0 ? R.drawable.ic_stat_message
						: R.drawable.ic_stat_message_offline;
			}
			updateNotification(persistentNotification, ticker);
		} else {
			// Ongoing icons are in the right side, so hide it if necessary.
			if (messageNotifications.isEmpty()) {
				persistentNotification.icon = connected > 0 ? R.drawable.ic_stat_normal
						: R.drawable.ic_stat_offline;
				persistentNotification.when = startTime;
				// Show ticker for the messages in active chat.
				updateNotification(persistentNotification, ticker);
			} else {
				persistentNotification.icon = R.drawable.ic_placeholder;
				persistentNotification.when = Build.VERSION.SDK_INT >= 9 ? -Long.MAX_VALUE
						: Long.MAX_VALUE;
			}
		}

		if (SettingsManager.eventsPersistent()) {
			notify(PERSISTENT_NOTIFICATION_ID, persistentNotification);
		} else {
			notificationManager.cancel(PERSISTENT_NOTIFICATION_ID);
		}
	}

	private void notify(int id, Notification notification) {
		LogManager.i(this, "Notification: " + id + ", ticker: "
				+ notification.tickerText + ", sound: " + notification.sound
				+ ", vibro: "
				+ (notification.defaults & Notification.DEFAULT_VIBRATE)
				+ ", light: "
				+ (notification.defaults & Notification.DEFAULT_LIGHTS));
		try {
			notificationManager.notify(id, notification);
		} catch (SecurityException e) {
		}
	}

	/**
	 * Update notification according to ticker.
	 * 
	 * @param notification
	 * @param ticker
	 */
	private void updateNotification(Notification notification,
			MessageItem ticker) {
		if (ticker == null)
			return;
		if (ticker.getChat().getFirstNotification()
				|| !SettingsManager.eventsFirstOnly())
			setNotificationDefaults(
					notification,
					ChatManager.getInstance().isMakeVibro(
							ticker.getChat().getAccount(),
							ticker.getChat().getUser()),
					PhraseManager.getInstance().getSound(
							ticker.getChat().getAccount(),
							ticker.getChat().getUser(), ticker.getText()),
					AudioManager.STREAM_NOTIFICATION);
		if (ChatManager.getInstance().isShowText(ticker.getChat().getAccount(),
				ticker.getChat().getUser()))
			notification.tickerText = trimText(ticker.getText());
	}

	private MessageNotification getMessageNotification(String account,
			String user) {
		for (MessageNotification messageNotification : messageNotifications)
			if (messageNotification.equals(account, user))
				return messageNotification;
		return null;
	}

	/**
	 * Shows ticker with the message and updates message notification.
	 * 
	 * @param messageItem
	 * @param addNotification
	 *            Whether notification should be stored.
	 */
	public void onMessageNotification(MessageItem messageItem,
			boolean addNotification) {
		if (addNotification) {
			MessageNotification messageNotification = getMessageNotification(
					messageItem.getChat().getAccount(), messageItem.getChat()
							.getUser());
			if (messageNotification == null)
				messageNotification = new MessageNotification(messageItem
						.getChat().getAccount(), messageItem.getChat()
						.getUser(), null, null, 0);
			else
				messageNotifications.remove(messageNotification);
			messageNotification.addMessage(messageItem.getText());
			messageNotifications.add(messageNotification);
			final String account = messageNotification.getAccount();
			final String user = messageNotification.getUser();
			final String text = messageNotification.getText();
			final Date timestamp = messageNotification.getTimestamp();
			final int count = messageNotification.getCount();
			if (AccountManager.getInstance().getArchiveMode(account) != ArchiveMode.dontStore)
				Application.getInstance().runInBackground(new Runnable() {
					@Override
					public void run() {
						NotificationTable.getInstance().write(account, user,
								text, timestamp, count);
					}
				});
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

	public void removeMessageNotification(final String account,
			final String user) {
		MessageNotification messageNotification = getMessageNotification(
				account, user);
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
		for (NotificationProvider<? extends NotificationItem> notificationProvider : providers)
			if (notificationProvider instanceof AccountNotificationProvider) {
				((AccountNotificationProvider<? extends NotificationItem>) notificationProvider)
						.clearAccountNotifications(accountItem.getAccount());
				updateNotifications(notificationProvider, null);
			}
	}

	@Override
	public void run() {
		handler.removeCallbacks(this);
		updateMessageNotification(null);
	}

	public Notification getPersistentNotification() {
		return persistentNotification;
	}

	@Override
	public void onClose() {
		notificationManager.cancelAll();
	}

	/**
	 * @param text
	 * @return Trimmed text.
	 */
	private static String trimText(String text) {
		if (text.length() > MAX_NOTIFICATION_TEXT)
			return text.substring(0, MAX_NOTIFICATION_TEXT - 3) + "...";
		else
			return text;

	}

}
