package com.xabber.android.data.message.chat.groupchat;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.GroupchatUserRealmObject;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupchatMemberManager implements OnLoadListener {

    private static GroupchatMemberManager instance;
    private final Map<String, GroupchatMember> users = new HashMap<>();

    public static GroupchatMemberManager getInstance() {
        if (instance == null) instance = new GroupchatMemberManager();
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

    public void saveGroupchatUser(GroupchatUserExtension user, BareJid groupchatJid) {
        saveGroupchatUser(user, groupchatJid, System.currentTimeMillis());
    }

    public void saveGroupchatUser(GroupchatUserExtension user, BareJid groupchatJid, long timestamp) {
        if (!users.containsKey(user.getId())) {
            saveUser(user, groupchatJid, timestamp);
        } else if (timestamp > users.get(user.getId()).getTimestamp()) {
            saveUser(user, groupchatJid, timestamp);
        }
    }

    private void saveUser(GroupchatUserExtension user, BareJid groupchatJid, long timestamp) {
        users.put(user.getId(), refUserToUser(user, groupchatJid));
        saveGroupchatUserToRealm(refUserToRealm(user, groupchatJid), timestamp);
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

    public static GroupchatUserRealmObject refUserToRealm(GroupchatUserExtension user, BareJid groupchatJid) {
        GroupchatUserRealmObject realmUser = new GroupchatUserRealmObject(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        realmUser.setLastPresent(user.getLastPresent());
        if (groupchatJid != null) realmUser.setGroupchatJid(groupchatJid.toString());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatarInfo() != null) {
            realmUser.setAvatarHash(user.getAvatarInfo().getId());
            realmUser.setAvatarUrl(user.getAvatarInfo().getUrl().toString());
        }
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    public static GroupchatUserRealmObject userToRealmUser(GroupchatMember user) {
        GroupchatUserRealmObject realmUser = new GroupchatUserRealmObject(user.getId());
        realmUser.setNickname(user.getNickname());
        realmUser.setRole(user.getRole());
        realmUser.setLastPresent(user.getLastPresent());
        if (user.getGroupchatJid() != null) realmUser.setGroupchatJid(user.getGroupchatJid());
        if (user.getJid() != null) realmUser.setJid(user.getJid());
        if (user.getAvatarHash() != null) realmUser.setAvatarHash(user.getAvatarHash());
        if (user.getAvatarUrl() != null) realmUser.setAvatarUrl(user.getAvatarUrl());
        if (user.getBadge() != null) realmUser.setBadge(user.getBadge());
        return realmUser;
    }

    public static GroupchatMember refUserToUser(GroupchatUserExtension groupchatUserExtension, BareJid groupchatJid) {
        GroupchatMember user = new GroupchatMember(groupchatUserExtension.getId());
        if (groupchatJid != null) user.setGroupchatJid(groupchatJid.toString());
        if (groupchatUserExtension.getAvatarInfo() != null) {
            user.setAvatarHash(groupchatUserExtension.getAvatarInfo().getId());
            user.setAvatarUrl(groupchatUserExtension.getAvatarInfo().getUrl().toString());
        }
        user.setLastPresent(groupchatUserExtension.getLastPresent());
        user.setBadge(groupchatUserExtension.getBadge());
        user.setJid(groupchatUserExtension.getJid());
        user.setNickname(groupchatUserExtension.getNickname());
        user.setRole(groupchatUserExtension.getRole());
        return user;
    }

    public static GroupchatMember realmUserToUser(GroupchatUserRealmObject groupchatUser) {
        GroupchatMember user = new GroupchatMember(groupchatUser.getUniqueId());
        user.setAvatarHash(groupchatUser.getAvatarHash());
        user.setAvatarUrl(groupchatUser.getAvatarUrl());
        user.setLastPresent(groupchatUser.getLastPresent());
        user.setBadge(groupchatUser.getBadge());
        user.setJid(groupchatUser.getJid());
        user.setNickname(groupchatUser.getNickname());
        user.setRole(groupchatUser.getRole());
        user.setTimestamp(groupchatUser.getTimestamp());
        return user;
    }

}
