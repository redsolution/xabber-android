package com.xabber.android.data.database.repositories;

import com.xabber.android.data.database.realmobjects.PrivateChatRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterManager;

import java.util.HashSet;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

public class PrivateChatRepository {

    public static Set<BaseEntity> getSetOfAllBasePrivateChatEntitiesFromRealm(){
        Set<BaseEntity> resultSet = new HashSet<>();
        try {
            RealmResults<PrivateChatRealm> realmResults = Realm.getDefaultInstance()
                    .where(PrivateChatRealm.class)
                    .findAll();
            for (PrivateChatRealm privateChatRealm : realmResults){
                BaseEntity baseEntity = RosterManager.getInstance().getAbstractContact(
                        AccountJid.from(privateChatRealm.getAccount()),
                        UserJid.from(privateChatRealm.getUser()));
                resultSet.add(baseEntity);
            }
        } catch (Exception e) { LogManager.exception("PrivateChatrepository", e); }
        return resultSet;
    }
}
