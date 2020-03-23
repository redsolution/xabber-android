package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

import org.jxmpp.jid.Jid;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AvatarRealmObject extends RealmObject {
    public static final class Fields{
        public static final String ID = "id";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String CONTACT_JID = "contact_jid";
        public static final String VCARD_HASH = "vCardHash";
        public static final String PEP_HASH = "pepHash";
    }

    @PrimaryKey
    private String id;
    private String accountJid;
    private String contactJid;
    private String vCardHash;
    private String pepHash;

    public AvatarRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

    public AvatarRealmObject(Jid contactJid){
        this.id = UUID.randomUUID().toString();
        this.contactJid = contactJid.asBareJid().toString();
    }

    public AvatarRealmObject(ContactJid contactJid){
        this.id = UUID.randomUUID().toString();
        this.contactJid = contactJid.getBareJid().toString();
    }

    public AvatarRealmObject(AccountJid accountJid, ContactJid contactJid){
        this.id = UUID.randomUUID().toString();
        this.accountJid = accountJid.getFullJid().asBareJid().toString();
        this.contactJid = contactJid.getBareJid().toString();
    }

    public String getId() { return id; }

    public String getAccountJid() { return accountJid; }

    public String getContactJid() { return contactJid; }

    public String getPepHash() { return pepHash; }
    public void setPepHash(String pepHash) { this.pepHash = pepHash; }

    public String getVCardHash() { return vCardHash; }
    public void setVCardHash(String vCardHash) { this.vCardHash = vCardHash; }
}
