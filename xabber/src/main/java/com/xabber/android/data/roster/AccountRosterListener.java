package com.xabber.android.data.roster;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;

public class AccountRosterListener implements RosterListener, RosterLoadedListener {

    private AccountJid account;

    public AccountJid getAccount() {
        return account;
    }

    public AccountRosterListener(AccountJid account) {
        this.account = account;
    }

    @Override
    public void entriesAdded(Collection<Jid> collection) {
        update(collection);
    }

    @Override
    public void entriesUpdated(Collection<Jid> collection) {
        update(collection);
    }

    @Override
    public void entriesDeleted(Collection<Jid> collection) {
        update(collection);
    }

    @Override
    public void presenceChanged(Presence presence) {
        PresenceManager.getInstance().onPresenceChanged(account, presence);
    }

    private void update(Collection<Jid> addresses) {
        RosterManager.getInstance().updateContacts();

        Collection<BaseEntity> entities = new ArrayList<>();

        for (Jid address : addresses) {
            try {
                entities.add(new BaseEntity(account, UserJid.from(address)));
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(this, e);
            }
        }

        RosterManager.onContactsChanged(entities);
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        LogManager.i(this, "onRosterLoaded " + account);

        RosterManager.getInstance().updateContacts();

        final AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);

        for (OnRosterReceivedListener listener : Application.getInstance().getManagers(OnRosterReceivedListener.class)) {
            listener.onRosterReceived(accountItem);
        }
        AccountManager.getInstance().onAccountChanged(this.account);
    }

    @Override
    public void onRosterLoadingFailed(Exception e) {
        LogManager.exception(this, e);
    }
}
