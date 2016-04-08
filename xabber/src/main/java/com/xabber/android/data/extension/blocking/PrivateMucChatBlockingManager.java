package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.realm.BlockedContact;
import com.xabber.android.data.database.realm.BlockedContactsForAccount;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Local MUC private chat blocking
 */
public class PrivateMucChatBlockingManager {

    private final static PrivateMucChatBlockingManager instance;

    static {
        instance = new PrivateMucChatBlockingManager();
        Application.getInstance().addManager(instance);
    }

    public static PrivateMucChatBlockingManager getInstance() {
        return instance;
    }

    public List<UserJid> getBlockedContacts(AccountJid account) {
        Realm realm = Realm.getDefaultInstance();
        RealmList<BlockedContact> blockedContactsForAccount = getBlockedContactsForAccount(account, realm);

        List<UserJid> contacts = new ArrayList<>();
        for (BlockedContact blockedContact : blockedContactsForAccount) {
            try {
                contacts.add(UserJid.from(blockedContact.getFullJid()));
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(this, e);
            }
        }
        realm.close();

        return contacts;
    }

    public void blockContact(AccountJid account, UserJid user) {
        Realm realm  = Realm.getDefaultInstance();

        RealmList<BlockedContact> blockedContactsForAccount = getBlockedContactsForAccount(account, realm);

        realm.beginTransaction();
        final BlockedContact blockedContact = realm.createObject(BlockedContact.class);
        blockedContact.setFullJid(user.getJid().asFullJidIfPossible().toString());
        blockedContactsForAccount.add(blockedContact);

        realm.commitTransaction();
        realm.close();

        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        notifyListeners(account);
    }

    private RealmList<BlockedContact> getBlockedContactsForAccount(AccountJid account, Realm realm) {
        BlockedContactsForAccount blockedContactsForAccount = realm.where(BlockedContactsForAccount.class)
                .equalTo(BlockedContactsForAccount.Fields.ACCOUNT, account.toString())
                .findFirst();
        if (blockedContactsForAccount == null) {
            realm.beginTransaction();
            blockedContactsForAccount = realm.createObject(BlockedContactsForAccount.class);
            blockedContactsForAccount.setAccount(account.toString());
            realm.commitTransaction();
        }
        return blockedContactsForAccount.getBlockedContacts();
    }

    private void notifyListeners(AccountJid account) {
        for (OnBlockedListChangedListener onBlockedListChangedListener
                : Application.getInstance().getUIListeners(OnBlockedListChangedListener.class)) {
            onBlockedListChangedListener.onBlockedListChanged(account);
        }

        for (OnContactChangedListener onContactChangedListener
                : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
            onContactChangedListener.onContactsChanged(new ArrayList<BaseEntity>());
        }
    }

    public Map<AccountJid, Collection<UserJid>> getBlockedContacts() {
        Map<AccountJid, Collection<UserJid>> blockedContacts = new HashMap<>();

        for (AccountJid account : AccountManager.getInstance().getAccounts()) {
            blockedContacts.put(account, getBlockedContacts(account));
        }

        return blockedContacts;
    }

    public void unblockContacts(AccountJid account, final List<UserJid> contacts) {
        Realm realm = Realm.getDefaultInstance();
        RealmList<BlockedContact> blockedContacts = getBlockedContactsForAccount(account, realm);
        realm.beginTransaction();

        for (UserJid contact : contacts) {
            blockedContacts.removeAll(blockedContacts.where().equalTo(BlockedContact.FIELD_FULL_JID, contact.toString()).findAll());
        }

        realm.commitTransaction();
        realm.close();
        notifyListeners(account);
    }

    public void unblockAll(AccountJid account) {
        Realm realm = Realm.getDefaultInstance();
        RealmList<BlockedContact> blockedContactsForAccount = getBlockedContactsForAccount(account, realm);
        realm.beginTransaction();
        blockedContactsForAccount.clear();
        realm.commitTransaction();
        realm.close();
        notifyListeners(account);
    }

}
