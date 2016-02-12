package com.xabber.android.data.database.realm;

import io.realm.RealmList;
import io.realm.RealmObject;

public class BlockedContactsForAccount extends RealmObject {

    public static class Fields {
        public static final String ACCOUNT = "account";
    }

    private String account;
    private RealmList<BlockedContact> blockedContacts;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public RealmList<BlockedContact> getBlockedContacts() {
        return blockedContacts;
    }

    @SuppressWarnings("unused")
    public void setBlockedContacts(RealmList<BlockedContact> blockedContacts) {
        this.blockedContacts = blockedContacts;
    }
}
