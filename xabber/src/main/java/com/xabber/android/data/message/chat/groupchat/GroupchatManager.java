package com.xabber.android.data.message.chat.groupchat;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

//todo add and implement other listeners

public class GroupchatManager {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    private static final String NAMESPACE = "http://xabber.com/protocol/groupchat";

    static private GroupchatManager instance;

    public static GroupchatManager getInstance() {
        if (instance == null)
            instance = new GroupchatManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection connection) {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection)
                    .serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid).getConnection());
    }

}
