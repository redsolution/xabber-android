package com.xabber.android.data.database.repositories;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ChatNotificationsPreferencesRealmObject;
import com.xabber.android.data.database.realmobjects.ChatRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatRepository {

    private static final String LOG_TAG = ChatRepository.class.getSimpleName();

    public static void updateChatsInRealm(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    RealmResults<ChatRealmObject> realmResults = realm1
                            .where(ChatRealmObject.class)
                            .findAll();

                    for (ChatRealmObject chatRealmObject : realmResults){

                        MessageRealmObject messageRealmObject = realm1
                                .where(MessageRealmObject.class)
                                .equalTo(MessageRealmObject.Fields.USER, chatRealmObject.getStringContactJid())
                                .equalTo(MessageRealmObject.Fields.ACCOUNT, chatRealmObject.getStringAccountJid())
                                //.equalTo(MessageRealmObject.Fields.BARE_ACCOUNT_JID, chatRealmObject.getAccountJid())
                                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                                .findFirst();

                        chatRealmObject.setLastMessage(messageRealmObject);
                    }
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void updateLastMessageInRealm(AccountJid accountJid, ContactJid contactJid){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    MessageRealmObject messageRealmObject = realm1
                            .where(MessageRealmObject.class)
                            .equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString())
                            .equalTo(MessageRealmObject.Fields.USER, contactJid.getBareJid().toString())
                            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.DESCENDING)
                            .findFirst();

                    ChatRealmObject chatRealmObject = realm1
                            .where(ChatRealmObject.class)
                            .equalTo(ChatRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                            .equalTo(ChatRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                            .findFirst();

                    chatRealmObject.setLastMessage(messageRealmObject);
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void saveOrUpdateChatRealmObject(AccountJid accountJid, ContactJid contactJid,
                                                   @Nullable MessageRealmObject lastMessage,
                                                   int lastPosition, boolean isBlocked,
                                                   boolean isArchived, boolean isHistoryRequestAtStart,
                                                   boolean isGroupchat, int unreadCount,
                                              ChatNotificationsPreferencesRealmObject notificationsPreferences){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

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

                        realm1.insertOrUpdate(chatRealmObject);
                        realm1.insertOrUpdate(contactRealmObject);
                    }

                });

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static Collection<ChatRealmObject> getAllChatsFromRealm(){
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ChatRealmObject> realmResults = realm
                .where(ChatRealmObject.class)
                .findAll();
        if (Looper.getMainLooper() != Looper.myLooper())
            realm.close();
        return new ArrayList<>(realmResults);
    }

    public static Collection<ChatRealmObject> getAllChatsForAccountFromRealm(AccountJid accountJid){
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ChatRealmObject> realmResults = realm
                .where(ChatRealmObject.class)
                .equalTo(ChatRealmObject.Fields.ACCOUNT_JID,
                        accountJid.toString())
                .isNotNull(ChatRealmObject.Fields.LAST_MESSAGE)
                .findAll();

        if (Looper.getMainLooper() != Looper.myLooper())
            realm.close();
        return new ArrayList<>(realmResults);
    }

    public static ArrayList<ChatRealmObject> getAllChatsForEnabledAccountsFromRealm(){
        ArrayList<ChatRealmObject> result = new ArrayList<>();
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts())
            result.addAll(getAllChatsForAccountFromRealm(accountJid));

        return sortChatList(result);
    }

    public static ArrayList<ChatRealmObject> getAllRecentChatsForEnabledAccountsFromRealm(){
        ArrayList<ChatRealmObject> result = new ArrayList<>();
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts())
            for (ChatRealmObject chatRealmObject: getAllChatsForAccountFromRealm(accountJid))
                if (!chatRealmObject.isArchived())
                    result.add(chatRealmObject);

        return sortChatList(result);
    }

    public static ArrayList<ChatRealmObject> getAllArchivedChatsForEnabledAccount(){
        ArrayList<ChatRealmObject> result = new ArrayList<>();
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts())
            for (ChatRealmObject chatRealmObject: getAllChatsForAccountFromRealm(accountJid))
                if (chatRealmObject.isArchived())
                    result.add(chatRealmObject);

        return sortChatList(result);
    }

    public static ArrayList<ChatRealmObject> getAllUnreadChatsForEnabledAccount(){
        ArrayList<ChatRealmObject> result = new ArrayList<>();
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts())
            for (ChatRealmObject chatRealmObject: getAllChatsForAccountFromRealm(accountJid))
                if (!chatRealmObject.getLastMessage().isRead()
                        && chatRealmObject.getLastMessage().isIncoming())
                    result.add(chatRealmObject);

        return sortChatList(result);
    }

    public static ChatRealmObject getChatRealmObjectFromRealm(AccountJid accountJid, ContactJid contactJid){ //TODO REALM UPDATE should be multiply count of chats per contact
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        ChatRealmObject chatRealmObject = realm
                .where(ChatRealmObject.class)
                .equalTo(ChatRealmObject.Fields.ACCOUNT_JID,
                        accountJid.getFullJid().asBareJid().toString())
                .equalTo(ChatRealmObject.Fields.CONTACT_JID,
                        contactJid.getBareJid().toString())
                .findFirst();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return chatRealmObject;
    }

    private static ArrayList<ChatRealmObject> sortChatList(ArrayList<ChatRealmObject> list){
        list.sort((o1, o2) -> {
            if (o1.getLastMessageTimestamp() == o2.getLastMessageTimestamp())
                return 0;
            if (o1.getLastMessageTimestamp() > o2.getLastMessageTimestamp())
                return -1;
            else return 1;
        });
        return list;
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