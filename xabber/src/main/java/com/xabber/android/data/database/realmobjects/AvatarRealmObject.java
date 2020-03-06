package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AvatarRealmObject extends RealmObject {
    public static final class Fields{
        public static final String ID = "id";
        public static final String CONTACT = "contactRealmObject";
        public static final String VCARD_HASH = "vCardHash";
        public static final String PEP_HASH = "pepHash";
    }

    @PrimaryKey
    private String id;
    private ContactRealmObject contactRealmObject;
    private String vCardHash;
    private String pepHash;

    public AvatarRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

    public AvatarRealmObject(ContactRealmObject contactRealmObject){
        this.id = UUID.randomUUID().toString();
        this.contactRealmObject = contactRealmObject;
    }

    public ContactRealmObject getContactRealmObject() { return contactRealmObject; }
    public void setContactRealmObject(ContactRealmObject contactRealmObject) {
        this.contactRealmObject = contactRealmObject;
    }

    public String getId() { return id; }

    public String getPepHash() { return pepHash; }
    public void setPepHash(String pepHash) { this.pepHash = pepHash; }

    public String getVCardHash() { return vCardHash; }
    public void setVCardHash(String vCardHash) { this.vCardHash = vCardHash; }
}
