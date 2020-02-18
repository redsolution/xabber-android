package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.AvatarRealm;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class AvatarRepository {

    public static Map<Jid, String> getPepHashesMapFromRealm(){
        final Map<Jid, String> pepHashes = new HashMap<>();

        Application.getInstance().runOnUiThread(() -> {
            try {
                RealmResults<AvatarRealm> avatarRealms = Realm.getDefaultInstance()
                        .where(AvatarRealm.class)
                        .findAll();
                for (AvatarRealm avatarRealm : avatarRealms){
                    Jid jid = JidCreate.from(avatarRealm.getUser());
                    String pepHash = avatarRealm.getPepHash();
                    pepHashes.put(jid, pepHash.isEmpty()? AvatarManager.EMPTY_HASH : pepHash);
                }
            } catch (Exception e) { LogManager.exception("AvatarRepository", e); }
        });

        return pepHashes;
    }

    public static void savePepHashToRealm(final String user, final String pepHash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    AvatarRealm avatarRealm = new AvatarRealm(user);
                    avatarRealm.setPepHash(pepHash);
                    realm1.copyToRealmOrUpdate(avatarRealm);
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static Map<Jid, String> getHashesMapFromRealm(){
        final Map<Jid, String> pepHashes = new HashMap<>();

        Application.getInstance().runOnUiThread(() -> {
            try {
                RealmResults<AvatarRealm> avatarRealms = Realm.getDefaultInstance()
                        .where(AvatarRealm.class)
                        .findAll();
                for (AvatarRealm avatarRealm : avatarRealms){
                    Jid jid = JidCreate.from(avatarRealm.getUser());
                    String hash = avatarRealm.getHash();
                    pepHashes.put(jid, hash.isEmpty()? AvatarManager.EMPTY_HASH : hash);
                }
            } catch (Exception e) { LogManager.exception("AvatarRepository", e); }
        });

        return pepHashes;
    }

    public static void saveHashToRealm(final String user, final String hash){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    AvatarRealm avatarRealm = new AvatarRealm(user);
                    avatarRealm.setHash(hash);
                    realm1.copyToRealmOrUpdate(avatarRealm);
                });
            } catch (Exception e) {
                LogManager.exception("AvatarRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
