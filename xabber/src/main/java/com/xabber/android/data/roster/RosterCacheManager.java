package com.xabber.android.data.roster;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ContactGroupRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RosterCacheManager {

    public static final String LOG_TAG = "RosterCacheManager";

    private static RosterCacheManager instance;
    private Map<Long, String> lastActivityCache = new HashMap<>();

    public static RosterCacheManager getInstance() {
        if (instance == null)
            instance = new RosterCacheManager();
        return instance;
    }

    public static List<ContactRealmObject> loadContacts() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ContactRealmObject> contacts = realm
                .where(ContactRealmObject.class)
                .findAll();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return contacts;
    }

    public static void saveContact(final AccountJid accountJid, final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    if (contacts.size() > 1) {
                        RealmResults<ContactRealmObject> results = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT,
                                        accountJid.getFullJid().asBareJid().toString())
                                .findAll();
                        results.deleteAllFromRealm();
                    }

                    List<ContactRealmObject> newContacts = new ArrayList<>();
                    for (RosterContact contact : contacts) {
                        String account = contact.getAccount().getFullJid().asBareJid().toString();
                        String user = contact.getUser().getBareJid().toString();

                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ID,account + "/" + user).findFirst();
                        if (contactRealmObject == null) {
                            contactRealmObject = new ContactRealmObject(account + "/" + user);
                        }

                        RealmList<ContactGroupRealmObject> groups = new RealmList<>();
                        for (String groupName : contact.getGroupNames()) {
                            ContactGroupRealmObject group = realm1.copyToRealmOrUpdate(new ContactGroupRealmObject(groupName));
                            if (group.isManaged() && group.isValid())
                                groups.add(group);
                        }

                        contactRealmObject.setGroups(groups);
                        contactRealmObject.setAccount(account);
                        contactRealmObject.setUser(user);
                        contactRealmObject.setName(contact.getName());
                        contactRealmObject.setAccountResource(contact.getAccount().getFullJid().getResourcepart().toString());
                        newContacts.add(contactRealmObject);
                    }
                    realm1.copyToRealmOrUpdate(newContacts);
                });
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            finally { if (realm != null) realm.close(); }
        });
    }

    public static void removeContact(final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    for (RosterContact contact : contacts) {
                        String account = contact.getAccount().getFullJid().asBareJid().toString();
                        String user = contact.getUser().getBareJid().toString();

                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ID,account + "/" + user).findFirst();
                        if (contactRealmObject != null)
                            contactRealmObject.deleteFromRealm();
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }

        });
    }

    public static void removeContacts(AccountJid account) {
        final String accountJid = account.getFullJid().asBareJid().toString();
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                        RealmResults<ContactRealmObject> results = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT, accountJid)
                                .findAll();
                        results.deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void saveLastMessageToContact(final MessageRealmObject messageRealmObject) {
        if (messageRealmObject == null) return;
        final String account = messageRealmObject.getAccount().getFullJid().asBareJid().toString();
        final String user = messageRealmObject.getUser().getBareJid().toString();
        final String messageID = messageRealmObject.getUniqueId();
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ID, account + "/" + user)
                                .findFirst();
                        MessageRealmObject message = realm1
                                .where(MessageRealmObject.class)
                                .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageID)
                                .findFirst();
                        if (contactRealmObject != null && message != null && message.isValid() && message.isManaged()) {
                            contactRealmObject.setLastMessage(message);
                            realm1.copyToRealmOrUpdate(contactRealmObject);
                        }
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public String getCachedLastActivityString(long lastActivityTime) {
        return lastActivityCache.get(lastActivityTime);
    }

    public void putLastActivityStringToCache(long lastActivityTime, String string) {
        lastActivityCache.put(lastActivityTime, string);
    }
}
