package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
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
        final Map<BareJid, String> pepHashes = new HashMap<>();

        Application.getInstance().runOnUiThread(() -> {
            try {
                RealmResults<AvatarRealm> avatarRealms = Realm.getDefaultInstance()
                        .where(AvatarRealm.class)
                        .findAll();
                for (AvatarRealm avatarRealm : avatarRealms){
                    BareJid bareJid = JidCreate.from(avatarRealm.getUser()).asBareJid();
                    String pepHash = avatarRealm.getPepHash();
                    pepHashes.put(bareJid, pepHash.isEmpty()? AvatarManager.EMPTY_HASH : pepHash);
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

    public static Map<BareJid, String> getHashesMapFromRealm(){
        final Map<BareJid, String> pepHashes = new HashMap<>();

        Application.getInstance().runOnUiThread(() -> {
            try {
                RealmResults<AvatarRealm> avatarRealms = Realm.getDefaultInstance()
                        .where(AvatarRealm.class)
                        .findAll();
                for (AvatarRealm avatarRealm : avatarRealms){
                    BareJid bareJid = JidCreate.from(avatarRealm.getUser()).asBareJid();
                    String hash = avatarRealm.getHash();
                    pepHashes.put(bareJid, hash.isEmpty()? AvatarManager.EMPTY_HASH : hash);
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
