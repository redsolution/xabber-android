package com.xabber.android.data.extension.iqlast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqlast.LastActivityManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;

import java.util.HashMap;
import java.util.LinkedList;

public class LastActivityInteractor {

    private static LastActivityInteractor instance;
    private HashMap<UserJid, Long> lastActivities = new HashMap<>();
    private LinkedList<JidPair> queryForLastActivityUpdate = new LinkedList<>();
    private boolean isRun;

    public static LastActivityInteractor getInstance() {
        if (instance == null) instance = new LastActivityInteractor();
        return instance;
    }

    public void addJidToLastActivityQuery(AccountJid account, UserJid user) {
        queryForLastActivityUpdate.addLast(new JidPair(account, user));
        if (!this.isRun)
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    runGettingLastActivity();
                }
            });
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

    private void setLastActivity(AccountJid account, UserJid user, long time) {
        lastActivities.put(user, time);
        RosterManager.onContactChanged(account, user);
    }

    private synchronized void runGettingLastActivity() {
        this.isRun = true;
        while (!queryForLastActivityUpdate.isEmpty()) {
            JidPair item = queryForLastActivityUpdate.removeFirst();
            long lastActivity = requestLastActivity(item.account, item.user);
            setLastActivity(item.account, item.user, lastActivity);
        }
        this.isRun = false;
    }

    private long requestLastActivity(AccountJid account, UserJid user) {
        long lastActivitySeconds = 0;

        AccountItem accountItem = AccountManager
                .getInstance().getAccount(account);

        if (accountItem == null) return lastActivitySeconds;

        XMPPTCPConnection xmppConnection = accountItem.getConnection();

        LastActivityManager lastActivityManager = LastActivityManager.getInstanceFor(xmppConnection);
        LastActivity lastActivity = null;

        try {
            lastActivity = lastActivityManager.getLastActivity(user.getJid());
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }

        if (lastActivity != null) lastActivitySeconds = lastActivity.lastActivity;

        if (lastActivitySeconds > 0)
            lastActivitySeconds = System.currentTimeMillis()/1000 - lastActivitySeconds;
        return lastActivitySeconds;
    }

    private class JidPair {
        AccountJid account;
        UserJid user;

        public JidPair(AccountJid account, UserJid user) {
            this.account = account;
            this.user = user;
        }

    }

}
