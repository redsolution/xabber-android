package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
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
            result.add(new GroupMember(gro.getUniqueId(), gro.getJid(), gro.getGroupJid(),
                    gro.getNickname(), gro.getRole(), gro.getBadge(), gro.getAvatarHash(),
                    gro.getAvatarUrl(), gro.getLastPresent(), gro.isMe(), gro.isCanRestrictMembers(),
                    gro.isCanBlockMembers(), gro.isCanChangeBadge(), gro.isCanChangeNickname(),
                    gro.isCanDeleteMessages(), gro.isRestrictedToSendMessages(),
                    gro.isRestrictedToReadMessages(), gro.isRestrictedToSendInvitations(),
                    gro.isRestrictedToSendAudio(), gro.isRestrictedToSendImages()));

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        return result;
    }

    public static void removeGroupMemberById(String id) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                     GroupMemberRealmObject gro = realm1.where(GroupMemberRealmObject.class)
                            .equalTo(GroupMemberRealmObject.Fields.UNIQUE_ID, id)
                            .findFirst();
                     if (gro != null && !"".equals(gro.getUniqueId()))
                         gro.deleteFromRealm();
                     else LogManager.e(LOG_TAG, "Tried to delete from realm groupchat member with id " + id + ", but realm hasn't it");
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
                            .equalTo(GroupMemberRealmObject.Fields.UNIQUE_ID, groupMember.getId())
                            .findFirst();

                    if (gro == null)
                        gro = new GroupMemberRealmObject(groupMember.getId());

                    gro.setJid(groupMember.getJid());
                    gro.setGroupJid(groupMember.getGroupJid());
                    gro.setNickname(groupMember.getNickname());
                    gro.setRole(groupMember.getRole());
                    gro.setBadge(groupMember.getBadge());
                    gro.setAvatarHash(groupMember.getAvatarHash());
                    gro.setAvatarUrl(groupMember.getAvatarUrl());
                    gro.setLastPresent(groupMember.getLastPresent());

                    gro.setMe(groupMember.isMe());

                    gro.setCanRestrictMembers(groupMember.isCanRestrictMembers());
                    gro.setCanBlockMembers(groupMember.isCanBlockMembers());
                    gro.setCanChangeBadge(groupMember.isCanChangeBadge());
                    gro.setCanChangeNickname(groupMember.isCanChangeNickname());
                    gro.setCanDeleteMessages(groupMember.isCanDeleteMessages());

                    gro.setRestrictedToSendMessages(groupMember.isRestrictedToSendMessages());
                    gro.setRestrictedToReadMessages(groupMember.isRestrictedToReadMessages());
                    gro.setRestrictedToSendImages(groupMember.isRestrictedToSendImages());
                    gro.setRestrictedToSendAudio(groupMember.isRestrictedToSendAudio());
                    gro.setRestrictedToSendInvitations(groupMember.isRestrictedToSendInvitations());

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
