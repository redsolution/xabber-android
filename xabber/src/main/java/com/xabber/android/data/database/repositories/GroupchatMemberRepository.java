package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatMemberRealmObject;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.groupchat.GroupchatMember;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;

public class GroupchatMemberRepository {

    public static final String LOG_TAG = GroupchatMemberRepository.class.getSimpleName();

    public static GroupchatMemberRealmObject getGroupchatMemberRealmObjectById(String id) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        GroupchatMemberRealmObject groupchatMemberRealmObject = realm
                .where(GroupchatMemberRealmObject.class)
                .equalTo(GroupchatMemberRealmObject.Fields.UNIQUE_ID, id)
                .findFirst();
        if (Looper.myLooper() == Looper.getMainLooper())
            return groupchatMemberRealmObject;
        else {
            GroupchatMemberRealmObject result = realm.copyFromRealm(groupchatMemberRealmObject);
            realm.close();
            return result;
        }
    }

    public static Collection<GroupchatMember> getAllGroupchatMembersFromRealm() {

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        Collection<GroupchatMemberRealmObject> groupchatMemberRealmObjects = realm
                .where(GroupchatMemberRealmObject.class)
                .findAll();

        ArrayList<GroupchatMember> result = new ArrayList<>(groupchatMemberRealmObjects.size());

        for (GroupchatMemberRealmObject gro : groupchatMemberRealmObjects)
            result.add(new GroupchatMember(gro.getUniqueId(), gro.getJid(), gro.getGroupchatJid(),
                    gro.getRole(), gro.getNickname(), gro.getBadge(), gro.getAvatarHash(),
                    gro.getAvatarUrl(), gro.getLastPresent(), gro.isCanRestrictMembers(),
                    gro.isCanBlockMembers(), gro.isCanChangeBadge(), gro.isCanChangeNickname(),
                    gro.isCanDeleteMessages(), gro.isRestrictedToSendMessages(),
                    gro.isRestrictedToReadMessages(), gro.isRestrictedToSendInvitations(),
                    gro.isRestrictedToSendAudio(), gro.isRestrictedToSendImages()));

        if (Looper.myLooper() != Looper.getMainLooper() && realm != null) realm.close();

        return result;
    }

    public static void removeGroupchatMemberById(String id) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(GroupchatMemberRealmObject.class)
                        .equalTo(GroupchatMemberRealmObject.Fields.UNIQUE_ID, id)
                        .findFirst()
                        .deleteFromRealm());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void removeGroupchatMember(GroupchatMember groupchatMember) {
        removeGroupchatMemberById(groupchatMember.getId());
    }

    public static void saveOrUpdateGroupchatMember(GroupchatMember groupchatMember) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    GroupchatMemberRealmObject gro = realm1.where(GroupchatMemberRealmObject.class)
                            .equalTo(GroupchatMemberRealmObject.Fields.UNIQUE_ID, groupchatMember.getId())
                            .findFirst();

                    if (gro == null)
                        gro = new GroupchatMemberRealmObject(groupchatMember.getId());

                    gro.setJid(groupchatMember.getJid());
                    gro.setGroupchatJid(groupchatMember.getGroupchatJid());
                    gro.setNickname(groupchatMember.getNickname());
                    gro.setRole(groupchatMember.getRole());
                    gro.setBadge(groupchatMember.getBadge());
                    gro.setAvatarHash(groupchatMember.getAvatarHash());
                    gro.setAvatarUrl(groupchatMember.getAvatarUrl());
                    gro.setLastPresent(groupchatMember.getLastPresent());

                    gro.setCanRestrictMembers(groupchatMember.isCanRestrictMembers());
                    gro.setCanBlockMembers(groupchatMember.isCanBlockMembers());
                    gro.setCanChangeBadge(groupchatMember.isCanChangeBadge());
                    gro.setCanChangeNickname(groupchatMember.isCanChangeNickname());
                    gro.setCanDeleteMessages(groupchatMember.isCanDeleteMessages());

                    gro.setRestrictedToSendMessages(groupchatMember.isRestrictedToSendMessages());
                    gro.setRestrictedToReadMessages(groupchatMember.isRestrictedToReadMessages());
                    gro.setRestrictedToSendImages(groupchatMember.isRestrictedToSendImages());
                    gro.setRestrictedToSendAudio(groupchatMember.isRestrictedToSendAudio());
                    gro.setRestrictedToSendInvitations(groupchatMember.isRestrictedToSendInvitations());

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
