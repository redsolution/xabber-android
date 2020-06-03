package com.xabber.android.data.message.chat.groupchat;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatUserRealmObject;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.log.LogManager;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupchatUserManager implements OnLoadListener {

    private static GroupchatUserManager instance;
    private final Map<String, GroupchatMember> users = new HashMap<>();

    public static GroupchatUserManager getInstance() {
        if (instance == null) instance = new GroupchatUserManager();
        return instance;
    }

    @Override
    public void onLoad() {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<GroupchatUserRealmObject> users = realm
                .where(GroupchatUserRealmObject.class)
                .findAll();
        for (GroupchatUserRealmObject user : users) {
            this.users.put(user.getUniqueId(), realmUserToUser(user));
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    public GroupchatMember getGroupchatUser(String id) {
        return users.get(id);
    }

    public void saveGroupchatUser(GroupchatUserExtension user) {
        saveGroupchatUser(user, System.currentTimeMillis());
    }

    public void saveGroupchatUser(GroupchatUserExtension user, long timestamp) {
        if (!users.containsKey(user.getId())) {
            saveUser(user, timestamp);
        } else if (timestamp > users.get(user.getId()).getTimestamp()) {
            saveUser(user, timestamp);
        }
    }

    private void saveUser(GroupchatUserExtension user, long timestamp) {
        users.put(user.getId(), refUserToUser(user));
        saveGroupchatUserToRealm(refUserToRealm(user), timestamp);
    }

    private void saveGroupchatUserToRealm(final GroupchatUserRealmObject user, final long timestamp) {
        Application.getInstance().runInBackground(() -> {
            user.setTimestamp(timestamp);
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.copyToRealmOrUpdate(user);
                });
            } catch (Exception e) {
                LogManager.exception("GroupchatUserManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private GroupchatUserRealmObject refUserToRealm(GroupchatUserExtension user) {
        GroupchatUserRealmObject realmUser = new GroupchatUserRealmObject(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatar() != null) realmUser.setAvatar(user.getAvatar());
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    private GroupchatMember refUserToUser(GroupchatUserExtension groupchatUserExtension) {
        GroupchatMember user = new GroupchatMember(groupchatUserExtension.getId());
        user.setAvatar(groupchatUserExtension.getAvatar());
        user.setBadge(groupchatUserExtension.getBadge());
        user.setJid(groupchatUserExtension.getJid());
        user.setNickname(groupchatUserExtension.getNickname());
        user.setRole(groupchatUserExtension.getRole());
        return user;
    }

    private GroupchatMember realmUserToUser(GroupchatUserRealmObject groupchatUser) {
        GroupchatMember user = new GroupchatMember(groupchatUser.getUniqueId());
        user.setAvatar(groupchatUser.getAvatar());
        user.setBadge(groupchatUser.getBadge());
        user.setJid(groupchatUser.getJid());
        user.setNickname(groupchatUser.getNickname());
        user.setRole(groupchatUser.getRole());
        user.setTimestamp(groupchatUser.getTimestamp());
        return user;
    }

}
