package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class NotificationChatRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String NOTIFICATION_ID = "notificationID";
        public static final String CHAT_TITLE = "chatTitle";
        public static final String IS_GROUP_CHAT = "isGroupChat";
        public static final String MESSAGES = "messages";
    }

    @PrimaryKey
    @Required
    private String id;
    private String account;
    private String user;
    private int notificationID;
    private String chatTitle;
    private boolean isGroupChat;
    private RealmList<NotificationMessageRealmObject> messages;

    public NotificationChatRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public NotificationChatRealmObject(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public AccountJid getAccount() {
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setAccount(AccountJid account) {
        this.account = account.toString();
    }

    public ContactJid getUser() {
        try {
            return ContactJid.from(user);
        } catch (ContactJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(ContactJid user) {
        this.user = user.toString();
    }

    public int getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(int notificationID) {
        this.notificationID = notificationID;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public RealmList<NotificationMessageRealmObject> getMessages() {
        return messages;
    }

    public void setMessages(RealmList<NotificationMessageRealmObject> messages) {
        this.messages = messages;
    }
}
