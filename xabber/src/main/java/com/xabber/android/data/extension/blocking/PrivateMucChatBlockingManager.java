package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Local MUC private chat blocking
 */
public class PrivateMucChatBlockingManager {

    private final static PrivateMucChatBlockingManager instance;

    private Map<String, RealmList<BlockedContact>> blockListsForAccounts;
    private Realm realm;

    static {
        instance = new PrivateMucChatBlockingManager();
        Application.getInstance().addManager(instance);
    }

    public static PrivateMucChatBlockingManager getInstance() {
        return instance;
    }

    public PrivateMucChatBlockingManager() {
        blockListsForAccounts = new ConcurrentHashMap<>();
        realm = Realm.getDefaultInstance();
        final RealmQuery<BlockedContactsForAccount> query = realm.where(BlockedContactsForAccount.class);
        final RealmResults<BlockedContactsForAccount> all = query.findAll();
        for (BlockedContactsForAccount blockedContactsForAccount : all) {
            blockListsForAccounts.put(blockedContactsForAccount.getAccount(), blockedContactsForAccount.getBlockedContacts());
        }
    }

    public Collection<String> getBlockedContacts(String account) {
        return Collections.unmodifiableCollection(getBlockedListForAccount(account));
    }

    private List<String> getBlockedListForAccount(String account) {
        createListForAccountIfNotExists(account);

        List<String> contacts = new ArrayList<>();
        for (BlockedContact blockedContact : blockListsForAccounts.get(account)) {
            contacts.add(blockedContact.getFullJid());
        }

        return contacts;
    }

    private void createListForAccountIfNotExists(String account) {
        if (!blockListsForAccounts.containsKey(account)) {
            realm.beginTransaction();
            final BlockedContactsForAccount blockedContactsForAccount = realm.createObject(BlockedContactsForAccount.class);
            blockedContactsForAccount.setAccount(account);
            realm.commitTransaction();
            blockListsForAccounts.put(account, blockedContactsForAccount.getBlockedContacts());
        }
    }

    public void blockContact(String account, String user) {
        createListForAccountIfNotExists(account);

        realm.beginTransaction();
        final BlockedContact blockedContact = realm.createObject(BlockedContact.class);
        blockedContact.setFullJid(user);
        blockListsForAccounts.get(account).add(blockedContact);
        realm.commitTransaction();

        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);

        notifyListeners(account);
    }

    private void notifyListeners(String account) {
        for (OnBlockedListChangedListener onBlockedListChangedListener
                : Application.getInstance().getUIListeners(OnBlockedListChangedListener.class)) {
            onBlockedListChangedListener.onBlockedListChanged(account);
        }

        for (OnContactChangedListener onContactChangedListener
                : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
            onContactChangedListener.onContactsChanged(new ArrayList<BaseEntity>());
        }
    }

    public Map<String, Collection<String>> getBlockedContacts() {
        Map<String, Collection<String>> result = new HashMap<>();

        for (String account : blockListsForAccounts.keySet()) {
            result.put(account, getBlockedContacts(account));
        }

        return Collections.unmodifiableMap(result);
    }

    public void unblockContacts(String account, final List<String> contacts) {
        if (blockListsForAccounts.containsKey(account)) {
            final RealmList<BlockedContact> blockedContacts = blockListsForAccounts.get(account);
            realm.beginTransaction();

            for (String contact : contacts) {
                blockedContacts.removeAll(blockedContacts.where().equalTo(BlockedContact.FIELD_FULL_JID, contact).findAll());
            }

            realm.commitTransaction();
        }

        notifyListeners(account);
    }

    public void unblockAll(String account) {
        if (blockListsForAccounts.containsKey(account)) {
            realm.beginTransaction();
            blockListsForAccounts.get(account).clear();
            realm.commitTransaction();
        }
        notifyListeners(account);
    }

}
