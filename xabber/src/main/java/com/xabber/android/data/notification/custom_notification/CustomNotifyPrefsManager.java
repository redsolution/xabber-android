package com.xabber.android.data.notification.custom_notification;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.notification.MessageNotificationCreator;
import com.xabber.android.data.notification.NotificationChannelUtils;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;

public class CustomNotifyPrefsManager implements OnLoadListener {

    private static CustomNotifyPrefsManager instance;
    private List<NotifyPrefs> preferences = new ArrayList<>();

    public static CustomNotifyPrefsManager getInstance() {
        if (instance == null) instance = new CustomNotifyPrefsManager();
        return instance;
    }

    @Override
    public void onLoad() {
        final List<NotifyPrefs> prefs = findAllFromRealm();
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(prefs);
            }
        });
    }

    public void onLoaded(List<NotifyPrefs> prefs) {
        this.preferences.addAll(prefs);
    }

    public void createChatNotifyPrefs(Context context, NotificationManager notificationManager,
                                      AccountJid account, UserJid user,
                                      String vibro, boolean showPreview, String sound) {
        NotifyPrefs prefs = findPrefsByChat(account, user);
        if (prefs == null) {
            prefs = new NotifyPrefs(UUID.randomUUID().toString(), NotifyPrefs.Type.chat, account, user,
                    null, null, vibro, showPreview, sound);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String name = user.getBareJid().toString() + " (" + account.getFullJid().asBareJid().toString() + ')';
                String description = "Custom notification channel";
                prefs.setChannelID(NotificationChannelUtils.createCustomChannel(notificationManager,
                        name, description, Uri.parse(sound), MessageNotificationCreator.getVibroValue(vibro, context),
                        null));
            }

            preferences.add(prefs);
        } else {
            prefs.setShowPreview(showPreview);
            prefs.setSound(sound);
            prefs.setVibro(vibro);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                prefs.setChannelID(NotificationChannelUtils.updateCustomChannel(notificationManager, prefs.getChannelID(), Uri.parse(sound),
                        MessageNotificationCreator.getVibroValue(vibro, context), null));
        }
        saveOrUpdateToRealm(prefs);
    }

    public NotifyPrefs findPrefsByChat(AccountJid account, UserJid user) {
        for (NotifyPrefs item : preferences) {
            if (item.getAccount().equals(account) && item.getUser().equals(user)
                    && item.getType().equals(NotifyPrefs.Type.chat))
                return item;
        }
        return null;
    }

    public NotifyPrefs findPrefsByGroup(AccountJid account, String group) {
        for (NotifyPrefs item : preferences) {
            if (item.getAccount().equals(account) && item.getGroup().equals(group)
            && item.getType().equals(NotifyPrefs.Type.group))
                return item;
        }
        return null;
    }

    public NotifyPrefs findPrefsByPhrase(AccountJid account, Long phraseID) {
        for (NotifyPrefs item : preferences) {
            if (item.getAccount().equals(account) && item.getPhraseID().equals(phraseID)
                    && item.getType().equals(NotifyPrefs.Type.phrase))
                return item;
        }
        return null;
    }

    public NotifyPrefs findPrefsByAccount(AccountJid account) {
        for (NotifyPrefs item : preferences) {
            if (item.getAccount().equals(account) && item.getType().equals(NotifyPrefs.Type.account))
                return item;
        }
        return null;
    }

    public NotifyPrefs getNotifyPrefsIfExist(AccountJid account, UserJid user, String group, String text) {
        NotifyPrefs prefs = null;
        //prefs = findPrefsByPhrase(account, );
        if (prefs == null) prefs = findPrefsByChat(account, user);
        if (prefs == null) prefs = findPrefsByGroup(account, group);
        if (prefs == null) prefs = findPrefsByAccount(account);
        return prefs;
    }

    public void deleteChatNotifyPrefs(NotificationManager notificationManager, String id) {
        Iterator it = preferences.iterator();
        while (it.hasNext()) {
            NotifyPrefs item = (NotifyPrefs) it.next();
            if (item.getId().equals(id)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    NotificationChannelUtils.removeCustomChannel(notificationManager, item.getChannelID());
                it.remove();
                break;
            }
        }
        removeFromRealm(id);
    }

    // REALM

    private List<NotifyPrefs> findAllFromRealm() {
        List<NotifyPrefs> results = new ArrayList<>();
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<NotifyPrefsRealm> items = realm.where(NotifyPrefsRealm.class).findAll();
        for (NotifyPrefsRealm item : items) {
            NotifyPrefs prefs = new NotifyPrefs(item.getId(), NotifyPrefs.Type.get(item.getType()),
                    item.getAccount(), item.getUser(),
                    item.getGroup(), item.getPhraseID(), item.getVibro(),
                    item.isShowPreview(), item.getSound());
            prefs.setChannelID(item.getChannelID());
            results.add(prefs);
        }
        realm.close();
        return results;
    }

    private void removeFromRealm(final String id) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                RealmResults<NotifyPrefsRealm> items = realm.where(NotifyPrefsRealm.class)
                        .equalTo(NotifyPrefsRealm.Fields.ID, id).findAll();
                removeFromRealm(realm, items);
            }
        });
    }

    private void removeFromRealm(Realm realm, RealmResults<NotifyPrefsRealm> items) {
        realm.beginTransaction();
        for (NotifyPrefsRealm item : items) {
            item.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    private void saveOrUpdateToRealm(final NotifyPrefs prefs) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                NotifyPrefsRealm prefsRealm = new NotifyPrefsRealm(prefs.getId());
                prefsRealm.setType(prefs.getType().toString());
                prefsRealm.setAccount(prefs.getAccount());
                prefsRealm.setUser(prefs.getUser());
                prefsRealm.setGroup(prefs.getGroup());
                prefsRealm.setPhraseID(prefs.getPhraseID());
                prefsRealm.setChannelID(prefs.getChannelID());
                prefsRealm.setShowPreview(prefs.isShowPreview());
                prefsRealm.setSound(prefs.getSound());
                prefsRealm.setVibro(prefs.getVibro());

                Realm realm = RealmManager.getInstance().getNewRealm();
                realm.beginTransaction();
                NotifyPrefsRealm result = realm.copyToRealmOrUpdate(prefsRealm);
                realm.commitTransaction();
            }
        });
    }

}
