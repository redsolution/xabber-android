package com.xabber.android.data.roster;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.BaseEntity;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;

import java.util.ArrayList;
import java.util.Collection;

public class AccountRosterListener implements RosterListener, RosterLoadedListener {

    private String account;

    public String getAccount() {
        return account;
    }

    public AccountRosterListener(String account) {
        this.account = account;
    }

    @Override
    public void entriesAdded(Collection<String> addresses) {
        update(addresses);
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
        update(addresses);
    }


    @Override
    public void entriesDeleted(Collection<String> addresses) {
        update(addresses);
    }

    @Override
    public void presenceChanged(Presence presence) {
    }

    private void update(Collection<String> addresses) {
        RosterManager.getInstance().updateContacts();

        Collection<BaseEntity> entities = new ArrayList<>();

        for (String address : addresses) {
            entities.add(new BaseEntity(account, address));
        }

        RosterManager.getInstance().onContactsChanged(entities);
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        RosterManager.getInstance().updateContacts();

        final AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);

        for (OnRosterReceivedListener listener : Application.getInstance().getManagers(OnRosterReceivedListener.class)) {
            listener.onRosterReceived(accountItem);
        }
        AccountManager.getInstance().onAccountChanged(this.account);
    }
}
