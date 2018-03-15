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
package com.xabber.android.data.message.chat;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.ChatDataRealm;
import com.xabber.android.data.database.realm.NotificationStateRealm;
import com.xabber.android.data.database.sqlite.NotifyVisibleTable;
import com.xabber.android.data.database.sqlite.PrivateChatTable;
import com.xabber.android.data.database.sqlite.ShowTextTable;
import com.xabber.android.data.database.sqlite.SoundTable;
import com.xabber.android.data.database.sqlite.Suppress100Table;
import com.xabber.android.data.database.sqlite.VibroTable;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatData;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.RosterManager;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashSet;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Manage chat specific options.
 *
 * @author alexander.ivanov
 */
public class ChatManager implements OnLoadListener, OnAccountRemovedListener {

    public static final Uri EMPTY_SOUND = Uri
            .parse("com.xabber.android.data.message.ChatManager.EMPTY_SOUND");

    private static final Object PRIVATE_CHAT = new Object();
    private static ChatManager instance;

    /**
     * Stored input for user in account.
     */
    private final NestedMap<ChatInput> chatInputs;
    /**
     * List of chats whose messages mustn't be saved for user in account.
     */
    private final NestedMap<Object> privateChats;
    /**
     * Whether notification in visible chat should be used for user in account.
     */
    private final NestedMap<Boolean> notifyVisible;
    /**
     * Whether text of incoming message should be shown in notification bar for
     * user in account.
     */
    private final NestedMap<ShowMessageTextInNotification> showText;
    /**
     * Whether vibro notification should be used for user in account.
     */
    private final NestedMap<Boolean> makeVibro;
    /**
     * Sound, associated with chat for user in account.
     */
    private final NestedMap<Uri> sounds;
    /**
     * Whether 'This room is not anonymous'-messages (Status Code 100) should be suppressed
     */
    private final NestedMap<Boolean> suppress100;

