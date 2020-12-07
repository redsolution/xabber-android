package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.NotificationState;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;


//todo MAY BE USELESS

public class ChatNotificationStateRealmObject extends RealmObject {

    public static final class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String CONTACT_JIO = "contactJid";
        public static final String MODE = "mode";
        public static final String TIMESTAMP = "timestamp";
    }

    @PrimaryKey
    @Required
    private String id;

    private String accountJid;
    private String contactJid;

    private String mode;
    private long timestamp;

    public ChatNotificationStateRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public ChatNotificationStateRealmObject(String id){
        this.id = id;
    }

    public String getId() { return id; }

    public AccountJid getAccountJid() throws XmppStringprepException { return AccountJid.from(accountJid); }
    public void setAccountJid(AccountJid accountJid) { this.accountJid = accountJid.getBareJid().toString(); }

    public ContactJid getContactJid() throws ContactJid.ContactJidCreateException {
        return ContactJid.from(contactJid);
    }
    public void setContactJid(ContactJid contactJid) { this.contactJid = contactJid.getBareJid().toString(); }

    public NotificationState.NotificationMode getMode() {
        return (mode != null) ? NotificationState.NotificationMode.valueOf(mode)
                : NotificationState.NotificationMode.byDefault;
    }
    public void setMode(NotificationState.NotificationMode mode) { this.mode = mode.toString(); }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

}
