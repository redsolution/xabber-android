package com.xabber.android.data.notification.custom_notification;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
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

    public void createNotifyPrefs(Context context, NotificationManager notificationManager, Key key,
                                  String vibro, boolean showPreview, String sound) {
        NotifyPrefs prefs = findPrefs(key);
        if (prefs == null) {
            prefs = new NotifyPrefs(UUID.randomUUID().toString(), key, vibro, showPreview, sound);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                prefs.setChannelID(NotificationChannelUtils.createCustomChannel(notificationManager,
                        key.generateName(context), key.generateDescription(context), Uri.parse(sound),
                        MessageNotificationCreator.getVibroValue(vibro, context), null));
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

    public boolean isPrefsExist(Key key) {
        NotifyPrefs prefs = findPrefs(key);
        return prefs != null;
    }

    public NotifyPrefs findPrefs(Key key) {
        for (NotifyPrefs item : preferences) {
            if (item.getKey().equals(key)) return item;
        }
        return null;
    }

    public NotifyPrefs getNotifyPrefsIfExist(AccountJid account, UserJid user, String group, Long phraseID) {
        NotifyPrefs prefs = findPrefs(Key.createKey(phraseID));
        if (prefs == null) prefs = findPrefs(Key.createKey(account, user));
        if (prefs == null) prefs = findPrefs(Key.createKey(account, group));
        if (prefs == null) prefs = findPrefs(Key.createKey(account));
        return prefs;
    }

    public void deleteAllNotifyPrefs(NotificationManager notificationManager) {
        Iterator it = preferences.iterator();
        while (it.hasNext()) {
            NotifyPrefs item = (NotifyPrefs) it.next();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotificationChannelUtils.removeCustomChannel(notificationManager, item.getChannelID());
            it.remove();
        }
        removeAllFromRealm();
    }

    public void deleteNotifyPrefs(NotificationManager notificationManager, String id) {
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
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<NotifyPrefsRealm> items = realm
                .where(NotifyPrefsRealm.class)
                .findAll();
        for (NotifyPrefsRealm item : items) {
            NotifyPrefs prefs = new NotifyPrefs(item.getId(), Key.createKey(item), item.getVibro(),
                    item.isShowPreview(), item.getSound());
            prefs.setChannelID(item.getChannelID());
            results.add(prefs);
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return results;
    }

    private void removeFromRealm(final String id) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(NotifyPrefsRealm.class)
                            .equalTo(NotifyPrefsRealm.Fields.ID, id)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("CustomNotifyPrefsManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void removeAllFromRealm() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    realm1.where(NotifyPrefsRealm.class)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception("CustomNotifyPrefsManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    private void saveOrUpdateToRealm(final NotifyPrefs prefs) {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    NotifyPrefsRealm prefsRealm = new NotifyPrefsRealm(prefs.getId());

                    prefsRealm.setType(prefs.getKey().getType().toString());
                    if (prefs.getKey().getAccount() != null) prefsRealm.setAccount(prefs.getKey().getAccount());
                    if (prefs.getKey().getUser() != null) prefsRealm.setUser(prefs.getKey().getUser());
                    prefsRealm.setGroup(prefs.getKey().getGroup());
                    prefsRealm.setPhraseID(prefs.getKey().getPhraseID());
                    prefsRealm.setChannelID(prefs.getChannelID());
                    prefsRealm.setShowPreview(prefs.isShowPreview());
                    prefsRealm.setSound(prefs.getSound());
                    prefsRealm.setVibro(prefs.getVibro());

                    NotifyPrefsRealm result = realm1.copyToRealmOrUpdate(prefsRealm);
                });
            } catch (Exception e) {
                LogManager.exception("CustomNotifyPrefsManager", e);
            } finally { if (realm != null) realm.close(); }
        });
    }

}
