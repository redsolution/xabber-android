package com.xabber.android.data.groupchat;

import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.messagerealm.GroupchatUser;
import com.xabber.android.data.extension.references.RefUser;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupchatUserManager implements OnLoadListener {

    private static GroupchatUserManager instance;
    private final Map<String, GroupUser> users = new HashMap<>();

    public static GroupchatUserManager getInstance() {
        if (instance == null) instance = new GroupchatUserManager();
        return instance;
    }

    @Override
    public void onLoad() {
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        RealmResults<GroupchatUser> users = realm.where(GroupchatUser.class).findAll();
        for (GroupchatUser user : users) {
            this.users.put(user.getUniqueId(), realmUserToUser(user));
        }
    }

    public GroupUser getGroupchatUser(String id) {
        return users.get(id);
    }

    public void saveGroupchatUser(RefUser user) {
        saveGroupchatUser(user, System.currentTimeMillis());
    }

    public void saveGroupchatUser(RefUser user, long timestamp) {
        if (!users.containsKey(user.getId())) {
            saveUser(user, timestamp);
        } else if (timestamp > users.get(user.getId()).getTimestamp()) {
            saveUser(user, timestamp);
        }
    }

    private void saveUser(RefUser user, long timestamp) {
        users.put(user.getId(), refUserToUser(user));
        saveGroupchatUserToRealm(refUserToRealm(user), timestamp);
    }

    private void saveGroupchatUserToRealm(final GroupchatUser user, final long timestamp) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                user.setTimestamp(timestamp);
                Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(user);
                realm.commitTransaction();
                realm.close();
            }
        });
    }

    private GroupchatUser refUserToRealm(RefUser user) {
        GroupchatUser realmUser = new GroupchatUser(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatar() != null) realmUser.setAvatar(user.getAvatar());
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    private GroupUser refUserToUser(RefUser refUser) {
        GroupUser user = new GroupUser(refUser.getId());
        user.setAvatar(refUser.getAvatar());
        user.setBadge(refUser.getBadge());
        user.setJid(refUser.getJid());
        user.setNickname(refUser.getNickname());
        user.setRole(refUser.getRole());
        return user;
    }

    private GroupUser realmUserToUser(GroupchatUser groupchatUser) {
        GroupUser user = new GroupUser(groupchatUser.getUniqueId());
        user.setAvatar(groupchatUser.getAvatar());
        user.setBadge(groupchatUser.getBadge());
        user.setJid(groupchatUser.getJid());
        user.setNickname(groupchatUser.getNickname());
        user.setRole(groupchatUser.getRole());
        user.setTimestamp(groupchatUser.getTimestamp());
        return user;
    }

}
