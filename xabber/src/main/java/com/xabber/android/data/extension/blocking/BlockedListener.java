package com.xabber.android.data.extension.blocking;


import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smackx.blocking.JidsBlockedListener;
import org.jxmpp.jid.Jid;

import java.util.List;

class BlockedListener implements JidsBlockedListener {
    private static final String LOG_TAG = BlockedListener.class.getSimpleName();
    private AccountJid account;

    public BlockedListener(AccountJid account) {
        this.account = account;
    }

    @Override
    public void onJidsBlocked(List<Jid> blockedJids) {
        for (Jid jid : blockedJids) {
            try {
                BlockingManager.blockContactLocally(account, UserJid.from(jid));
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }


        BlockingManager.notify(account);
    }
}
