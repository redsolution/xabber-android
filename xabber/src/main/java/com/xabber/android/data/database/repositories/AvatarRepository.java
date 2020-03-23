package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AvatarRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class AvatarRepository {

    public static Map<BareJid, String> getPepHashesMapFromRealm(){
        Map<BareJid, String> pepHashes = new HashMap<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<AvatarRealmObject> avatarRealmObjects = realm
                    .where(AvatarRealmObject.class)
                    .isNotNull(AvatarRealmObject.Fields.PEP_HASH)
                    .findAll();
            for (AvatarRealmObject avatarRealmObject : avatarRealmObjects) {
                BareJid bareJid = JidCreate.from(avatarRealmObject.getContactJid()).asBareJid();
                String pepHash = avatarRealmObject.getPepHash();
                pepHashes.put(bareJid, pepHash.isEmpty() ? AvatarManager.EMPTY_HASH : pepHash);
            }
        } catch (Exception e) {
            LogManager.exception("AvatarRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return pepHashes;
    }

    public static void savePepHashToRealm(final Jid contactJid, final String pepHash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    ContactRealmObject contactRealmObject = realm1
                            .where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid.asBareJid().toString())
                            .findFirst();

                    if (contactRealmObject != null && pepHash != null && !pepHash.isEmpty()){

                        AvatarRealmObject avatarRealmObject = realm1
                                .where(AvatarRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid.asBareJid().toString())
                                .findFirst();

                        if (avatarRealmObject != null) {
                            avatarRealmObject.setPepHash(pepHash);
                        } else {
                            avatarRealmObject = new AvatarRealmObject(contactJid);
                            avatarRealmObject.setPepHash(pepHash);
                        }

                        if (!contactRealmObject.getAvatars().contains(avatarRealmObject))
                            contactRealmObject.getAvatars().add(avatarRealmObject);

                        realm1.insertOrUpdate(contactRealmObject);
                        realm1.insertOrUpdate(avatarRealmObject);
                    }
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static Map<BareJid, String> getHashesMapFromRealm(){
        Map<BareJid, String> pepHashes = new HashMap<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<AvatarRealmObject> avatarRealmObjects = realm
                    .where(AvatarRealmObject.class)
                    .isNotNull(AvatarRealmObject.Fields.VCARD_HASH)
                    .findAll();
            for (AvatarRealmObject oldAvatarRealmObject : avatarRealmObjects) {
                BareJid bareJid = JidCreate.from(oldAvatarRealmObject.getContactJid()).asBareJid();
                String hash = oldAvatarRealmObject.getVCardHash();
                pepHashes.put(bareJid, hash.isEmpty() ? AvatarManager.EMPTY_HASH : hash);
            }
        } catch (Exception e) {
            LogManager.exception("AvatarRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return pepHashes;
    }

    public static void saveHashToRealm(final Jid contactJid, final String hash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    ContactRealmObject contactRealmObject = realm1
                            .where(ContactRealmObject.class)
                            .equalTo(ContactRealmObject.Fields.CONTACT_JID, contactJid.asBareJid().toString())
                            .findFirst();

                    if (contactRealmObject != null && hash != null && !hash.isEmpty()){

                        AvatarRealmObject avatarRealmObject = realm1
                                .where(AvatarRealmObject.class)
                                .equalTo(ContactRealmObject.Fields.CONTACT_JID , contactJid.asBareJid().toString())
                                .findFirst();

                        if (avatarRealmObject != null) {
                            avatarRealmObject.setVCardHash(hash);
                        } else {
                            avatarRealmObject = new AvatarRealmObject(contactJid);
                            avatarRealmObject.setVCardHash(hash);
                        }

                        if (!contactRealmObject.getAvatars().contains(avatarRealmObject))
                            contactRealmObject.getAvatars().add(avatarRealmObject);

                        realm1.insertOrUpdate(contactRealmObject);
                        realm1.insertOrUpdate(avatarRealmObject);
                    }

                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
