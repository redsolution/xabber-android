package com.xabber.android.data.roster;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ContactGroup;
import com.xabber.android.data.database.realmobjects.ContactRealm;
import com.xabber.android.data.database.realmobjects.MessageItem;
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

    public static List<ContactRealm> loadContacts() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ContactRealm> contacts = realm
                .where(ContactRealm.class)
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
                        RealmResults<ContactRealm> results = realm1
                                .where(ContactRealm.class)
                                .equalTo(ContactRealm.Fields.ACCOUNT,
                                        accountJid.getFullJid().asBareJid().toString())
                                .findAll();
                        results.deleteAllFromRealm();
                    }

                    List<ContactRealm> newContacts = new ArrayList<>();
                    for (RosterContact contact : contacts) {
                        String account = contact.getAccount().getFullJid().asBareJid().toString();
                        String user = contact.getUser().getBareJid().toString();

                        ContactRealm contactRealm = realm1
                                .where(ContactRealm.class)
                                .equalTo(ContactRealm.Fields.ID,account + "/" + user).findFirst();
                        if (contactRealm == null) {
                            contactRealm = new ContactRealm(account + "/" + user);
                        }

                        RealmList<ContactGroup> groups = new RealmList<>();
                        for (String groupName : contact.getGroupNames()) {
                            ContactGroup group = realm1.copyToRealmOrUpdate(new ContactGroup(groupName));
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

                        ContactRealm contactRealm = realm1
                                .where(ContactRealm.class)
                                .equalTo(ContactRealm.Fields.ID,account + "/" + user).findFirst();
                        if (contactRealm != null)
                            contactRealm.deleteFromRealm();
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
                        RealmResults<ContactRealm> results = realm1
                                .where(ContactRealm.class)
                                .equalTo(ContactRealm.Fields.ACCOUNT, accountJid)
                                .findAll();
                        results.deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void saveLastMessageToContact(final MessageItem messageItem) {
        if (messageItem == null) return;
        final String account = messageItem.getAccount().getFullJid().asBareJid().toString();
        final String user = messageItem.getUser().getBareJid().toString();
        final String messageID = messageItem.getUniqueId();
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                        ContactRealm contactRealm = realm1
                                .where(ContactRealm.class)
                                .equalTo(ContactRealm.Fields.ID, account + "/" + user)
                                .findFirst();
                        MessageItem message = realm1
                                .where(MessageItem.class)
                                .equalTo(MessageItem.Fields.UNIQUE_ID, messageID)
                                .findFirst();
                        if (contactRealm != null && message != null && message.isValid() && message.isManaged()) {
                            contactRealm.setLastMessage(message);
                            realm1.copyToRealmOrUpdate(contactRealm);
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
