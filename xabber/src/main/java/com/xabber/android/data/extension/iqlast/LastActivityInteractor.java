package com.xabber.android.data.extension.iqlast;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jxmpp.jid.Jid;

import java.util.HashMap;

public class LastActivityInteractor implements OnPacketListener {

    private static LastActivityInteractor instance;
    private HashMap<UserJid, Long> lastActivities = new HashMap<>();

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
                if (result > 0) {
                    result = System.currentTimeMillis() / 1000 - result;
                    setLastActivity(connection.getAccount(), UserJid.from(jid), result);
                }
            } catch (UserJid.UserJidCreateException e) {
                e.printStackTrace();
            }
        }
    }

    public void setLastActivityTimeNow(AccountJid account, UserJid user) {
        long time = System.currentTimeMillis()/1000;
        setLastActivity(account, user, time);
    }

    public long getLastActivity(UserJid user) {
        Long lastActivity = lastActivities.get(user);
        if (lastActivity != null) return lastActivity;
        else return 0;
    }

    public void requestLastActivityAsync(AccountJid account, UserJid user) {
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

    private void setLastActivity(AccountJid account, UserJid user, long time) {
        lastActivities.put(user, time);
        RosterManager.onContactChanged(account, user);
    }

}
