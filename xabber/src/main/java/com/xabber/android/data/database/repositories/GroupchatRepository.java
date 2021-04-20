package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatRealmObject;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.GroupChat;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupchatRepository {

    private static final String LOG_TAG = GroupchatRepository.class.getSimpleName();

    public static void removeGroupChatFromRealm(GroupChat groupChat) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> realm1.where(GroupchatRealmObject.class)
                        .equalTo(GroupchatRealmObject.Fields.ACCOUNT_JID, groupChat.getAccount().getBareJid().toString())
                        .equalTo(GroupchatRealmObject.Fields.GROUPCHAT_JID, groupChat.getContactJid().toString())
                        .findFirst()
                        .deleteFromRealm());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static void saveOrUpdateGroupchatRealmObject(GroupChat groupChat) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {

                    GroupchatRealmObject groupchatRealmObject = realm1
                            .where(GroupchatRealmObject.class)
                            .equalTo(GroupchatRealmObject.Fields.ACCOUNT_JID,
                                    groupChat.getAccount().toString())
                            .equalTo(GroupchatRealmObject.Fields.GROUPCHAT_JID,
                                    groupChat.getContactJid().toString())
                            .findFirst();

                    if (groupchatRealmObject == null)
                        groupchatRealmObject = new GroupchatRealmObject(groupChat.getAccount(),
                                groupChat.getContactJid());

                    groupchatRealmObject.setOwner(groupChat.getOwner());
                    groupchatRealmObject.setName(groupChat.getName());
                    groupchatRealmObject.setPrivacy(groupChat.getPrivacyType());
                    groupchatRealmObject.setIndex(groupChat.getIndexType());
                    groupchatRealmObject.setMembership(groupChat.getMembershipType());
                    groupchatRealmObject.setDescription(groupChat.getDescription());
                    groupchatRealmObject.setPinnedMessageId(groupChat.getPinnedMessageId());
                    groupchatRealmObject.setMembersListVersion(groupChat.getMembersListVersion());
                    groupchatRealmObject.setMembersCount(groupChat.getNumberOfMembers());

                    groupchatRealmObject.setCanInvite(groupChat.isCanInvite());
                    groupchatRealmObject.setCanChangeSettings(groupChat.isCanChangeSettings());
                    groupchatRealmObject.setCanChangeUsersSettings(groupChat.isCanChangeUsersSettings());
                    groupchatRealmObject.setCanChangeNicknames(groupChat.isCanChangeNicknames());
                    groupchatRealmObject.setCanChangeBadge(groupChat.isCanChangeBadge());
                    groupchatRealmObject.setCanBlockUsers(groupChat.isCanBlockUsers());
                    groupchatRealmObject.setCanChangeAvatars(groupChat.isCanChangeAvatars());

                    groupchatRealmObject.setNotificationState(groupChat.getNotificationState());
                    groupchatRealmObject.setResource(groupChat.getResource());

                    realm1.insertOrUpdate(groupchatRealmObject);
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    public static Collection<GroupChat> getAllGroupchatsFromRealm() {

        if (Looper.getMainLooper() != Looper.getMainLooper())
            LogManager.w(LOG_TAG, "Read realm from non ui thread! Unclosed realm instance!");

        ArrayList<GroupChat> list = new ArrayList<>();

        RealmResults<GroupchatRealmObject> realmObjects = DatabaseManager.getInstance()
                .getDefaultRealmInstance()
                .where(GroupchatRealmObject.class)
                .findAll();

        for (GroupchatRealmObject gro : realmObjects) {
            try{
                list.add(new GroupChat(gro.getAccountJid(), gro.getGroupchatJid(), gro.getIndex(),
                        gro.getMembership(), gro.getPrivacy(), gro.getOwner(), gro.getName(),
                        gro.getDescription(), gro.getMembersCount(), gro.getPinnedMessageId(),
                        gro.getMembersListVersion(), gro.isCanInvite(), gro.isCanChangeSettings(),
                        gro.isCanChangeUsersSettings(), gro.isCanChangeNicknames(), gro.isCanChangeBadge(),
                        gro.isCanBlockUsers(), gro.isCanChangeAvatars(), gro.getResource(), gro.getNotificationState()));
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        }

        return list;
    }

}
