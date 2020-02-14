package com.xabber.android.data.database.repositories;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.RoomRealm;
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
        try {
            RealmResults<RoomRealm> realmResults = Realm.getDefaultInstance()
                    .where(RoomRealm.class)
                    .findAll();
            for (RoomRealm roomRealm : realmResults){
                Resourcepart nickName = Resourcepart.from(roomRealm.getNickname());
                AccountJid account = AccountJid.from(roomRealm.getAccount());
                EntityBareJid room = JidCreate.entityBareFrom(roomRealm.getRoom());
                String password = roomRealm.getPassword();

                roomChats.add(RoomChat.create(account, room, nickName, password));
            }
        } catch (Exception e) { LogManager.exception("RoomRepository", e); }
        return roomChats;
    }

    public static Collection<RoomChat> getAllNeedJoinRoomChatsFromRealm(){
        Collection<RoomChat> roomChats = new ArrayList<>();
        try {
            RealmResults<RoomRealm> realmResults = Realm.getDefaultInstance()
                    .where(RoomRealm.class)
                    .equalTo(RoomRealm.Fields.NEED_JOIN, true)
                    .findAll();
            for (RoomRealm roomRealm : realmResults){
                Resourcepart nickName = Resourcepart.from(roomRealm.getNickname());
                AccountJid account = AccountJid.from(roomRealm.getAccount());
                EntityBareJid room = JidCreate.entityBareFrom(roomRealm.getRoom());
                String password = roomRealm.getPassword();

                roomChats.add(RoomChat.create(account, room, nickName, password));
            }
        } catch (Exception e) { LogManager.exception("RoomRepository", e); }
        return roomChats;
    }

    public static void saveRoomToRealm(final String account, final String room,
                                       final String nickname, final String password,
                                       final boolean isNeedJoin){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(new RoomRealm(account, room, nickname, password,
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
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(RoomRealm.class)
                            .equalTo(RoomRealm.Fields.ACCOUNT, account)
                            .equalTo(RoomRealm.Fields.ROOM, room)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e){
                LogManager.exception("Roomrepository", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
