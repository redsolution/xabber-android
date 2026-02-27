package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageWithoutReceiptRealmObject;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class MessageWithoutReceiptRepository {

    private static final String LOG_TAG = MessageWithoutReceiptRepository.class.getSimpleName();

    public static void saveToRealm(String messageOriginId){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->
                        realm1.copyToRealmOrUpdate(new MessageWithoutReceiptRealmObject(messageOriginId)));
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                    realm.close();
            }
        });
    }

    public static void removeFromRealm(String messageOriginId){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(MessageWithoutReceiptRealmObject.class)
                        .equalTo(MessageWithoutReceiptRealmObject.Fields.MESSAGE_ORIGIN_ID, messageOriginId)
                        .findAll()
                        .deleteAllFromRealm());
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                    realm.close();
            }
        });
    }

    public static List<String> getAll(){
        List<String> result = new ArrayList<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            for (MessageWithoutReceiptRealmObject mwr : realm.where(MessageWithoutReceiptRealmObject.class).findAll()){
                result.add(mwr.getMessageOriginId());
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                realm.close();
        }
        return result;
    }


}
