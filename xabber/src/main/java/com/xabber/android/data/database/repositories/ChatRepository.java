package com.xabber.android.data.database.repositories;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ChatNotificationsPreferencesRealmObject;
import com.xabber.android.data.database.realmobjects.ChatRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.RegularChat;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatRepository {

    private static final String LOG_TAG = ChatRepository.class.getSimpleName();

    public static void saveOrUpdateChatRealmObject(AccountJid accountJid, ContactJid contactJid,
                                                   @Nullable MessageRealmObject lastMessage,
                                                   int lastPosition, boolean isBlocked,
                                                   boolean isArchived, boolean isHistoryRequestAtStart,
                                                   boolean isGroupchat, int unreadCount,
                                              ChatNotificationsPreferencesRealmObject notificationsPreferences){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                LogManager.d("ChatListFragment", "Start to save chat");
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    LogManager.d("ChatListFragment", "Continue to save chat");
                    ContactRealmObject contactRealmObject = realm1
                            .where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, accountJid.getFullJid().asBareJid().toString())
                            .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                            .findFirst();

                    ChatRealmObject chatRealmObject = realm1
                            .where(ChatRealmObject.class)
                            .equalTo(ChatRealmObject.Fields.ACCOUNT_JID,
                                    accountJid.toString())
                            .equalTo(ChatRealmObject.Fields.CONTACT_JID,
                                    contactJid.getBareJid().toString())
                            .findFirst();

                    MessageRealmObject messageRealmObject = realm1
                            .where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.USER, contactJid.getBareJid().toString())
                            .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                            //.equalTo(MessageRealmObject.Fields.BARE_ACCOUNT_JID, accountJid.getFullJid().asBareJid().toString())
                            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                            .findFirst();

                    if (chatRealmObject == null) {

                        ChatRealmObject newChatRealmObject = new ChatRealmObject(accountJid, contactJid,
                                lastMessage == null ? messageRealmObject : lastMessage,
                                isGroupchat, isArchived, isBlocked, isHistoryRequestAtStart,
                                unreadCount, lastPosition, notificationsPreferences );

                        if (!contactRealmObject.getChats().contains(newChatRealmObject))
                            contactRealmObject.getChats().add(newChatRealmObject);

                        LogManager.d("ChatListFragment", "Performing chat saving");
                        realm1.insertOrUpdate(newChatRealmObject);
                        realm1.insertOrUpdate(contactRealmObject);
                    } else {

                        chatRealmObject.setLastMessage(lastMessage == null ? messageRealmObject : lastMessage);
                        chatRealmObject.setLastPosition(lastPosition);
                        chatRealmObject.setBlocked(isBlocked);
                        chatRealmObject.setArchived(isArchived);
                        chatRealmObject.setHistoryRequestAtStart(isHistoryRequestAtStart);
                        chatRealmObject.setGroupchat(isGroupchat);
                        chatRealmObject.setUnreadMessagesCount(unreadCount); //TODO REALM UPDATE also unread and notif prefs!
                        chatRealmObject.setChatNotificationsPreferences(notificationsPreferences);

                        if (!contactRealmObject.getChats().contains(chatRealmObject))
                            contactRealmObject.getChats().add(chatRealmObject);

                        LogManager.d("ChatListFragment", "Performing chat saving");
                        realm1.insertOrUpdate(chatRealmObject);
                        realm1.insertOrUpdate(contactRealmObject);
                    }

                });

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static Collection<AbstractChat> getAllChatsFromRealm(){
        Collection<AbstractChat> result = new ArrayList<>();

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ChatRealmObject> realmResults = realm
                .where(ChatRealmObject.class)
                .findAll();

        for (ChatRealmObject chatRealmObject : realmResults){
            RegularChat regularChat = new RegularChat(chatRealmObject.getAccountJid(), chatRealmObject.getContactJid());
            regularChat.setArchivedWithoutRealm(chatRealmObject.isArchived());
            regularChat.setLastPosition(chatRealmObject.getLastPosition());
            regularChat.setHistoryRequestedWithoutRealm(chatRealmObject.isHistoryRequestAtStart());
            regularChat.setLastActionTimestamp(chatRealmObject.getLastMessageTimestamp());
            regularChat.setGroupchat(chatRealmObject.isGroupchat());
            result.add(regularChat);
        }

        if (Looper.getMainLooper() != Looper.myLooper())
            realm.close();

        return result;
    }

    public static void clearUnusedNotificationStateFromRealm() {
//        final long startTime = System.currentTimeMillis();   //TODO REALM UPDATE
//        Application.getInstance().runInBackground(() -> {
//            Realm realm = null;
//            try {
//                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
//                realm.executeTransaction(realm1 -> {
//                    RealmResults<NotificationStateRealmObject> results = realm1
//                            .where(NotificationStateRealmObject.class)
//                            .findAll();
//
//                    for (NotificationStateRealmObject notificationState : results) {
//                        ChatRealmObject chatRealmObject = realm1
//                                .where(ChatRealmObject.class)
//                                .equalTo(N)
//                                .findFirst();
//                        if (chatRealmObject == null) notificationState.deleteFromRealm();
//                    }
//                });
//            } catch (Exception e) {
//                LogManager.exception("ChatManager", e);
//            } finally { if (realm != null) realm.close(); }
//        });
//
//        LogManager.d("REALM", Thread.currentThread().getName()
//                + " clear unused notif. state: " + (System.currentTimeMillis() - startTime));
    }
}



//    NotificationStateRealmObject notificationStateRealmObject = chatRealm.getNotificationState();
//                    if (notificationStateRealmObject == null)
//                            notificationStateRealmObject = new NotificationStateRealmObject();
//
//                            notificationStateRealmObject.setMode(chat.getNotificationState().getMode());
//                            notificationStateRealmObject.setTimestamp(chat.getNotificationState().getTimestamp());
//                            chatRealm.setNotificationState(notificationStateRealmObject);         todo REALM UPDATE old code backup