    /**
     * chat scroll states - position of message list
     */
    private final NestedMap<Parcelable> scrollStates;

    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }

        return instance;
    }

    private ChatManager() {
        chatInputs = new NestedMap<>();
        privateChats = new NestedMap<>();
        sounds = new NestedMap<>();
        showText = new NestedMap<>();
        makeVibro = new NestedMap<>();
        notifyVisible = new NestedMap<>();
        suppress100 = new NestedMap<>();
        scrollStates = new NestedMap<>();
    }

    @Override
    public void onLoad() {
        final Set<BaseEntity> privateChats = new HashSet<>();
        final NestedMap<Boolean> notifyVisible = new NestedMap<>();
        final NestedMap<ShowMessageTextInNotification> showText = new NestedMap<>();
        final NestedMap<Boolean> makeVibro = new NestedMap<>();
        final NestedMap<Uri> sounds = new NestedMap<>();
        final NestedMap<Boolean> suppress100 = new NestedMap<>();
        Cursor cursor;
        cursor = PrivateChatTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        privateChats.add(RosterManager.getInstance().getAbstractContact(
                                AccountJid.from(PrivateChatTable.getAccount(cursor)),
                                UserJid.from(PrivateChatTable.getUser(cursor))
                        ));

                    } catch (UserJid.UserJidCreateException | XmppStringprepException e) {
                        LogManager.exception(this, e);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        cursor = NotifyVisibleTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    notifyVisible.put(NotifyVisibleTable.getAccount(cursor),
                            NotifyVisibleTable.getUser(cursor),
                            NotifyVisibleTable.getValue(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        cursor = ShowTextTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    showText.put(ShowTextTable.getAccount(cursor),
                            ShowTextTable.getUser(cursor),
                            ShowTextTable.getValue(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        cursor = VibroTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    makeVibro.put(VibroTable.getAccount(cursor),
                            VibroTable.getUser(cursor),
                            VibroTable.getValue(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        cursor = SoundTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    sounds.put(SoundTable.getAccount(cursor),
                            SoundTable.getUser(cursor),
                            SoundTable.getValue(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        cursor = Suppress100Table.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    makeVibro.put(Suppress100Table.getAccount(cursor),
                            Suppress100Table.getUser(cursor),
                            Suppress100Table.getValue(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        clearUnusedNotificationStateFromRealm();

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(privateChats, notifyVisible, showText, makeVibro,
                        sounds, suppress100);
            }
        });
    }

    private void onLoaded(Set<BaseEntity> privateChats,
                          NestedMap<Boolean> notifyVisible, NestedMap<ShowMessageTextInNotification> showText,
                          NestedMap<Boolean> vibro, NestedMap<Uri> sounds, NestedMap<Boolean> suppress100) {
        for (BaseEntity baseEntity : privateChats) {
            this.privateChats.put(baseEntity.getAccount().toString(),
                    baseEntity.getUser().toString(), PRIVATE_CHAT);
        }
        this.notifyVisible.addAll(notifyVisible);
        this.showText.addAll(showText);
        this.makeVibro.addAll(vibro);
        this.sounds.addAll(sounds);
        this.suppress100.addAll(suppress100);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        chatInputs.clear(accountItem.getAccount().toString());
        privateChats.clear(accountItem.getAccount().toString());
        sounds.clear(accountItem.getAccount().toString());
        showText.clear(accountItem.getAccount().toString());
        makeVibro.clear(accountItem.getAccount().toString());
        notifyVisible.clear(accountItem.getAccount().toString());
        suppress100.clear(accountItem.getAccount().toString());
    }

    /**
     * Whether to save history for specified chat.
     *
     * @param account
     * @param user
     * @return
     */
    public boolean isSaveMessages(AccountJid account, UserJid user) {
        return privateChats.get(account.toString(), user.toString()) != PRIVATE_CHAT;
    }

    /**
     * Sets whether to save history for specified chat.
     *
     * @param account
     * @param user
     * @param save
     */
    public void setSaveMessages(final AccountJid account, final UserJid user,
                                final boolean save) {
        if (save) {
            privateChats.remove(account.toString(), user.toString());
        } else {
            privateChats.put(account.toString(), user.toString(), PRIVATE_CHAT);
        }
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                if (save) {
                    PrivateChatTable.getInstance().remove(account.toString(), user.toString());
                } else {
                    PrivateChatTable.getInstance().write(account.toString(), user.toString());
                }
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return typed but not sent message.
     */
    public String getTypedMessage(AccountJid account, UserJid user) {
        ChatInput chat = chatInputs.get(account.toString(), user.toString());
        if (chat == null) {
            return "";
        }
        return chat.getTypedMessage();
    }

    /**
     * @param account
     * @param user
     * @return Start selection position.
     */
    public int getSelectionStart(AccountJid account, UserJid user) {
        ChatInput chat = chatInputs.get(account.toString(), user.toString());
        if (chat == null) {
            return 0;
        }
        return chat.getSelectionStart();
    }

    /**
     * @param account
     * @param user
     * @return End selection position.
     */
    public int getSelectionEnd(AccountJid account, UserJid user) {
        ChatInput chat = chatInputs.get(account.toString(), user.toString());
        if (chat == null) {
            return 0;
        }
        return chat.getSelectionEnd();
    }

    /**
     * Sets typed message and selection options for specified chat.
     *
     * @param account
     * @param user
     * @param typedMessage
     * @param selectionStart
     * @param selectionEnd
     */
    public void setTyped(AccountJid account, UserJid user, String typedMessage,
                         int selectionStart, int selectionEnd) {
        ChatInput chat = chatInputs.get(account.toString(), user.toString());
        if (chat == null) {
            chat = new ChatInput();
            chatInputs.put(account.toString(), user.toString(), chat);
        }
        chat.setTyped(typedMessage, selectionStart, selectionEnd);
    }

    /**
     * @param account
     * @param user
     * @return Whether notification in visible chat must be shown. Common value
     * if there is no user specific value.
     */
    public boolean isNotifyVisible(AccountJid account, UserJid user) {
        Boolean value = notifyVisible.get(account.toString(), user.toString());
        if (value == null) {
            return SettingsManager.eventsVisibleChat();
        }
        return value;
    }

    public void setNotifyVisible(final AccountJid account, final UserJid user, final boolean value) {
        notifyVisible.put(account.toString(), user.toString(), value);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotifyVisibleTable.getInstance().write(account.toString(), user.toString(), value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether text of messages must be shown in notification area.
     * Common value if there is no user specific value.
     */
    public boolean isShowText(AccountJid account, UserJid user) {
        switch (getShowText(account, user)) {
            case show:
                return true;
            case hide:
                return false;
            case default_settings:
            default:
                return SettingsManager.eventsShowText();
        }
    }

    public boolean isShowTextOnMuc(AccountJid account, UserJid user) {
        switch (getShowText(account, user)) {
            case show:
                return true;
            case hide:
                return false;
            case default_settings:
            default:
                return SettingsManager.eventsShowTextOnMuc();
        }
    }

    public ShowMessageTextInNotification getShowText(AccountJid account, UserJid user) {
        ShowMessageTextInNotification showMessageTextInNotification = showText.get(account.toString(), user.toString());
        if (showMessageTextInNotification == null) {
            return ShowMessageTextInNotification.default_settings;
        } else {
            return showMessageTextInNotification;
        }
    }

    public void setShowText(final AccountJid account, final UserJid user, final ShowMessageTextInNotification value) {
        showText.put(account.toString(), user.toString(), value);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                ShowTextTable.getInstance().write(account.toString(), user.toString(), value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether vibro should be used while notification. Common value if
     * there is no user specific value.
     */
    public boolean isMakeVibro(AccountJid account, UserJid user) {
        Boolean value = makeVibro.get(account.toString(), user.toString());
        if (value == null) {
            return true;
        }
        return value;
    }

    public void setMakeVibro(final AccountJid account, final UserJid user, final boolean value) {
        makeVibro.put(account.toString(), user.toString(), value);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                VibroTable.getInstance().write(account.toString(), user.toString(), value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Sound for notification. Common value if there is no user specific
     * value.
     */
    public Uri getSound(AccountJid account, UserJid user, boolean isMUC) {
        Uri value = sounds.get(account.toString(), user.toString());
        if (value == null) {
            if (isMUC) return SettingsManager.eventsSoundMuc();
            return SettingsManager.eventsSound();
        }
        if (EMPTY_SOUND.equals(value)) {
            return null;
        }
        return value;
    }

    public void setSound(final AccountJid account, final UserJid user, final Uri value) {
        sounds.put(account.toString(), user.toString(), value == null ? EMPTY_SOUND : value);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                SoundTable.getInstance().write(account.toString(), user.toString(),
                        value == null ? EMPTY_SOUND : value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether 'This Room is not Anonymous'-messages (Status Code 100) should be suppressed.
     */
    public boolean isSuppress100(AccountJid account, UserJid user) {
        Boolean value = suppress100.get(account.toString(), user.toString());
        if (value == null)
            return SettingsManager.eventsSuppress100();
        return value;
    }

    public void setSuppress100(final AccountJid account, final UserJid user,
                             final boolean value) {
        suppress100.put(account.toString(), user.toString(), value);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Suppress100Table.getInstance().write(account.toString(), user.toString(), value);
            }
        });
    }

    public Parcelable getScrollState(AccountJid account, UserJid user) {
        return scrollStates.get(account.toString(), user.toString());
    }

    public void setScrollState(AccountJid account, UserJid user, Parcelable parcelable) {
        scrollStates.put(account.toString(), user.toString(), parcelable);
    }

    public void clearScrollStates() {
        scrollStates.clear();
    }

    public void saveOrUpdateChatDataToRealm(final AbstractChat chat) {
        final long startTime = System.currentTimeMillis();
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        String accountJid = chat.getAccount().toString();
                        String userJid = chat.getUser().toString();

                        ChatDataRealm chatRealm = realm.where(ChatDataRealm.class)
                                .equalTo("accountJid", accountJid)
                                .equalTo("userJid", userJid)
                                .findFirst();

                        if (chatRealm == null)
                            chatRealm = new ChatDataRealm(accountJid, userJid);

                        chatRealm.setUnreadCount(chat.getUnreadMessageCount());
                        chatRealm.setArchived(chat.isArchived());

                        NotificationStateRealm notificationStateRealm = chatRealm.getNotificationState();
                        if (notificationStateRealm == null)
                            notificationStateRealm = new NotificationStateRealm();

                        notificationStateRealm.setMode(chat.getNotificationState().getMode());
                        notificationStateRealm.setTimestamp(chat.getNotificationState().getTimestamp());
                        chatRealm.setNotificationState(notificationStateRealm);

                        RealmObject realmObject = realm.copyToRealmOrUpdate(chatRealm);
                    }
                });
            }
        });
        LogManager.d("REALM", Thread.currentThread().getName()
                + " save chat data: " + (System.currentTimeMillis() - startTime));
    }

    @Nullable
    public ChatData loadChatDataFromRealm(AbstractChat chat) {
        String accountJid = chat.getAccount().toString();
        String userJid = chat.getUser().toString();
        ChatData chatData = null;

        Realm realm = RealmManager.getInstance().getNewRealm();
        ChatDataRealm realmChat = realm.where(ChatDataRealm.class)
                .equalTo("accountJid", accountJid)
                .equalTo("userJid", userJid)
                .findFirst();

        if (realmChat != null) {
            NotificationState notificationState;
            if (realmChat.getNotificationState() != null) {
                 notificationState = new NotificationState(
                        realmChat.getNotificationState().getMode(),
                        realmChat.getNotificationState().getTimestamp()
                );
            } else notificationState =
                    new NotificationState(NotificationState.NotificationMode.bydefault, 0);

            chatData = new ChatData(
                    realmChat.getSubject(),
                    realmChat.getAccountJid(),
                    realmChat.getUserJid(),
                    realmChat.getUnreadCount(),
                    realmChat.isArchived(),
                    notificationState);
        }

        realm.close();
        return chatData;
    }

    public void clearUnusedNotificationStateFromRealm() {
        final long startTime = System.currentTimeMillis();
        // TODO: 13.03.18 ANR - WRITE
        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();

        RealmResults<NotificationStateRealm> results = realm.where(NotificationStateRealm.class).findAll();

        for (NotificationStateRealm notificationState : results) {
            ChatDataRealm chatDataRealm = realm.where(ChatDataRealm.class)
                    .equalTo("notificationState.id", notificationState.getId()).findFirst();
            if (chatDataRealm == null) notificationState.deleteFromRealm();
        }

        realm.commitTransaction();
        realm.close();
        LogManager.d("REALM", Thread.currentThread().getName()
                + " clear unused notif. state: " + (System.currentTimeMillis() - startTime));
    }
}
