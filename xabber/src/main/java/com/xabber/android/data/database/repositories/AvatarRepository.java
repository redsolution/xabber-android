package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AvatarRealmObject;
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
            RealmResults<AvatarRealmObject> avatarRealmObjects = realm
                    .where(AvatarRealmObject.class)
                    .isNotNull(AvatarRealmObject.Fields.PEP_HASH)
                    .findAll();
            for (AvatarRealmObject avatarRealmObject : avatarRealmObjects) {
                BareJid bareJid = JidCreate.from(avatarRealmObject.getUser()).asBareJid();
                String pepHash = avatarRealmObject.getPepHash();
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
                    AvatarRealmObject avatarRealmObject = realm1
                            .where(AvatarRealmObject.class)
                            .equalTo(AvatarRealmObject.Fields.USER, user)
                            .findFirst();
                    if (avatarRealmObject != null) {
                        avatarRealmObject.setPepHash(pepHash);
                    } else {
                        avatarRealmObject = new AvatarRealmObject(user);
                        avatarRealmObject.setPepHash(pepHash);
                    }
                    realm1.copyToRealmOrUpdate(avatarRealmObject);
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
                    .isNotNull(AvatarRealmObject.Fields.HASH)
                    .findAll();
            for (AvatarRealmObject avatarRealmObject : avatarRealmObjects) {
                BareJid bareJid = JidCreate.from(avatarRealmObject.getUser()).asBareJid();
                String hash = avatarRealmObject.getHash();
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
                    AvatarRealmObject avatarRealmObject = realm1
                            .where(AvatarRealmObject.class)
                            .equalTo(AvatarRealmObject.Fields.USER, user)
                            .findFirst();
                    if (avatarRealmObject != null) {
                        avatarRealmObject.setHash(hash);
                    } else {
                        avatarRealmObject = new AvatarRealmObject(user);
                        avatarRealmObject.setHash(hash);
                    }
                    realm1.copyToRealmOrUpdate(avatarRealmObject);
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
