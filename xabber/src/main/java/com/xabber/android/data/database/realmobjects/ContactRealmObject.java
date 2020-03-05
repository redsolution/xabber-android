package com.xabber.android.data.database.realmobjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ContactRealmObject extends RealmObject {

    public static final class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT_JID = "accountJid";
        public static final String CONTACT_JID = "contactJid";
        public static final String BEST_NAME = "bestName";
        public static final String CHATS = "chats";
        public static final String AVATARS = "avatars";
        public static final String RESOURCES = "resources";
    }

    //TODO REALM UPDATE add status link
    @PrimaryKey
    private String id;
    private String accountJid;
    private String contactJid;
    private String bestName;
    public RealmList<ChatRealmObject> chats;
    public RealmList<OldAvatarRealmObject> avatars;
    public RealmList<ResourceRealmObject> resources;
    public RealmList<ContactGroupRealmObject> groups;

    public ContactRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountJid() { return accountJid; }
    public void setAccountJid(String accountJid) { this.accountJid = accountJid; }

    public String getContactJid() { return contactJid; }
    public void setContactJid(String contactJid) { this.contactJid = contactJid; }

    public String getBestName() { return bestName; }
    public void setBestName(String bestName) { this.bestName = bestName; }

    public RealmList<ChatRealmObject> getChats() { return chats; }

    public RealmList<OldAvatarRealmObject> getAvatars() { return avatars; }

    public RealmList<ResourceRealmObject> getResources() { return resources; }

    public RealmList<ContactGroupRealmObject> getGroups() { return groups; }
    public void setGroups(RealmList<ContactGroupRealmObject> groups) { this.groups = groups; }

    public Collection<String> getGroupsNames() {
        Collection<String> result = new ArrayList<String>();
        for (ContactGroupRealmObject contactGroupRealmObject : getGroups())
            result.add(contactGroupRealmObject.getGroupName());
        return result;
    }
}
