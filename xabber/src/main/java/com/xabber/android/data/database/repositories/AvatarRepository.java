package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.OldAvatarRealmObject;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;
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
            RealmResults<OldAvatarRealmObject> oldAvatarRealmObjects = realm
                    .where(OldAvatarRealmObject.class)
                    .isNotNull(OldAvatarRealmObject.Fields.PEP_HASH)
                    .findAll();
            for (OldAvatarRealmObject oldAvatarRealmObject : oldAvatarRealmObjects) {
                BareJid bareJid = JidCreate.from(oldAvatarRealmObject.getUser()).asBareJid();
                String pepHash = oldAvatarRealmObject.getPepHash();
                pepHashes.put(bareJid, pepHash.isEmpty() ? AvatarManager.EMPTY_HASH : pepHash);
            }
        } catch (Exception e) {
            LogManager.exception("AvatarRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return pepHashes;
    }

    public static void savePepHashToRealm(final String user, final String pepHash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    OldAvatarRealmObject oldAvatarRealmObject = realm1
                            .where(OldAvatarRealmObject.class)
                            .equalTo(OldAvatarRealmObject.Fields.USER, user)
                            .findFirst();
                    if (oldAvatarRealmObject != null) {
                        oldAvatarRealmObject.setPepHash(pepHash);
                    } else {
                        oldAvatarRealmObject = new OldAvatarRealmObject(user);
                        oldAvatarRealmObject.setPepHash(pepHash);
                    }
                    realm1.copyToRealmOrUpdate(oldAvatarRealmObject);
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
            RealmResults<OldAvatarRealmObject> oldAvatarRealmObjects = realm
                    .where(OldAvatarRealmObject.class)
                    .isNotNull(OldAvatarRealmObject.Fields.HASH)
                    .findAll();
            for (OldAvatarRealmObject oldAvatarRealmObject : oldAvatarRealmObjects) {
                BareJid bareJid = JidCreate.from(oldAvatarRealmObject.getUser()).asBareJid();
                String hash = oldAvatarRealmObject.getHash();
                pepHashes.put(bareJid, hash.isEmpty() ? AvatarManager.EMPTY_HASH : hash);
            }
        } catch (Exception e) {
            LogManager.exception("AvatarRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return pepHashes;
    }

    public static void saveHashToRealm(final String user, final String hash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    OldAvatarRealmObject oldAvatarRealmObject = realm1
                            .where(OldAvatarRealmObject.class)
                            .equalTo(OldAvatarRealmObject.Fields.USER, user)
                            .findFirst();
                    if (oldAvatarRealmObject != null) {
                        oldAvatarRealmObject.setHash(hash);
                    } else {
                        oldAvatarRealmObject = new OldAvatarRealmObject(user);
                        oldAvatarRealmObject.setHash(hash);
                    }
                    realm1.copyToRealmOrUpdate(oldAvatarRealmObject);
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
