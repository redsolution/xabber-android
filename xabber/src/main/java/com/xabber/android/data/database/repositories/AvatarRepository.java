package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AvatarRealm;
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
            RealmResults<AvatarRealm> avatarRealms = realm
                    .where(AvatarRealm.class)
                    .isNotNull(AvatarRealm.Fields.PEP_HASH)
                    .findAll();
            for (AvatarRealm avatarRealm : avatarRealms) {
                BareJid bareJid = JidCreate.from(avatarRealm.getUser()).asBareJid();
                String pepHash = avatarRealm.getPepHash();
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
                    AvatarRealm avatarRealm = realm1
                            .where(AvatarRealm.class)
                            .equalTo(AvatarRealm.Fields.USER, user)
                            .findFirst();
                    if (avatarRealm != null) {
                        avatarRealm.setPepHash(pepHash);
                    } else {
                        avatarRealm = new AvatarRealm(user);
                        avatarRealm.setPepHash(pepHash);
                    }
                    realm1.copyToRealmOrUpdate(avatarRealm);
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
            RealmResults<AvatarRealm> avatarRealms = realm
                    .where(AvatarRealm.class)
                    .isNotNull(AvatarRealm.Fields.HASH)
                    .findAll();
            for (AvatarRealm avatarRealm : avatarRealms) {
                BareJid bareJid = JidCreate.from(avatarRealm.getUser()).asBareJid();
                String hash = avatarRealm.getHash();
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
                    AvatarRealm avatarRealm = realm1
                            .where(AvatarRealm.class)
                            .equalTo(AvatarRealm.Fields.USER, user)
                            .findFirst();
                    if (avatarRealm != null) {
                        avatarRealm.setHash(hash);
                    } else {
                        avatarRealm = new AvatarRealm(user);
                        avatarRealm.setHash(hash);
                    }
                    realm1.copyToRealmOrUpdate(avatarRealm);
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
