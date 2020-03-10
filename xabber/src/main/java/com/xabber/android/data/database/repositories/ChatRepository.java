package com.xabber.android.data.database.repositories;

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

import io.realm.Realm;

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

                String account = accountJid.toString();
                String contact = userJid.toString();

                ChatRealmObject chatRealmObject = realm
                        .where(ChatRealmObject.class)
                        .equalTo(ChatRealmObject.Fields.CONTACT + "." + ContactRealmObject.Fields.ACCOUNT_JID, account)
                        .equalTo(ChatRealmObject.Fields.CONTACT + "." + ContactRealmObject.Fields.CONTACT_JID, contact)
                        .findFirst();

                ContactRealmObject contactRealmObject = realm
                        .where(ContactRealmObject.class)
                        .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, account)
                        .equalTo(ContactRealmObject.Fields.CONTACT_JID, contact)
                        .findFirst();

                if (chatRealmObject == null) {
                    ChatRealmObject newChatRealmObject = new ChatRealmObject(contactRealmObject,
                            lastMessage == null ? MessageRepository.getLastMessageForContactChat(contactRealmObject) : lastMessage,
                            isGroupchat, isArchived, isBlocked, isHistoryRequestAtStart,
                            unreadCount, lastPosition, notificationsPreferences ); //TODO REALM UPDATE unread!!!!11 notif prefs!!111
                    realm.executeTransaction(realm1 -> {
                        realm1.copyToRealm(newChatRealmObject);
                    });
                } else {

                }
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
