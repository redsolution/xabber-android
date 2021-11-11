package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;

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
        List<ContactJid> blockedList = BlockingManager.getInstance().getCachedBlockedContacts(account);

        for (Jid jid : unblockedJids) {
            try {
                blockedList.remove(ContactJid.from(jid));
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(getClass().getSimpleName(), e);
            }
        }

        BlockingManager.notify(account);
    }
}
