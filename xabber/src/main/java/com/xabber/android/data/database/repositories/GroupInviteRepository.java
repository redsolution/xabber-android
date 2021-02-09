package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupInviteRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupInviteRepository {

    private static final String LOG_TAG = GroupInviteRepository.class.getSimpleName();

    public static List<GroupInviteRealmObject> getAllInvitationsFromRealm(){
        List<GroupInviteRealmObject> result = new ArrayList<>();
        Realm realm = null;

        try{
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<GroupInviteRealmObject> currentInvites = realm.where(GroupInviteRealmObject.class)
                    .findAll();
            if (Looper.myLooper() == Looper.getMainLooper()){
                result.addAll(currentInvites);
            } else result.addAll(realm.copyFromRealm(currentInvites));

        } catch (Exception e){
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close();
        }

        return result;
    }

    public static void saveOrUpdateInviteToRealm(GroupInviteRealmObject giro){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(giro));
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void saveOrUpdateInviteToRealm(AccountJid accountJid, ContactJid senderJid, ContactJid groupJid,
                                                 String reason, long date, boolean isIncoming, boolean isRead){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    GroupInviteRealmObject giro = new GroupInviteRealmObject(accountJid, groupJid, senderJid);
                    giro.setIncoming(isIncoming);
                    giro.setReason(reason);
                    giro.setDate(date);
                    realm1.copyToRealmOrUpdate(giro);
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void removeInviteFromRealm(AccountJid accountJid, ContactJid groupJid){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try{
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(GroupInviteRealmObject.class)
                            .equalTo(GroupInviteRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                            .equalTo(GroupInviteRealmObject.Fields.GROUP_JID, groupJid.toString())
                            .findAll().deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

}
