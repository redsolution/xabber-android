package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class ChatRealmObject extends RealmObject {

    public static final class Fields{
        public static final String ID = "id";
        public static final String CONTACT = "contact";
        public static final String LAST_MESSAGE = "lastMessage";
        public static final String IS_GROUPCHAT = "isGroupchat";
        public static final String IS_ARCHIVED = "isArchived";
        public static final String IS_BLOCKED = "isBlocked";
        public static final String UNREAD_MESSAGES_COUNT = "unreadMessagesCount";
        public static final String CHAT_NOTIFICATIONS_PREFERENCES = "chatNotificationsPreferences";
    }

    @PrimaryKey
    private String id;

    private ContactRealmObject contact;

    private MessageRealmObject lastMessage;
    private boolean isGroupchat;
    private boolean isArchived;
    private boolean isBlocked;
    private int unreadMessagesCount;
    private ChatNotificationsPreferencesRealmObject chatNotificationsPreferences;

}
