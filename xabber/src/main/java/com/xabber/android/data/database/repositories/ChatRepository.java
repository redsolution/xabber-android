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
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

public class ChatRepository {

    private static final String LOG_TAG = ChatRepository.class.getSimpleName();

    public static void saveOrUpdateChatRealmObject(AccountJid accountJid, UserJid userJid,
                                                   @Nullable MessageRealmObject lastMessage,
                                                   int lastPosition, boolean isBlocked,
                                                   boolean isArchived, boolean isHistoryRequestAtStart,
                                                   boolean isGroupchat, int unreadCount,
                                              ChatNotificationsPreferencesRealmObject notificationsPreferences){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();

                ChatRealmObject chatRealmObject = getChatRealmObjectFromRealm(accountJid, userJid);

                ContactRealmObject contactRealmObject = ContactRepository
                        .getContactRealmObjectFromRealm(accountJid, userJid);

                if (chatRealmObject == null) {
                    ChatRealmObject newChatRealmObject = new ChatRealmObject(contactRealmObject,
                            lastMessage == null ? MessageRepository.getLastMessageForContactChat(contactRealmObject) : lastMessage,
                            isGroupchat, isArchived, isBlocked, isHistoryRequestAtStart,
                            unreadCount, lastPosition, notificationsPreferences ); //TODO REALM UPDATE unread!!!!11 notif prefs!!111
                    realm.executeTransaction(realm1 -> {
                        realm1.copyToRealm(newChatRealmObject);
                    });
                } else {
                    realm.executeTransaction(realm1 -> {

                        if (lastMessage == null)
                            chatRealmObject.setLastMessage(MessageRepository.getLastMessageForContactChat(contactRealmObject));
                        else chatRealmObject.setLastMessage(lastMessage);

                        chatRealmObject.setLastPosition(lastPosition);
                        chatRealmObject.setBlocked(isBlocked);
                        chatRealmObject.setArchived(isArchived);
                        chatRealmObject.setHistoryRequestAtStart(isHistoryRequestAtStart);
                        chatRealmObject.setGroupchat(isGroupchat);
                        chatRealmObject.setUnreadMessagesCount(unreadCount); //TODO REALM UPDATE also unread and notif prefs!
                        chatRealmObject.setChatNotificationsPreferences(notificationsPreferences);
                    });

                }
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

    public static ChatRealmObject getChatRealmObjectFromRealm(AccountJid accountJid, UserJid contactJid){ //TODO REALM UPDATE should be multiply count of chats per contact
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        ChatRealmObject chatRealmObject = realm
                .where(ChatRealmObject.class)
                .equalTo(ChatRealmObject.Fields.CONTACT + "." + ContactRealmObject.Fields.ACCOUNT_JID,
                        accountJid.getFullJid().asBareJid().toString())
                .equalTo(ChatRealmObject.Fields.CONTACT + "." + ContactRealmObject.Fields.CONTACT_JID,
                        contactJid.getBareJid().toString())
                .findFirst();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return chatRealmObject;
    }

}



//    NotificationStateRealmObject notificationStateRealmObject = chatRealm.getNotificationState();
//                    if (notificationStateRealmObject == null)
//                            notificationStateRealmObject = new NotificationStateRealmObject();
//
//                            notificationStateRealmObject.setMode(chat.getNotificationState().getMode());
//                            notificationStateRealmObject.setTimestamp(chat.getNotificationState().getTimestamp());
//                            chatRealm.setNotificationState(notificationStateRealmObject);         todo REALM UPDATE old code backup