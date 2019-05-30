package com.xabber.android.data.roster;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.ContactGroup;
import com.xabber.android.data.database.realm.ContactRealm;
import com.xabber.android.data.entity.AccountJid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RosterCacheManager {

    private static RosterCacheManager instance;
    private Map<Long, String> lastActivityCache = new HashMap<>();

    public static RosterCacheManager getInstance() {
        if (instance == null)
            instance = new RosterCacheManager();
        return instance;
    }

    public static List<ContactRealm> loadContacts() {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        return realm.where(ContactRealm.class).findAll();
    }

    public static void saveContact(AccountJid accountJid, Collection<RosterContact> contacts) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();

        realm.beginTransaction();
        if (contacts.size() > 1) {
            RealmResults<ContactRealm> results = realm.where(ContactRealm.class)
                    .equalTo(ContactRealm.Fields.ACCOUNT,
                            accountJid.getFullJid().asBareJid().toString()).findAll();
            results.deleteAllFromRealm();
        }

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

    public static void removeContact(Collection<RosterContact> contacts) {
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        realm.beginTransaction();
        for (RosterContact contact : contacts) {
            String account = contact.getAccount().getFullJid().asBareJid().toString();
            String user = contact.getUser().getBareJid().toString();

            ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID,
                    account + "/" + user).findFirst();
            if (contactRealm != null)
                contactRealm.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    public static void removeContacts(AccountJid account) {
        String accountJid = account.getFullJid().asBareJid().toString();
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        RealmResults<ContactRealm> results = realm.where(ContactRealm.class)
                .equalTo(ContactRealm.Fields.ACCOUNT, accountJid).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();
    }

    public static void saveLastMessageToContact(Realm realm, MessageItem messageItem) {
        if (messageItem == null) return;
        final String account = messageItem.getAccount().getFullJid().asBareJid().toString();
        final String user = messageItem.getUser().getBareJid().toString();
        final String messageID = messageItem.getUniqueId();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID, account + "/" + user).findFirst();
                MessageItem message = realm.where(MessageItem.class).equalTo(MessageItem.Fields.UNIQUE_ID, messageID).findFirst();
                if (contactRealm != null && message.isValid() && message.isManaged()) {
                    contactRealm.setLastMessage(message);
                    realm.copyToRealmOrUpdate(contactRealm);
                }
            }
        });
    }

    public String getCachedLastActivityString(long lastActivityTime) {
        return lastActivityCache.get(lastActivityTime);
    }

    public void putLastActivityStringToCache(long lastActivityTime, String string) {
        lastActivityCache.put(lastActivityTime, string);
    }
}
