package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.RegularChatRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterContact;

import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContactRepository {

    public static final String LOG_TAG = ContactRepository.class.getSimpleName();

    public static List<ContactRealmObject> getContactsFromRealm() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<ContactRealmObject> contacts = realm
                .where(ContactRealmObject.class)
                .findAll();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return contacts;
    }

    public static String getBestNameFromRealm(Jid jid){
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        String name = "";
        ContactRealmObject contact = realm
                .where(ContactRealmObject.class)
                .equalTo(RegularChatRealmObject.Fields.CONTACT_JID, jid.toString())
                .findFirst();

        if (contact != null && contact.getBestName() != null)
            name = contact.getBestName();

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        return name;
    }

    public static void saveContactToRealm(final AccountJid accountJid, final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    if (contacts.size() > 1) {
                        RealmResults<ContactRealmObject> results = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT_JID,
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
                                .equalTo(ContactRealmObject.Fields.ID,account + "/" + user)
                                .findFirst();
                        if (contactRealmObject == null) {
                            contactRealmObject = new ContactRealmObject();
                        }

//                        RealmList<CircleRealmObject> groups = new RealmList<>();
//                        for (String groupName : contact.getGroupNames()) {
//                            CircleRealmObject group = realm1.copyToRealmOrUpdate(new CircleRealmObject(groupName));
//                            if (group.isManaged() && group.isValid())
//                                groups.add(group);
//                        } TODO REALM UPDATE. THIS!

                        //contactRealmObject.setGroups(groups);
                        contactRealmObject.setAccountJid(account);
                        contactRealmObject.setContactJid(user);
                        contactRealmObject.setBestName(contact.getName());
                        // contactRealmObject.getResources().add(contact.getAccount().getFullJid().getResourcepart().toString()); TODO REALM UPDATE Implement resource writing
                        newContacts.add(contactRealmObject);
                    }
                    realm1.copyToRealmOrUpdate(newContacts);
                });
            } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            finally { if (realm != null) realm.close(); }
        });
    }

    public static void removeContacts(final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    for (RosterContact contact : contacts) {
                        String accountJid = contact.getAccount().getFullJid().asBareJid().toString();
                        String contactJid = contact.getUser().getBareJid().toString();

                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT_JID,accountJid)
                                .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid)
                                .findFirst();
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
                    realm1.where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, accountJid)
                            .findAll().deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static ContactRealmObject getContactRealmObjectFromRealm(AccountJid accountJid, ContactJid contactJid){
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        ContactRealmObject contactRealmObject = realm
                .where(ContactRealmObject.class)
                .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, accountJid.getFullJid().asBareJid().toString())
                .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                .findFirst();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return contactRealmObject;
    }

}
