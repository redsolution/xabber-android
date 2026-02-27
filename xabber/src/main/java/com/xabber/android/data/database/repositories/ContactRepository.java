package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.RegularChatRealmObject;
import com.xabber.android.data.entity.AccountJid;
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

        if (Looper.myLooper() != Looper.getMainLooper())
            realm.close();

        return contacts;
    }

    public static String getBestNameFromRealm(Jid jid) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        String name = "";

        ContactRealmObject contact = realm
                .where(ContactRealmObject.class)
                .equalTo(RegularChatRealmObject.Fields.CONTACT_JID, jid.toString())
                .findFirst();

        if (contact != null && contact.getBestName() != null)
            name = contact.getBestName();

        if (Looper.myLooper() != Looper.getMainLooper())
            realm.close();

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
                        String account = contact.getAccount().getFullJid().toString();
                        String user = contact.getContactJid().getBareJid().toString();

                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, account)
                                .equalTo(ContactRealmObject.Fields.CONTACT_JID, user)
                                .findFirst();

                        if (contactRealmObject == null) {
                            contactRealmObject = new ContactRealmObject();
                        }

                        contactRealmObject.setAccountJid(account);
                        contactRealmObject.setContactJid(user);
                        contactRealmObject.setBestName(contact.getName());
                        newContacts.add(contactRealmObject);
                    }
                    realm1.copyToRealmOrUpdate(newContacts);
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null)
                    realm.close();
            }
        });
    }

    public static void removeContacts(final Collection<RosterContact> contacts) {
        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> {
                    for (RosterContact contact : contacts) {
                        String accountJid = contact.getAccount().getFullJid().asBareJid().toString();
                        String contactJid = contact.getContactJid().getBareJid().toString();

                        ContactRealmObject contactRealmObject = realm1
                                .where(ContactRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, accountJid)
                                .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid)
                                .findFirst();
                        if (contactRealmObject != null) {
                            VCardRepository.deleteVCardFromRealm(contact.getContactJid());
                            contactRealmObject.deleteFromRealm();
                        }
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }

        });
    }

    public static void removeContacts(AccountJid accountJid) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    ContactRealmObject[] contactRealmObjects = realm1
                            .where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                            .findAll().toArray(new ContactRealmObject[0]);

                    // Delete the user's own avatar
                    AvatarRepository.deleteAvatarFromRealm(accountJid.getBareJid().toString());

                    for (ContactRealmObject contactRealmObject: contactRealmObjects) {
                        if (contactRealmObject != null) {
                            AvatarRepository.deleteAvatarFromRealm(contactRealmObject.getContactJid());
                            contactRealmObject.deleteFromRealm();
                        }
                    }
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null)
                    realm.close();
            }
        });
    }

}
