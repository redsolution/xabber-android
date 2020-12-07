package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.NotificationState;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RegularChatRealmObject extends RealmObject {

    public static final class Fields{
        public static final String ID = "id";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String CONTACT_JID = "contactJid";
        public static final String LAST_MESSAGE = "lastMessage";
        public static final String IS_ARCHIVED = "isArchived";
        public static final String IS_BLOCKED = "isBlocked";
        public static final String UNREAD_MESSAGES_COUNT = "unreadMessagesCount";
        public static final String LAST_POSITION = "lastPosition";
        public static final String CHAT_NOTIFICATIONS_PREFERENCES = "chatNotificationsPreferences";
        public static final String IS_HISTORY_REQUEST_AT_START = "isHistoryRequestAtStart";
        public static final String LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp";
        public static final String NOTIFICATION_MODE = "notificationMode";
        public static final String NOTIFICATION_TIMESTAMP = "notificationTimestamp";
    }

    @PrimaryKey
    private String id;

    private String accountJid;
    private String contactJid;
    private MessageRealmObject lastMessage;
    private Long lastMessageTimestamp;
    private boolean isArchived;
    private boolean isBlocked;
    private boolean isHistoryRequestAtStart;
    private int unreadMessagesCount;
    private int lastPosition;
    private String notificationMode;
    private long notificationTimestamp;

    public RegularChatRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

    public RegularChatRealmObject(AccountJid accountJid, ContactJid contactJid){
        this.id = UUID.randomUUID().toString();
        this.accountJid = accountJid.toString();
        this.contactJid = contactJid.getBareJid().toString();
    }

    public RegularChatRealmObject(AccountJid accountJid, ContactJid contactJid,
                                  MessageRealmObject lastMessage, boolean isArchived, boolean isBlocked,
                                  boolean isHistoryRequestAtStart, int unreadMessagesCount, int lastPosition,
                                  NotificationState notificationState){
        this.id = UUID.randomUUID().toString();
        this.accountJid = accountJid.toString();
        this.contactJid = contactJid.getBareJid().toString();
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessage.getTimestamp();
        this.isArchived = isArchived;
        this.isBlocked = isBlocked;
        this.isHistoryRequestAtStart = isHistoryRequestAtStart;
        this.unreadMessagesCount = unreadMessagesCount;
        this.lastPosition = lastPosition;
        this.notificationMode = notificationState.getMode().toString();
        this.notificationTimestamp = notificationState.getTimestamp();
    }

    public AccountJid getAccountJid(){
        try {
            return AccountJid.from(accountJid);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public ContactJid getContactJid(){
        try {
            return ContactJid.from(contactJid);
        } catch (ContactJid.ContactJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setLastPosition(int lastPosition) { this.lastPosition = lastPosition; }
    public int getLastPosition() { return lastPosition; }

    public MessageRealmObject getLastMessage() { return lastMessage; }
    public void setLastMessage(MessageRealmObject lastMessage) {
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessage.getTimestamp();
    }

    public Long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public boolean isHistoryRequestAtStart() { return isHistoryRequestAtStart; }
    public void setHistoryRequestAtStart(boolean historyRequestAtStart) { isHistoryRequestAtStart = historyRequestAtStart; }

    public int getUnreadMessagesCount() { return unreadMessagesCount; }
    public void setUnreadMessagesCount(int unreadMessagesCount) { this.unreadMessagesCount = unreadMessagesCount; }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationMode = notificationState.getMode().toString();
        this.notificationTimestamp = notificationState.getTimestamp();
    }
    public NotificationState getNotificationState(){
        if (notificationMode != null){
            return new NotificationState(NotificationState.NotificationMode.valueOf(notificationMode),
                    notificationTimestamp);
        } else return new NotificationState(NotificationState.NotificationMode.byDefault, 0);
    }

}
