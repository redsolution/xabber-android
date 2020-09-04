package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatMemberRealmObject;

import io.realm.Realm;

public class GroupchatMemberRepository {

    public static GroupchatMemberRealmObject getGroupchatMemberRealmObjectById(String id){
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        GroupchatMemberRealmObject groupchatMemberRealmObject = realm
                .where(GroupchatMemberRealmObject.class)
                .equalTo(GroupchatMemberRealmObject.Fields.UNIQUE_ID, id)
                .findFirst();
        if (Looper.myLooper().equals(Looper.getMainLooper()))
            return groupchatMemberRealmObject;
        else {
            GroupchatMemberRealmObject result = realm.copyFromRealm(groupchatMemberRealmObject);
            if (realm != null)
                realm.close();
            return result;
        }

    }
}
