package com.xabber.android.data.extension.iqlast;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jxmpp.jid.Jid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LastActivityInteractor implements OnPacketListener {

    private static LastActivityInteractor instance;
    private HashMap<ContactJid, Long> lastActivities = new HashMap<>();
    private Map<AccountJid, Iterator<RosterContact>> rosterItemIterators = new HashMap<>();
    private Map<AccountJid, ContactJid> requestedLastActivity = new HashMap<>();

    public static LastActivityInteractor getInstance() {
        if (instance == null) instance = new LastActivityInteractor();
        return instance;
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof LastActivity) {
            try {
                Jid jid = packet.getFrom();
                long result = ((LastActivity) packet).lastActivity;
                AccountJid account = connection.getAccount();
                ContactJid contact = ContactJid.from(jid);
                if (result > 0) {
                    result = System.currentTimeMillis() / 1000 - result;
                    setLastActivity(account, contact, result);
                }
                if (contact.equals(requestedLastActivity.get(account))) {
                    requestedLastActivity.remove(account);
                    requestNextContactLastActivity(account);
                }
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(getClass().getSimpleName(), e);
            }
        }
    }

    public void setLastActivityTimeNow(AccountJid account, ContactJid user) {
        long time = System.currentTimeMillis()/1000;
        setLastActivity(account, user, time);
    }

    public long getLastActivity(ContactJid user) {
        Long lastActivity = lastActivities.get(user);
        if (lastActivity != null) return lastActivity;
        else return 0;
    }

    public void requestLastActivityAsync(AccountJid account, ContactJid user) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem != null) {
            LastActivity activity = new LastActivity(user.getJid());
            try {
                accountItem.getConnection().sendStanza(activity);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LogManager.d(LastActivityInteractor.class, e.toString());
            }
        }
    }

    public void requestRosterLastActivity(AccountJid account, Collection<RosterContact> rosterContacts) {
        if (account != null) {
            if (rosterItemIterators.get(account) == null) {
                rosterItemIterators.put(account, rosterContacts.iterator());
            }
            requestNextContactLastActivity(account);
        }
    }

    public void interruptLastActivityRequest(AccountJid account) {
        if (account != null) {
            if (rosterItemIterators.get(account) != null) {
                rosterItemIterators.remove(account);
            }
            if (requestedLastActivity.get(account) != null) {
                requestedLastActivity.remove(account);
            }
        }
    }

    private void requestNextContactLastActivity(AccountJid account) {
        Iterator<RosterContact> iterator = rosterItemIterators.get(account);
        if (iterator != null && iterator.hasNext()) {
            RosterContact contact = iterator.next();
            requestedLastActivity.put(account, contact.getContactJid());
            requestLastActivityAsync(account, contact.getContactJid());
        } else {
            rosterItemIterators.remove(account);
            LogManager.d("timeCount/iterativeRequest", "finished getting last activity, time since connected = "
                    + (System.currentTimeMillis() - VCardManager.getInstance().getStart()) + " ms");
        }
    }

    private void setLastActivity(AccountJid account, ContactJid user, long time) {
        lastActivities.put(user, time);
        RosterManager.onContactChanged(account, user);
    }

}
