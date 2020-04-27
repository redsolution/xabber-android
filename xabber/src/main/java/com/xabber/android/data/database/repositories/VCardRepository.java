package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.VCardRealmObject;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.vcard.VCard;

import java.util.Collection;

import io.realm.Realm;

public class VCardRepository {

    public static final String LOG_TAG = VCardRepository.class.getSimpleName();

    public static VCardRealmObject getVCardForContactFromRealm(ContactJid contactJid){
        VCardRealmObject vCardRealmObject = null;
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            if (Looper.myLooper() == Looper.getMainLooper())
                 vCardRealmObject = realm.where(VCardRealmObject.class)
                        .equalTo(VCardRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                        .findFirst();
            else {
                VCardRealmObject tmpVCardRealmObject = realm.where(VCardRealmObject.class)
                        .equalTo(VCardRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                        .findFirst();
                vCardRealmObject = realm.copyFromRealm(tmpVCardRealmObject);
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close();
        }
        return vCardRealmObject;
    }

    public static Collection<VCardRealmObject> getAllVCardsFromRealm(){
        Collection<VCardRealmObject> vCardRealmObjects = null;
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            if (Looper.myLooper() == Looper.getMainLooper())
                vCardRealmObjects = realm.where(VCardRealmObject.class)
                        .findAll();
            else {
                Collection<VCardRealmObject> tmpCollection = realm.where(VCardRealmObject.class)
                        .findAll();
                vCardRealmObjects = realm.copyFromRealm(tmpCollection);
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close();
        }
        return vCardRealmObjects;
    }

    public static void saveOrUpdateVCardToRealm(final ContactJid contactJid, final VCard vCard){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    VCardRealmObject vCardRealmObject = realm1
                            .where(VCardRealmObject.class)
                            .equalTo(VCardRealmObject.Fields.CONTACT_JID, contactJid.getBareJid().toString())
                            .findFirst();
                    if (vCardRealmObject == null && vCardRealmObject.getVCard() == null){
                        vCardRealmObject = new VCardRealmObject(contactJid, vCard);
                    } else {
                        vCardRealmObject.setVCard(vCard);
                    }
                    realm1.insertOrUpdate(vCardRealmObject);
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }


}
