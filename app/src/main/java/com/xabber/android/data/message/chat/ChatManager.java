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

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.NestedMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Manage chat specific options.
 *
 * @author alexander.ivanov
 */
public class ChatManager implements OnLoadListener, OnAccountRemovedListener {

    public static final Uri EMPTY_SOUND = Uri
            .parse("com.xabber.android.data.message.ChatManager.EMPTY_SOUND");

    private static final Object PRIVATE_CHAT = new Object();
    private final static ChatManager instance;

    static {
        instance = new ChatManager();
        Application.getInstance().addManager(instance);
    }

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

    private ChatManager() {
        chatInputs = new NestedMap<ChatInput>();
        privateChats = new NestedMap<Object>();
        sounds = new NestedMap<Uri>();
        showText = new NestedMap<>();
        makeVibro = new NestedMap<Boolean>();
        notifyVisible = new NestedMap<Boolean>();
        suppress100 = new NestedMap<Boolean>();
    }

    public static ChatManager getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        final Set<BaseEntity> privateChats = new HashSet<BaseEntity>();
        final NestedMap<Boolean> notifyVisible = new NestedMap<Boolean>();
        final NestedMap<ShowMessageTextInNotification> showText = new NestedMap<>();
        final NestedMap<Boolean> makeVibro = new NestedMap<Boolean>();
        final NestedMap<Uri> sounds = new NestedMap<Uri>();
        final NestedMap<Boolean> suppress100 = new NestedMap<Boolean>();
        Cursor cursor;
        cursor = PrivateChatTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    privateChats.add(new BaseEntity(PrivateChatTable
                            .getAccount(cursor), PrivateChatTable
                            .getUser(cursor)));
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
        for (BaseEntity baseEntity : privateChats)
            this.privateChats.put(baseEntity.getAccount(),
                    baseEntity.getUser(), PRIVATE_CHAT);
        this.notifyVisible.addAll(notifyVisible);
        this.showText.addAll(showText);
        this.makeVibro.addAll(vibro);
        this.sounds.addAll(sounds);
        this.suppress100.addAll(suppress100);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        chatInputs.clear(accountItem.getAccount());
        privateChats.clear(accountItem.getAccount());
        sounds.clear(accountItem.getAccount());
        showText.clear(accountItem.getAccount());
        makeVibro.clear(accountItem.getAccount());
        notifyVisible.clear(accountItem.getAccount());
        suppress100.clear(accountItem.getAccount());
    }

    /**
     * Whether to save history for specified chat.
     *
     * @param account
     * @param user
     * @return
     */
    public boolean isSaveMessages(String account, String user) {
        return privateChats.get(account, user) != PRIVATE_CHAT;
    }

    /**
     * Sets whether to save history for specified chat.
     *
     * @param account
     * @param user
     * @param save
     */
    public void setSaveMessages(final String account, final String user,
                                final boolean save) {
        if (save)
            privateChats.remove(account, user);
        else
            privateChats.put(account, user, PRIVATE_CHAT);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (save)
                    PrivateChatTable.getInstance().remove(account, user);
                else
                    PrivateChatTable.getInstance().write(account, user);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return typed but not sent message.
     */
    public String getTypedMessage(String account, String user) {
        ChatInput chat = chatInputs.get(account, user);
        if (chat == null)
            return "";
        return chat.getTypedMessage();
    }

    /**
     * @param account
     * @param user
     * @return Start selection position.
     */
    public int getSelectionStart(String account, String user) {
        ChatInput chat = chatInputs.get(account, user);
        if (chat == null)
            return 0;
        return chat.getSelectionStart();
    }

    /**
     * @param account
     * @param user
     * @return End selection position.
     */
    public int getSelectionEnd(String account, String user) {
        ChatInput chat = chatInputs.get(account, user);
        if (chat == null)
            return 0;
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
    public void setTyped(String account, String user, String typedMessage,
                         int selectionStart, int selectionEnd) {
        ChatInput chat = chatInputs.get(account, user);
        if (chat == null) {
            chat = new ChatInput();
            chatInputs.put(account, user, chat);
        }
        chat.setTyped(typedMessage, selectionStart, selectionEnd);
    }

    /**
     * @param account
     * @param user
     * @return Whether notification in visible chat must be shown. Common value
     * if there is no user specific value.
     */
    public boolean isNotifyVisible(String account, String user) {
        Boolean value = notifyVisible.get(account, user);
        if (value == null)
            return SettingsManager.eventsVisibleChat();
        return value;
    }

    public void setNotifyVisible(final String account, final String user,
                                 final boolean value) {
        notifyVisible.put(account, user, value);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                NotifyVisibleTable.getInstance().write(account, user, value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether text of messages must be shown in notification area.
     * Common value if there is no user specific value.
     */
    public boolean isShowText(String account, String user) {
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

    public ShowMessageTextInNotification getShowText(String account, String user) {
        ShowMessageTextInNotification showMessageTextInNotification = showText.get(account, user);
        if (showMessageTextInNotification == null) {
            return ShowMessageTextInNotification.default_settings;
        } else {
            return showMessageTextInNotification;
        }
    }

    public void setShowText(final String account, final String user, final ShowMessageTextInNotification value) {
        showText.put(account, user, value);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                ShowTextTable.getInstance().write(account, user, value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether vibro should be used while notification. Common value if
     * there is no user specific value.
     */
    public boolean isMakeVibro(String account, String user) {
        Boolean value = makeVibro.get(account, user);
        if (value == null)
            return SettingsManager.eventsVibro();
        return value;
    }

    public void setMakeVibro(final String account, final String user,
                             final boolean value) {
        makeVibro.put(account, user, value);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                VibroTable.getInstance().write(account, user, value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Sound for notification. Common value if there is no user specific
     * value.
     */
    public Uri getSound(String account, String user) {
        Uri value = sounds.get(account, user);
        if (value == null)
            return SettingsManager.eventsSound();
        if (EMPTY_SOUND.equals(value))
            return null;
        return value;
    }

    public void setSound(final String account, final String user,
                         final Uri value) {
        sounds.put(account, user, value == null ? EMPTY_SOUND : value);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                SoundTable.getInstance().write(account, user,
                        value == null ? EMPTY_SOUND : value);
            }
        });
    }

    /**
     * @param account
     * @param user
     * @return Whether 'This Room is not Anonymous'-messages (Status Code 100) should be suppressed.
     */
    public boolean isSuppress100(String account, String user) {
        Boolean value = suppress100.get(account, user);
        if (value == null)
            return SettingsManager.eventsSuppress100();
        return value;
    }

    public void setSuppress100(final String account, final String user,
                             final boolean value) {
        suppress100.put(account, user, value);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Suppress100Table.getInstance().write(account, user, value);
            }
        });
    }
}
