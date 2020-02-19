package com.xabber.android.data.database.repositories;

import android.net.Uri;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.PhraseNotificationRealm;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.phrase.Phrase;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class PhraseNotificationRepository {

    public static ArrayList<Phrase> getAllPhrases(){
        LogManager.d("PhrasesRepo", "getAll");
        final ArrayList<Phrase> phrasesList = new ArrayList<>();

        Application.getInstance().runOnUiThread(() -> {
            RealmResults<PhraseNotificationRealm> realmObjList = Realm.getDefaultInstance()
                    .where(PhraseNotificationRealm.class)
                    .findAll();

            for (PhraseNotificationRealm realmObj : realmObjList){
                Uri uri = Uri.parse(realmObj.getSound());
                phrasesList.add(new Phrase(realmObj.getId(), realmObj.getValue(), realmObj.getUser(),
                        realmObj.getGroup(), realmObj.getRegexp(), uri));
            }
        });

        return phrasesList;
    }

    public static void removePhraseById(final long id){
        LogManager.d("PhrasesRepo", "removeById");
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(PhraseNotificationRealm.class)
                            .equalTo(PhraseNotificationRealm.Fields.ID, id)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("PhraseNotificationRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void saveNewPhrase(final Phrase phrase, final String value, final String user,
                                     final String group, final boolean regexp, final Uri sound){
        LogManager.d("PhrasesRepo", "saveNew");
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
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
            } finally { if (realm != null) realm.close(); }
        });
    }
}
