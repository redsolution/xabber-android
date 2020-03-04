package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.RoomRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

public class RoomRepository {

    public static Collection<RoomChat> getAllRoomChatsFromRealm(){
        Collection<RoomChat> roomChats = new ArrayList<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<RoomRealmObject> realmResults = realm
                    .where(RoomRealmObject.class)
                    .findAll();
            for (RoomRealmObject roomRealmObject : realmResults){
                Resourcepart nickName = Resourcepart.from(roomRealmObject.getNickname());
                AccountJid account = AccountJid.from(roomRealmObject.getAccount());
                EntityBareJid room = JidCreate.entityBareFrom(roomRealmObject.getRoom());
                String password = roomRealmObject.getPassword();

                roomChats.add(RoomChat.create(account, room, nickName, password));
            }
        } catch (Exception e) {
            LogManager.exception("RoomRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
        return roomChats;
    }

    public static Collection<RoomChat> getAllNeedJoinRoomChatsFromRealm(){
        Collection<RoomChat> roomChats = new ArrayList<>();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<RoomRealmObject> realmResults = realm
                    .where(RoomRealmObject.class)
                    .equalTo(RoomRealmObject.Fields.NEED_JOIN, true)
                    .findAll();
            for (RoomRealmObject roomRealmObject : realmResults){
                Resourcepart nickName = Resourcepart.from(roomRealmObject.getNickname());
                AccountJid account = AccountJid.from(roomRealmObject.getAccount());
                EntityBareJid room = JidCreate.entityBareFrom(roomRealmObject.getRoom());
                String password = roomRealmObject.getPassword();

                roomChats.add(RoomChat.create(account, room, nickName, password));
            }
        } catch (Exception e) {
            LogManager.exception("RoomRepository", e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }

        return roomChats;
    }

    public static void saveRoomToRealm(final String account, final String room,
                                       final String nickname, final String password,
                                       final boolean isNeedJoin){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(new RoomRealmObject(account, room, nickname, password,
                            isNeedJoin));
                });
            } catch (Exception e){
                LogManager.exception("RoomRepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static void deleteRoomFromRealm(final String account, final String room){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(RoomRealmObject.class)
                            .equalTo(RoomRealmObject.Fields.ACCOUNT, account)
                            .equalTo(RoomRealmObject.Fields.ROOM, room)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("Roomrepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
