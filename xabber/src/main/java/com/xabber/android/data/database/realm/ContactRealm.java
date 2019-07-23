package com.xabber.android.data.database.realm;

import com.xabber.android.data.database.messagerealm.MessageItem;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ContactRealm extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String NAME = "name";
        public static final String ACCOUNT_RESOURCE = "accountResource";
        public static final String LAST_MESSAGE = "lastMessage";
        public static final String GROUPS = "groups";
    }

    @PrimaryKey
    @Required
    private String id;

    private String account;
    private String user;
    private String accountResource;
    private String name;
    private MessageItem lastMessage;
    private RealmList<ContactGroup> groups;

    public ContactRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public ContactRealm(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountResource() {
        return accountResource;
    }

    public void setAccountResource(String accountResource) {
        this.accountResource = accountResource;
    }

    public MessageItem getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageItem lastMessage) {
        this.lastMessage = lastMessage;
    }

    public RealmList<ContactGroup> getGroups() {
        return groups;
    }

    public void setGroups(RealmList<ContactGroup> groups) {
        this.groups = groups;
    }
}
