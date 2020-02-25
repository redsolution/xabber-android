package com.xabber.android.data.database.repositories;

import android.net.Uri;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.PhraseNotificationRealm;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.Phrase;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class PhraseNotificationRepository {

    public static ArrayList<Phrase> getAllPhrases(){
        ArrayList<Phrase> phrasesList = new ArrayList<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<PhraseNotificationRealm> realmObjList = realm
                    .where(PhraseNotificationRealm.class)
                    .findAll();

            for (PhraseNotificationRealm realmObj : realmObjList){
                Uri uri = Uri.parse(realmObj.getSound());
                phrasesList.add(new Phrase(realmObj.getId(), realmObj.getValue(), realmObj.getUser(),
                        realmObj.getGroup(), realmObj.getRegexp(), uri));
            }
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }


        return phrasesList;
    }

    public static void removePhraseById(final long id){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(PhraseNotificationRealm.class)
                            .equalTo(PhraseNotificationRealm.Fields.ID, id)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("PhraseNotificationRepository", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }

    public static void saveNewPhrase(final Phrase phrase, final String value, final String user,
                                     final String group, final boolean regexp, final Uri sound){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    PhraseNotificationRealm phraseNotifRealm = new PhraseNotificationRealm(phrase.getId());
                    phraseNotifRealm.setValue(value);
                    phraseNotifRealm.setUser(user);
                    phraseNotifRealm.setGroup(group);
                    phraseNotifRealm.setRegexp(regexp);
                    phraseNotifRealm.setSound(sound == null ? ChatManager.EMPTY_SOUND.toString()
                            : sound.toString());

                    realm1.copyToRealmOrUpdate(phraseNotifRealm);
                });
            } catch (Exception e) {
                LogManager.exception("PhraseNotificationRepository", e);
            } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        });
    }
}
