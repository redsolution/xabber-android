package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smackx.blocking.JidsUnblockedListener;
import org.jxmpp.jid.Jid;

import java.util.List;

public class UnblockedListener implements JidsUnblockedListener {
    private AccountJid account;

    public UnblockedListener(AccountJid account) {
        this.account = account;
    }

    @Override
    public void onJidsUnblocked(List<Jid> unblockedJids) {
        BlockingManager.notify(account);
    }
}
