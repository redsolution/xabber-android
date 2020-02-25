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

import android.net.Uri;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ChatDataRealm;
import com.xabber.android.data.database.realmobjects.NotificationStateRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatData;
import com.xabber.android.data.message.NotificationState;

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
     * chat scroll states - position of message list
     */

    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }

        return instance;
    }

    private ChatManager() {
        chatInputs = new NestedMap<>();
    }

    @Override
    public void onLoad() {
        clearUnusedNotificationStateFromRealm();
    }


    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        chatInputs.clear(accountItem.getAccount().toString());
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

    public void saveOrUpdateChatDataToRealm(final AbstractChat chat) {
        final long startTime = System.currentTimeMillis();
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
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

                        chatRealm.setLastPosition(chat.getLastPosition());
                        chatRealm.setArchived(chat.isArchived());
                        chatRealm.setHistoryRequestedAtStart(chat.isHistoryRequestedAtStart());
                        chatRealm.setLastActionTimestamp(chat.getLastActionTimestamp());
                        chatRealm.setChatStateMode(chat.getChatstateMode());
                        chatRealm.setGroupchat(chat.isGroupchat());

                        NotificationStateRealm notificationStateRealm = chatRealm.getNotificationState();
                        if (notificationStateRealm == null)
                            notificationStateRealm = new NotificationStateRealm();

                        notificationStateRealm.setMode(chat.getNotificationState().getMode());
                        notificationStateRealm.setTimestamp(chat.getNotificationState().getTimestamp());
                        chatRealm.setNotificationState(notificationStateRealm);

                        RealmObject realmObject = realm.copyToRealmOrUpdate(chatRealm);
                    }
                });
                if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
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

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        ChatDataRealm realmChat = realm
                .where(ChatDataRealm.class)
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
                    realmChat.isArchived(),
                    notificationState,
                    realmChat.getLastPosition(),
                    realmChat.isHistoryRequestedAtStart(),
                    realmChat.getLastActionTimestamp(),
                    realmChat.getChatstateMode(),
                    realmChat.isGroupchat());
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return chatData;
    }

    public void clearUnusedNotificationStateFromRealm() {
        final long startTime = System.currentTimeMillis();
        // TODO: 13.03.18 ANR - WRITE
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<NotificationStateRealm> results = realm1
                            .where(NotificationStateRealm.class)
                            .findAll();

                    for (NotificationStateRealm notificationState : results) {
                        ChatDataRealm chatDataRealm = realm1
                                .where(ChatDataRealm.class)
                                .equalTo("notificationState.id", notificationState.getId())
                                .findFirst();
                        if (chatDataRealm == null) notificationState.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception("ChatManager", e);
            } finally { if (realm != null) realm.close(); }
        });

        LogManager.d("REALM", Thread.currentThread().getName()
                + " clear unused notif. state: " + (System.currentTimeMillis() - startTime));
    }
}
