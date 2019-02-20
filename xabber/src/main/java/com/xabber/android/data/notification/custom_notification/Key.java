package com.xabber.android.data.notification.custom_notification;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import javax.annotation.Nullable;

public class Key {
    private AccountJid account;
    private UserJid user;
    private String group;
    private Long phraseID;
    private NotifyPrefs.Type type;

    private Key() { }

    public static Key createKey(AccountJid account, UserJid user) {
        Key key = new Key();
        key.account = account;
        key.user = user;
        key.type = NotifyPrefs.Type.chat;
        return key;
    }

    public static Key createKey(AccountJid account, String group) {
        Key key = new Key();
        key.account = account;
        key.group = group;
        key.type = NotifyPrefs.Type.group;
        return key;
    }

    public static Key createKey(AccountJid account, Long phraseID) {
        Key key = new Key();
        key.account = account;
        key.phraseID = phraseID;
        key.type = NotifyPrefs.Type.phrase;
        return key;
    }

    public static Key createKey(AccountJid account) {
        Key key = new Key();
        key.account = account;
        key.type = NotifyPrefs.Type.account;
        return key;
    }

    public static @Nullable
    Key createKey(NotifyPrefsRealm prefsRealm) {
        if (prefsRealm.getAccount() != null && prefsRealm.getUser() != null)
            return createKey(prefsRealm.getAccount(), prefsRealm.getUser());
        else if (prefsRealm.getAccount() != null && prefsRealm.getGroup() != null)
            return createKey(prefsRealm.getAccount(), prefsRealm.getGroup());
        else if (prefsRealm.getAccount() != null && prefsRealm.getPhraseID() != null && prefsRealm.getPhraseID() != -1)
            return createKey(prefsRealm.getAccount(), prefsRealm.getPhraseID());
        else if (prefsRealm.getAccount() != null )
            return createKey(prefsRealm.getAccount());
        else return null;
    }

    public static @Nullable
    Key createKey(AccountJid account, UserJid user, String group, Long phraseID) {
        if (account != null && user != null)
            return createKey(account, user);
        else if (account != null && group != null)
            return createKey(account, group);
        else if (account != null && phraseID != null && phraseID != -1)
            return createKey(account, phraseID);
        else if (account != null )
            return createKey(account);
        else return null;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }

    public Long getPhraseID() {
        return phraseID;
    }

    public NotifyPrefs.Type getType() {
        return type;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Key key = (Key) obj;
        if (key == null) return false;
        if (!this.type.equals(key.type)) return false;
        switch (this.type) {
            case chat:
                if (this.account == null || this.user == null) return false;
                else if (this.account.equals(key.account) && this.user.equals(key.user)) return true;
            case group:
                if (this.account == null || this.group == null) return false;
                else if (this.account.equals(key.account) && this.group.equals(key.group)) return true;
            case phrase:
                if (this.account == null || this.phraseID == null) return false;
                else if (this.account.equals(key.account) && this.phraseID.equals(key.phraseID)) return true;
            case account:
                if (this.account == null) return false;
                else if (this.account.equals(key.account)) return true;
        }
        return false;
    }

    public String generateName() {
        switch (type) {
            case account:
                return account.getFullJid().asBareJid().toString();
            case chat:
                return user.getBareJid().toString() + " (" + account.getFullJid().asBareJid().toString() + ')';
            case group:
                return group + " (" + account.getFullJid().asBareJid().toString() + ')';
            case phrase:
                return "key phrase";
        }
        return "custom notification";
    }

    public String generateDescription() {
        String description = "Custom notification channel for ";
        switch (type) {
            case account:
                return description + "account " + generateName();
            case chat:
                return description + "chat " + generateName();
            case group:
                return description + "group " + generateName();
            case phrase:
                return description + "key phrase " + generateName();
        }
        return "Custom notification channel";
    }
}