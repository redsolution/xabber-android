package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groups.GroupMember;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;

public class GroupMemberRepository {

    public static final String LOG_TAG = GroupMemberRepository.class.getSimpleName();

    public static Collection<GroupMember> getAllGroupMembersFromRealm() {

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        Collection<GroupMemberRealmObject> groupMemberRealmObjects = realm
                .where(GroupMemberRealmObject.class)
                .findAll();

        ArrayList<GroupMember> result = new ArrayList<>(groupMemberRealmObjects.size());

        for (GroupMemberRealmObject gro : groupMemberRealmObjects)
            result.add(new GroupMember(gro.getPrimaryKey(), gro.getMemberId(), gro.getAccountJid(), gro.getGroupJid(),
                    gro.getJid(), gro.getNickname(), gro.getRole(), gro.getBadge(), gro.getAvatarHash(),
                    gro.getAvatarUrl(), gro.getLastSeen(), gro.isMe(), gro.isBlocked(), gro.isKicked()));

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        return result;
    }

    public static void removeGroupMemberById(AccountJid accountJid, ContactJid groupJid, String memberId) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                     GroupMemberRealmObject gro = realm1.where(GroupMemberRealmObject.class)
                            .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, accountJid.getBareJid().toString())
                            .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.getBareJid().toString())
                            .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberId)
                            .findFirst();
                     if (gro != null && !"".equals(gro.getMemberId()))
                         gro.deleteFromRealm();
                     else LogManager.e(LOG_TAG,
                             "Tried to delete from realm group member from group: " + groupJid.toString()
                                     +  ", accountJid: " + accountJid.getBareJid().toString()
                                     + ", memberId: " + memberId + ", but realm hasn't it");
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void saveOrUpdateGroupMember(GroupMember groupMember) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    GroupMemberRealmObject gro = realm1.where(GroupMemberRealmObject.class)
                            .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, groupMember.getAccountJid().toString())
                            .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupMember.getGroupJid().getBareJid().toString())
                            .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, groupMember.getMemberId())
                            .findFirst();

                    if (gro == null) gro = GroupMemberRealmObject.createGroupMemberRealmObject(
                            groupMember.getAccountJid(), groupMember.getGroupJid(), groupMember.getMemberId());

                    gro.setJid(groupMember.getJid());
                    gro.setNickname(groupMember.getNickname());
                    gro.setRole(groupMember.getRole());
                    gro.setBadge(groupMember.getBadge());
                    gro.setAvatarHash(groupMember.getAvatarHash());
                    gro.setAvatarUrl(groupMember.getAvatarUrl());
                    gro.setLastSeen(groupMember.getLastPresent());

                    gro.setMe(groupMember.isMe());
                    gro.setBlocked(groupMember.isBlocked());
                    gro.setKicked(groupMember.isKicked());

                    realm1.insertOrUpdate(gro);
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

}
