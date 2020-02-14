package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.VCardRealm;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.StructuredName;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class VCardRepository {

    public static Map<Jid, StructuredName> getAllVCardsFromRealm(){
        Map<Jid, StructuredName> allVCardsMap = new HashMap<>();
        RealmResults<VCardRealm> realmResults = Realm.getDefaultInstance()
                .where(VCardRealm.class)
                .findAll();
        for (VCardRealm vCardRealm : realmResults){
            try {
                Jid jid = JidCreate.from(vCardRealm.getUser());
                StructuredName structuredName = new StructuredName(vCardRealm.getNickName(),
                        vCardRealm.getFormattedName(), vCardRealm.getFirstName(),
                        vCardRealm.getMiddleName(), vCardRealm.getLastName());
                allVCardsMap.put(jid, structuredName);
            } catch (Exception e) { LogManager.exception("VCardRepository", e); }
        }
        return allVCardsMap;
    }

    public static void saveVCardToRealm(final Jid jid, final StructuredName structuredName){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    VCardRealm vCardRealm = new VCardRealm(jid.toString(),
                            structuredName.getNickName(),
                            structuredName.getFormattedName(),
                            structuredName.getFirstName(),
                            structuredName.getMiddleName(),
                            structuredName.getLastName());
                    realm1.copyToRealmOrUpdate(vCardRealm);
                });
            } catch (Exception e){
                LogManager.exception("VCardrepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}
