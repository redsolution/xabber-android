package com.xabber.android.data.roster;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.ContactGroup;
import com.xabber.android.data.database.realm.ContactRealm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class RosterCacheManager {

    public static List<ContactRealm> loadContacts() {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        return realm.where(ContactRealm.class).findAll();
    }

    public static void saveContact(Collection<RosterContact> contacts) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();

        realm.beginTransaction();
        List<ContactRealm> newContacts = new ArrayList<>();
        for (RosterContact contact : contacts) {
            String account = contact.getAccount().getFullJid().asBareJid().toString();
            String user = contact.getUser().getBareJid().toString();

            ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID,
                    account + "/" + user).findFirst();
            if (contactRealm == null) {
                contactRealm = new ContactRealm(account + "/" + user);
            }

            RealmList<ContactGroup> groups = new RealmList<>();
            for (String groupName : contact.getGroupNames()) {
                ContactGroup group = realm.copyToRealmOrUpdate(new ContactGroup(groupName));
                if (group.isManaged() && group.isValid())
                    groups.add(group);
            }

            contactRealm.setGroups(groups);
            contactRealm.setAccount(account);
            contactRealm.setUser(user);
            contactRealm.setName(contact.getName());
            contactRealm.setAccountResource(contact.getAccount().getFullJid().getResourcepart().toString());
            newContacts.add(contactRealm);
        }
        realm.copyToRealmOrUpdate(newContacts);
        realm.commitTransaction();
    }

    public static void saveLastMessageToContact(Realm realm, MessageItem messageItem) {
        if (messageItem == null) return;
        String account = messageItem.getAccount().getFullJid().asBareJid().toString();
        String user = messageItem.getUser().getBareJid().toString();
        ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID, account + "/" + user).findFirst();
        realm.beginTransaction();
        if (contactRealm != null && messageItem.isValid() && messageItem.isManaged()) {
            contactRealm.setLastMessage(messageItem);
            realm.copyToRealmOrUpdate(contactRealm);
        }
        realm.commitTransaction();
    }
}
