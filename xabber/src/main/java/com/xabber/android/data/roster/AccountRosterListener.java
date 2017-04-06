package com.xabber.android.data.roster;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jxmpp.jid.Jid;

import java.util.Collection;

public class AccountRosterListener implements RosterListener, RosterLoadedListener {

    private AccountJid account;

    public AccountJid getAccount() {
        return account;
    }

    public AccountRosterListener(AccountJid account) {
        this.account = account;
    }

    private String getLogTag() {
        StringBuilder logTag = new StringBuilder();
        logTag.append(getClass().getSimpleName());

        if (account != null) {
            logTag.append(": ");
            logTag.append(account);
        }
        return logTag.toString();
    }

    @Override
    public void entriesAdded(Collection<Jid> addresses) {
        LogManager.i(getLogTag(), "entriesAdded " + addresses);
        RosterManager.getInstance().onContactsAdded(account, addresses);
    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {
        LogManager.i(getLogTag(), "entriesUpdated " + addresses);
        RosterManager.getInstance().onContactsUpdated(account, addresses);
    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses) {
        LogManager.i(getLogTag(), "entriesDeleted " + addresses);
        RosterManager.getInstance().onContactsDeleted(account, addresses);
    }

    @Override
    public void presenceChanged(Presence presence) {
        PresenceManager.getInstance().onPresenceChanged(account, presence);
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        LogManager.i(getLogTag(), "onRosterLoaded");
        final AccountItem accountItem = AccountManager.getInstance().getAccount(AccountRosterListener.this.account);

        if (accountItem != null) {
            for (OnRosterReceivedListener listener : Application.getInstance().getManagers(OnRosterReceivedListener.class)) {
                listener.onRosterReceived(accountItem);
            }
        }
        AccountManager.getInstance().onAccountChanged(AccountRosterListener.this.account);
    }

    @Override
    public void onRosterLoadingFailed(Exception e) {
        LogManager.e(getLogTag(), "onRosterLoadingFailed");
        LogManager.exception(getLogTag(), e);
    }
}
