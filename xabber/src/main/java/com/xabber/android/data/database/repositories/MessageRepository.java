package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.messagerealm.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageRepository {

    private static final String LOG_TAG = MessageRepository.class.getSimpleName();

    public static int getAllUnreadMessagesCount(){
        int unreadCount = 0;
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts())
            unreadCount += Realm.getDefaultInstance()
                    .where(MessageItem.class)
                    .equalTo(MessageItem.Fields.INCOMING, true)
                    .equalTo(MessageItem.Fields.READ, false)
                    .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                    .isNotNull(MessageItem.Fields.TEXT)
                    .equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString())
                    .findAll()
                    .size();
        int waitToRead = 0;
        for (AbstractChat abstractChat : MessageManager.getInstance().getChatsOfEnabledAccount()) {
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                waitToRead += abstractChat.getwaitToMarkAsReadCount();
            if (abstractChat.isArchived())
                unreadCount -= abstractChat.getUnreadMessageCount();
        }
        unreadCount -= waitToRead;
        return unreadCount > 0 ? unreadCount : 0;
    }

    public static RealmResults<MessageItem> getChatMessages(AccountJid accountJid, UserJid userJid) {
        return getChatMessagesQuery(accountJid, userJid)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
    }

    public static RealmResults<MessageItem> getChatMessagesAsync(AccountJid accountJid, UserJid userJid) {
        return getChatMessagesQuery(accountJid, userJid)
                .findAllSortedAsync(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
    }

    public static RealmQuery<MessageItem> getChatMessagesQuery(AccountJid accountJid, UserJid userJid) {
        return Realm.getDefaultInstance()
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString())
                .equalTo(MessageItem.Fields.USER, userJid.toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.TEXT);
    }

    public static void removeAccountMessages(final AccountJid account) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(MessageItem.class)
                            .equalTo(MessageItem.Fields.ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();

                    realm1.where(SyncInfo.class)
                            .equalTo(SyncInfo.FIELD_ACCOUNT, account.toString())
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
