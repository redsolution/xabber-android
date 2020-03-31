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

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ChatRealmObject;
import com.xabber.android.data.database.repositories.ChatRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    public Collection<ChatRealmObject> getAllChats(ChatListFragment.ChatListState chatListState){
        if (chatListState == ChatListFragment.ChatListState.recent)
            return ChatRepository.getAllChatsForEnabledAccountsFromRealm();
        if (chatListState == ChatListFragment.ChatListState.unread)
            return ChatRepository.getAllUnreadChatsForEnabledAccount();
        return ChatRepository.getAllChatsForEnabledAccountsFromRealm();
    }

    @Override
    public void onLoad() {
//        DatabaseManager.getInstance().getObservableListener()
//                .debounce(500, TimeUnit.MILLISECONDS)
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnError(throwable -> LogManager.exception("ChatListFragment", throwable))
//                .subscribe(realm -> {
//                    try {
//                        ChatRepository.updateChatsInRealm();
//                    } catch (Exception e) {
//                        LogManager.exception("ChatList", e);
//                    }
//                });
        ChatRepository.clearUnusedNotificationStateFromRealm();
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
    public String getTypedMessage(AccountJid account, ContactJid user) {
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
    public int getSelectionStart(AccountJid account, ContactJid user) {
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
    public int getSelectionEnd(AccountJid account, ContactJid user) {
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
    public void setTyped(AccountJid account, ContactJid user, String typedMessage,
                         int selectionStart, int selectionEnd) {
        ChatInput chat = chatInputs.get(account.toString(), user.toString());
        if (chat == null) {
            chat = new ChatInput();
            chatInputs.put(account.toString(), user.toString(), chat);
        }
        chat.setTyped(typedMessage, selectionStart, selectionEnd);
    }

    public void saveOrUpdateChatDataToRealm(final AbstractChat chat) {
        ChatRepository.saveOrUpdateChatRealmObject(chat.getAccount(), chat.getUser(), null,
                chat.getLastPosition(), false, chat.isArchived(), chat.isHistoryRequestedAtStart(),
                chat.isGroupchat(), chat.getUnreadMessageCount(), null);
    }
}
