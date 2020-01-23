package com.xabber.android.data.roster;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.ContactGroup;
import com.xabber.android.data.database.realm.ContactRealm;
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
        return Realm.getDefaultInstance()
                .where(ContactRealm.class)
                .findAll();
    }

    public static void saveContact(final AccountJid accountJid, final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
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
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
                finally {
                    if (realm != null) realm.close();
                }
            }
        });
    }

    public static void removeContact(final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            for (RosterContact contact : contacts) {
                                String account = contact.getAccount().getFullJid().asBareJid().toString();
                                String user = contact.getUser().getBareJid().toString();

                                ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID,
                                        account + "/" + user).findFirst();
                                if (contactRealm != null)
                                    contactRealm.deleteFromRealm();
                            }
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            }
        });
    }

    public static void removeContacts(AccountJid account) {
        final String accountJid = account.getFullJid().asBareJid().toString();
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<ContactRealm> results = realm.where(ContactRealm.class)
                                    .equalTo(ContactRealm.Fields.ACCOUNT, accountJid).findAll();
                            results.deleteAllFromRealm();
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            }
        });
    }

    public static void saveLastMessageToContact(final MessageItem messageItem) {
        if (messageItem == null) return;
        final String account = messageItem.getAccount().getFullJid().asBareJid().toString();
        final String user = messageItem.getUser().getBareJid().toString();
        final String messageID = messageItem.getUniqueId();
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            ContactRealm contactRealm = realm.where(ContactRealm.class).equalTo(ContactRealm.Fields.ID, account + "/" + user).findFirst();
                            MessageItem message = realm.where(MessageItem.class).equalTo(MessageItem.Fields.UNIQUE_ID, messageID).findFirst();
                            if (contactRealm != null && message != null && message.isValid() && message.isManaged()) {
                                contactRealm.setLastMessage(message);
                                realm.copyToRealmOrUpdate(contactRealm);
                            }
                        }
                    });
                } catch (Exception e){ LogManager.exception(LOG_TAG, e); }
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
